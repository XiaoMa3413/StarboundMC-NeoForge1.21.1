"""Rebuild command-deck models and tailored edge art; preserve the large-panel atlas."""
import json
from pathlib import Path
from command_deck_edge_art import EdgeAtlas

ASSETS = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/starboundmc'
TILE = {'casing': (0, 0), 'frame': (8, 0),
        'controls': (0, 8), 'chart': (8, 8)}
EDGES = EdgeAtlas()


def dimensions(start, end, side):
    dx, dy, dz = (b - a for a, b in zip(start, end))
    return {'north': (dx, dy), 'south': (dx, dy),
            'east': (dz, dy), 'west': (dz, dy),
            'up': (dx, dz), 'down': (dx, dz)}[side]


def face_uv(start, end, side, kind):
    if kind == 'screen':
        return [0, 0, 16, 16]
    width, height = dimensions(start, end, side)
    # Each material occupies 8 UV units. Keep equal scale in BOTH axes:
    # crop narrow faces, never squash a whole detailed panel onto a thin edge.
    u, v = TILE[kind]
    if kind == 'chart':
        # The chart is a display: fit its full width and crop symmetrically,
        # with equal density in both axes so the orbit is never stretched.
        scale = 8 / max(width, height)
        return [u + (8 - width * scale) / 2, v + (8 - height * scale) / 2,
                u + (8 + width * scale) / 2, v + (8 + height * scale) / 2]
    u += (16 - width) / 4
    v += (16 - height) / 4
    return [u, v, u + width / 2, v + height / 2]


def box(start, end, material='casing', overrides=None):
    faces = {}
    for side in ('north', 'south', 'east', 'west', 'up', 'down'):
        kind = (overrides or {}).get(side, material)
        width, height = dimensions(start, end, side)
        tailored = kind not in ('screen', 'chart') and (
            min(width, height) <= 4 or side in ('east', 'west'))
        faces[side] = {'uv': EDGES.region(kind, width, height) if tailored else face_uv(start, end, side, kind),
                       'texture': '#edges' if tailored else '#screen' if kind == 'screen' else '#chart' if kind == 'chart' else '#atlas'}
    return {'from': start, 'to': end, 'faces': faces}


def write(name, elements):
    model = {'parent': 'minecraft:block/block',
             'textures': {'atlas': 'starboundmc:block/command_deck_atlas',
                          'edges': 'starboundmc:block/command_deck_edges',
                          'chart': 'starboundmc:block/command_deck_atlas',
                          'screen': 'starboundmc:block/ship_ai_terminal_screen',
                          'particle': 'starboundmc:block/command_deck_atlas'},
             'elements': elements}
    (ASSETS / f'models/block/{name}.json').write_text(
        json.dumps(model, indent=2) + '\n', encoding='utf-8')
    print(f'{name}: {len(elements)} elements')


def main():
    # North-facing wall terminal. Keep the existing placement footprint.
    ai = [box([2, 2, 12], [14, 14, 16]),
          box([1, 2, 10], [15, 14, 12], 'frame'),
          box([2.5, 4, 9.7], [13.5, 12.5, 10], overrides={'north': 'screen'}),
          box([3, 1, 9], [13, 3, 14], overrides={'up': 'controls'}),
          box([1, 3, 9.5], [2.5, 13, 10], 'frame'),
          box([13.5, 3, 9.5], [15, 13, 10], 'frame'),
          box([2.5, 12.5, 9.5], [13.5, 14, 10], 'frame'),
          box([2.5, 3, 9.5], [13.5, 4, 10], 'frame')]
    for x in (1.25, 14):
        for y in (4, 6, 8, 10):
            ai.append(box([x, y, 9.25], [x + .75, y + .5, 9.5], 'controls'))
    write('ship_ai_terminal', ai)

    # Navigation table: pedestal, armored tabletop, recessed chart and control lip.
    nav = [box([2, 0, 2], [14, 2, 14], 'frame'),
           box([4, 2, 4], [12, 8, 12]),
           box([1, 8, 1], [15, 10, 15], 'frame'),
           box([2, 10, 4], [14, 10.25, 14], overrides={'up': 'chart'}),
           box([2, 10, 1], [14, 10.5, 3.5], overrides={'up': 'controls'}),
           box([1, 10, 3.5], [2, 11, 15], 'frame'),
           box([14, 10, 3.5], [15, 11, 15], 'frame'),
           box([2, 10, 14], [14, 11, 15], 'frame')]
    for x in (2, 12):
        nav.append(box([x, 2, 5], [x + 2, 8, 11], 'frame'))
    for x in (1, 13):
        nav.append(box([x, 11, 13], [x + 2, 13, 15], 'controls'))
    write('starmap_terminal', nav)
    EDGES.image.save(ASSETS / 'textures/block/command_deck_edges.png')
    print(f'{len(EDGES.regions)} complete dimension-specific edge panels')


if __name__ == '__main__':
    main()
