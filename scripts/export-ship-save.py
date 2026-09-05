"""Read a closed Java 1.21 save and archive its ship as a vanilla structure NBT.

Requires nbtlib (pip install nbtlib). Source files are never changed.
The output is an archive, not an automatically installed ship template.
"""
import argparse
from collections import Counter
from copy import deepcopy
from datetime import datetime, timezone
import gzip
import hashlib
import io
import json
from pathlib import Path
import sys
import zipfile
import zlib

try:
    import nbtlib as n
except ImportError:
    sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'build/shuttle-tools'))
    import nbtlib as n

AIR = {'minecraft:air', 'minecraft:cave_air', 'minecraft:void_air'}


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def region_chunks(path):
    data = path.read_bytes()
    if not data:
        return  # Minecraft can leave an empty region file after removing its last entity.
    if len(data) < 8192:
        raise ValueError(f'Truncated region: {path}')
    for index in range(1024):
        sector = int.from_bytes(data[index * 4:index * 4 + 3], 'big')
        if not sector:
            continue
        offset = sector * 4096
        size = int.from_bytes(data[offset:offset + 4], 'big')
        compression = data[offset + 4]
        if compression & 128:
            raise ValueError(f'External chunk unsupported: {path}:{index}')
        payload = data[offset + 5:offset + 4 + size]
        if len(payload) != size - 1:
            raise ValueError(f'Truncated chunk: {path}:{index}')
        if compression == 1:
            payload = gzip.decompress(payload)
        elif compression == 2:
            payload = zlib.decompress(payload)
        elif compression != 3:
            raise ValueError(f'Unknown compression {compression}: {path}:{index}')
        yield n.File.parse(io.BytesIO(payload))


