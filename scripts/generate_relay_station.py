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
# Raised roof caps and exposed transverse ribs break up the main pressure hull.
for z0, z1 in ((7, 13), (17, 23)):
    for x in range(10, 21):
        for z in range(z0, z1 + 1):
            put(x, 9, z, 'light_gray_concrete')
    for x in range(12, 19):
        for z in range(z0 + 1, z1): put(x, 10, z, 'smooth_stone')
for z in (6, 14, 16, 24):
    for x in range(7, 24):
        put(x, 3, z, 'polished_deepslate')
        put(x, 9, z, 'polished_deepslate')
    for x in (7, 23):
        for y in range(4, 9): put(x, y, z, 'light_gray_concrete')
for x in (8, 22):
    for z in range(7, 24):
        put(x, 7, z, 'light_gray_concrete')
        if z in (13, 14, 16, 17): put(x, 6, z, 'cyan_terracotta')

# Recessed docking porch, kept aligned with the original three-wide entrance.
for z in range(2, 6):
    for x in range(9, 14):
        put(x, 4, z, 'polished_deepslate')
        put(x, 8, z, 'light_gray_concrete')
    for x in (9, 13):
        for y in range(5, 8): put(x, y, z, 'gray_concrete')
for x in (9, 13):
    put(x, 5, 2, 'ochre_froglight', {'axis': 'y'})
    put(x, 7, 2, 'yellow_concrete')
for z in range(2, 10): put(11, 4, z, 'yellow_concrete')

# Two separated cell banks on each wing, joined by a narrow structural spar.
for x in (*range(2, 8), *range(23, 29)): put(x, 6, 15, 'polished_deepslate')
for left, right in ((1, 5), (25, 29)):
    for z in range(7, 24): put((left + right) // 2, 6, z, 'iron_block')
    for z0, z1 in ((7, 13), (17, 23)):
        for x in range(left, right + 1):
            for z in range(z0, z1 + 1):
                edge = x in (left, right) or z in (z0, z1)
                put(x, 7, z, 'polished_deepslate' if edge else 'blue_concrete')
                if not edge:
                    put(x, 8, z, 'blue_stained_glass', {})
    for z in (8, 22): put((left + right) // 2, 7, z, 'sea_lantern')

# Shallow stepped reflector with a central feed, within the snapshot envelope.
for y in (10, 11): put(15, y, 20, 'polished_deepslate')
for dx in range(-4, 5):
    for dz in range(-4, 5):
        radius = dx * dx + dz * dz
        if radius > 20: continue
        y = 12 if radius <= 5 else 13 if radius <= 13 else 14
        put(15 + dx, y, 20 + dz, 'smooth_quartz' if radius <= 13 else 'light_gray_concrete')
for y in (13, 14, 15): put(15, y, 20, 'end_rod', {'facing': 'up'})
for y in range(10, 14): put(19, y, 9, 'iron_bars')
put(19, 14, 9, 'redstone_block')

# Functional room dressing stays outside doorways and reward access.
for z in (8, 12, 18, 22):
    for x in (9, 21): put(x, 7, z, 'sea_lantern')
for x in (10, 12, 18, 20):
    put(x, 4, 14, 'cyan_terracotta')
for z in (18, 19, 20):
    put(9, 5, z, 'polished_blackstone')
    put(9, 6, z, 'cyan_stained_glass')
for x in (18, 19, 20): put(x, 7, 23, 'iron_trapdoor', {'facing': 'north', 'half': 'top', 'open': 'false', 'powered': 'false', 'waterlogged': 'false'})
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
assert all(0 < x < 30 and 0 < y < 16 and 0 < z < 30 for x, y, z in blocks), 'Keep snapshot boundary empty'
for z in range(2, 7):
    for x in (10, 11, 12):
        for y in (5, 6): assert (x, y, z) not in blocks, 'Docking passage must remain open'
walkable = {(x, z) for x in range(1, 30) for z in range(1, 30)
            if (x, 4, z) in blocks and (x, 5, z) not in blocks and (x, 6, z) not in blocks}
reached, pending = {(11, 2)}, [(11, 2)]
while pending:
    x, z = pending.pop()
    for neighbor in ((x - 1, z), (x + 1, z), (x, z - 1), (x, z + 1)):
        if neighbor in walkable and neighbor not in reached:
            reached.add(neighbor)
            pending.append(neighbor)
assert {(11, 10), (19, 10), (11, 20), (19, 20)} <= reached, 'All four rooms must be reachable'
for x, z in ((11, 21), (20, 10)):
    assert any(p in reached for p in ((x - 1, z), (x + 1, z), (x, z - 1), (x, z + 1))), 'Reward access blocked'
target = Path(__file__).resolve().parents[1] / 'src/main/resources/data/starboundmc/structure/abandoned_relay_station.nbt'
target.parent.mkdir(parents=True, exist_ok=True)
target.write_bytes(gzip.compress(b'\x0a\0\0' + payload(10, root), mtime=0))
print(f'{len(blocks)} blocks, {len(palette)} states -> {target}')
