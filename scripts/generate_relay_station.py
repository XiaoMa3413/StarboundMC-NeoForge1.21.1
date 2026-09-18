"""Author the small, four-room relay MVP as a reproducible vanilla structure NBT.

No runtime procedural replacement: the game places this checked-in template.
Outer empty space is reserved for player modifications and safe snapshot boundaries.
Run from any cwd with Python 3; no third-party dependencies.
"""
from pathlib import Path
import gzip
import json
import struct


def text(value): return (8, value)
def integer(value): return (3, value)
def byte(value): return (1, value)
def compound(value): return (10, value)
def array(kind, values): return (9, (kind, values))


def string(value):
    encoded = value.encode('utf-8')
    return struct.pack('>H', len(encoded)) + encoded


def payload(kind, value):
    if kind == 1: return struct.pack('>b', value)
    if kind == 3: return struct.pack('>i', value)
    if kind == 8: return string(value)
    if kind == 9:
        element, values = value
        return bytes([element]) + struct.pack('>i', len(values)) + b''.join(payload(element, v) for v in values)
    if kind == 10:
        return b''.join(bytes([k]) + string(n) + payload(k, v) for n, (k, v) in value.items()) + b'\0'
    raise ValueError(kind)


palette, indices, blocks = [], {}, {}


def put(x, y, z, name, properties=None, nbt=None):
    if name == 'air':
        blocks.pop((x, y, z), None)
        return
    key = (name, tuple(sorted((properties or {}).items())))
    if key not in indices:
        state = {'Name': text('minecraft:' + name)}
        if properties: state['Properties'] = compound({k: text(v) for k, v in properties.items()})
        indices[key] = len(palette)
        palette.append(state)
    entry = {'pos': array(3, [x, y, z]), 'state': integer(indices[key])}
    if nbt: entry['nbt'] = compound(nbt)
    blocks[(x, y, z)] = entry


for x in range(8, 23):
    for z in range(6, 25):
        put(x, 4, z, 'smooth_stone')
        put(x, 8, z, 'polished_deepslate')
        for y in range(5, 8):
            if x in (8, 22) or z in (6, 24):
                put(x, y, z, 'gray_concrete' if y != 6 else 'tinted_glass')

# Cross walls divide an entry corridor, control room, storage and workshop.
for y in range(5, 8):
    for z in range(7, 24): put(15, y, z, 'polished_deepslate')
    for x in range(9, 22): put(x, y, 15, 'polished_deepslate')
for y in (5, 6):
    for z in (10, 11, 20, 21): put(15, y, z, 'air')
    for x in (11, 12, 18, 19): put(x, y, 15, 'air')
    for x in (10, 11, 12): put(x, y, 6, 'air')
for x, z in ((11, 10), (19, 10), (11, 20), (19, 20)):
    put(x, 8, z, 'sea_lantern')
for x in range(1, 8):
    for z in range(9, 22):
        put(x, 7, z, 'iron_block' if x in (1, 7) or z in (9, 21) else 'blue_stained_glass')
        put(30-x, 7, z, 'iron_block' if x in (1, 7) or z in (9, 21) else 'blue_stained_glass')
for y in range(9, 13): put(15, y, 20, 'iron_block' if y == 9 else 'iron_bars')
for x in range(13, 17): put(x, 12, 20, 'iron_bars')
for x in (10, 11, 12): put(x, 5, 23, 'observer', {'facing': 'north', 'powered': 'false'})
put(19, 5, 22, 'crafting_table')
put(20, 5, 22, 'furnace', {'facing': 'north', 'lit': 'false'})
put(21, 5, 22, 'anvil', {'facing': 'north'})
put(9, 5, 9, 'iron_bars')
put(9, 5, 10, 'iron_bars')


def barrel(x, z, items, title):
    stacks = [{'Slot': byte(i), 'id': text(item), 'count': integer(count)} for i, (item, count) in enumerate(items)]
    put(x, 5, z, 'barrel', {'facing': 'up', 'open': 'false'}, {
        'id': text('minecraft:barrel'), 'Items': array(10, stacks),
        'CustomName': text(json.dumps({'translate': title}, separators=(',', ':'))),
    })


barrel(11, 21, [('starboundmc:relay_data_core', 1), ('starboundmc:matter_manipulator_module', 1)], 'container.starboundmc.relay.control')
barrel(20, 10, [('starboundmc:oxygen_canister', 1), ('starboundmc:oxygen_canister', 1), ('starboundmc:emergency_food_can', 3)], 'container.starboundmc.relay.supply')
root = {
    'DataVersion': integer(3955), 'size': array(3, [31, 17, 31]),
    'palette': array(10, palette), 'blocks': array(10, list(blocks.values())), 'entities': array(10, []),
}
target = Path(__file__).resolve().parents[1] / 'src/main/resources/data/starboundmc/structure/abandoned_relay_station.nbt'
target.parent.mkdir(parents=True, exist_ok=True)
target.write_bytes(gzip.compress(b'\x0a\0\0' + payload(10, root), mtime=0))
print(f'{len(blocks)} blocks, {len(palette)} states -> {target}')