def read_ship(dimension):
    blocks, block_entities, entities = {}, {}, []
    versions = set()
    for path in sorted((dimension / 'region').glob('*.mca')):
        for chunk in region_chunks(path):
            versions.add(int(chunk['DataVersion']))
            cx, cz = int(chunk['xPos']), int(chunk['zPos'])
            for section in chunk['sections']:
                states = section.get('block_states')
                if not states:
                    continue
                palette = states['palette']
                if all(str(p['Name']) in AIR for p in palette):
                    continue
                sy = int(section['Y'])
                bits = max(4, (len(palette) - 1).bit_length())
                per_long = 64 // bits
                mask = (1 << bits) - 1
                packed = states.get('data')
                for i in range(4096):
                    # Since 1.16, each entry stays within one padded 64-bit word.
                    state_index = 0 if len(palette) == 1 else (int(packed[i // per_long]) >> ((i % per_long) * bits)) & mask
                    state = palette[state_index]
                    if str(state['Name']) in AIR:
                        continue
                    pos = (cx * 16 + (i & 15), sy * 16 + (i >> 8), cz * 16 + ((i >> 4) & 15))
                    if pos in blocks:
                        raise ValueError(f'Duplicate position {pos}')
                    blocks[pos] = deepcopy(state)
            for entity in chunk.get('block_entities', []):
                pos = tuple(int(entity[k]) for k in ('x', 'y', 'z'))
                block_entities[pos] = deepcopy(entity)
    for path in sorted((dimension / 'entities').glob('*.mca')):
        for chunk in region_chunks(path):
            entities.extend(deepcopy(list(chunk.get('Entities', []))))
    return blocks, block_entities, entities, versions


def ints(values):
    return n.List[n.Int](values)


def export(save, output):
    save, output = save.resolve(), output.resolve()
    if output == save or save in output.parents:
        raise ValueError('Output must be outside the source save')
    if output.exists():
        raise FileExistsError(f'Refusing to overwrite archive {output}')
    level = n.load(save / 'level.dat')
    dimension = save / 'dimensions/starboundmc/ship'
    if not (dimension / 'region').is_dir():
        raise ValueError('No saved ship dimension')
    source_files = sorted(p for p in save.rglob('*') if p.is_file() and p.name != 'session.lock')
    before = {str(p.relative_to(save)): sha(p) for p in source_files}
    blocks, block_entities, entities, versions = read_ship(dimension)
    if not blocks:
        raise ValueError('No ship blocks found')
    origin = tuple(min(p[i] for p in blocks) for i in range(3))
    maximum = tuple(max(p[i] for p in blocks) for i in range(3))
    size = tuple(maximum[i] - origin[i] + 1 for i in range(3))
    if size[0] * size[1] * size[2] > 1_000_000:
        raise ValueError(f'Unexpectedly large extent {size}; inspect detached blocks before exporting')
    if set(block_entities) - set(blocks):
        raise ValueError('Block entities without blocks')
    output.mkdir(parents=True)
    # Keep the original NBT, inventories, progress and entities intact in a full backup.
    backup = output / 'source-save.zip'
    with zipfile.ZipFile(backup, 'w', zipfile.ZIP_DEFLATED) as archive:
        for path in source_files:
            archive.write(path, path.relative_to(save).as_posix())
    with zipfile.ZipFile(backup) as archive:
        assert archive.testzip() is None
        for path in source_files:
            assert hashlib.sha256(archive.read(path.relative_to(save).as_posix())).hexdigest() == before[str(path.relative_to(save))]

    air = n.Compound({'Name': n.String('minecraft:air')})
    palette, indices, placed = [], {}, []
    # Explicit air preserves walls/windows removed by the user when applied to a hull.
    for y in range(size[1]):
        for z in range(size[2]):
            for x in range(size[0]):
                world = (origin[0] + x, origin[1] + y, origin[2] + z)
                state = blocks.get(world, air)
                key = state.snbt()
                if key not in indices:
                    indices[key] = len(palette)
                    palette.append(deepcopy(state))
                entry = n.Compound({'pos': ints([x, y, z]), 'state': n.Int(indices[key])})
                if world in block_entities:
                    tag = deepcopy(block_entities[world])
                    for coord in ('x', 'y', 'z'):
                        tag.pop(coord, None)
                    entry['nbt'] = tag
                placed.append(entry)

    # Structure contents include placed decoration, but not temporary dropped items,
    # mobs or the invisible seat mount. All entities remain in the full save backup.
    decoration_ids = {'minecraft:painting', 'minecraft:item_frame', 'minecraft:glow_item_frame', 'minecraft:armor_stand'}
    decorations = []
    for entity in entities:
        pos = [float(v) for v in entity.get('Pos', [])]
        if len(pos) != 3 or str(entity.get('id')) not in decoration_ids:
            continue
        if not all(origin[i] <= pos[i] <= maximum[i] + 1 for i in range(3)):
            continue
        relative = [pos[i] - origin[i] for i in range(3)]
        tag = deepcopy(entity)
        tag['Pos'] = n.List[n.Double](relative)
        anchor = [int(pos[i] // 1) - origin[i] for i in range(3)]
        if all(k in tag for k in ('TileX', 'TileY', 'TileZ')):
            anchor = [int(tag[k]) - origin[i] for i, k in enumerate(('TileX', 'TileY', 'TileZ'))]
            for i, k in enumerate(('TileX', 'TileY', 'TileZ')):
                tag[k] = n.Int(anchor[i])
        decorations.append(n.Compound({'pos': n.List[n.Double](relative), 'blockPos': ints(anchor), 'nbt': tag}))
    template = n.File({'DataVersion': n.Int(level['Data']['DataVersion']), 'size': ints(size),
                       'palette': n.List[n.Compound](palette), 'blocks': n.List[n.Compound](placed),
                       'entities': n.List[n.Compound](decorations)})
    template.save(output / 'shuttle-interior.nbt', gzipped=True)
    # Reload the exported format, reconstruct every block and NBT, and compare to source.
    loaded = n.load(output / 'shuttle-interior.nbt')
    recovered, recovered_entities = {}, {}
    for entry in loaded['blocks']:
        pos = tuple(int(entry['pos'][i]) + origin[i] for i in range(3))
        state = loaded['palette'][int(entry['state'])]
        if str(state['Name']) not in AIR:
            recovered[pos] = state.snbt()
        if 'nbt' in entry:
            tag = deepcopy(entry['nbt'])
            for i, coord in enumerate(('x', 'y', 'z')):
                tag[coord] = n.Int(pos[i])
            recovered_entities[pos] = tag
    assert recovered == {p: state.snbt() for p, state in blocks.items()}
    assert set(recovered_entities) == set(block_entities)
    for pos, tag in recovered_entities.items():
        assert tag == block_entities[pos], f'Block entity mismatch: {pos}'
    assert loaded['entities'] == template['entities']
    after_files = sorted(p for p in save.rglob('*') if p.is_file() and p.name != 'session.lock')
    after = {str(p.relative_to(save)): sha(p) for p in after_files}
    assert before == after, 'Source save changed during export; close Minecraft and retry'
    snapshot = {'coordinate_frame': 'world', 'blocks': [
        {'pos': list(p), 'state': state.unpack(), **({'nbt_snbt': block_entities[p].snbt()} if p in block_entities else {})}
        for p, state in sorted(blocks.items())]}
    (output / 'blocks.json').write_text(json.dumps(snapshot, ensure_ascii=False, indent=2), encoding='utf-8')
    manifest = {'exported_utc': datetime.now(timezone.utc).isoformat(), 'source_save': str(save),
                'source_level_name': str(level['Data']['LevelName']), 'data_versions': sorted(versions),
                'world_origin': list(origin), 'world_max_inclusive': list(maximum), 'size_xyz': list(size),
                'non_air_blocks': len(blocks), 'explicit_air_blocks': len(placed) - len(blocks),
                'block_entities': len(block_entities), 'template_decoration_entities': len(decorations),
                'source_entity_counts': dict(Counter(str(e.get('id')) for e in entities)),
                'materials': dict(sorted(Counter(str(s['Name']) for s in blocks.values()).items())),
                'source_sha256': before, 'verification': 'zip CRC and hashes; source unchanged; all block states and block entity NBT round-trip matched',
                'installed_as_default': False}
    manifest['artifact_sha256'] = {p.name: sha(p) for p in output.iterdir() if p.is_file()}
    (output / 'manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding='utf-8')
    print(json.dumps({k: v for k, v in manifest.items() if k not in {'source_sha256', 'materials'}}, ensure_ascii=True, indent=2))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('save', type=Path)
    parser.add_argument('output', type=Path)
    args = parser.parse_args()
    export(args.save, args.output)
