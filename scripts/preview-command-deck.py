"""Orthographic textured model inspection, not an in-game screenshot."""
import json
from pathlib import Path
import numpy as np
from PIL import Image, ImageDraw, ImageEnhance

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/starboundmc'
canvas = Image.new('RGB', (1200, 680), '#0b1420')
depth_buffer = np.full((680, 1200), -np.inf)
grid_y, grid_x = np.mgrid[:680, :1200]


def render(name, center):
    model = json.loads((ASSETS / f'models/block/{name}.json').read_text())
    textures = {}
    for key, resource in model['textures'].items():
        im = Image.open(ASSETS / ('textures/' + resource.split(':')[1] + '.png')).convert('RGB')
        textures[key] = im.crop((0, 0, 48, 32)) if key == 'screen' else im
    faces = []
    for part in model['elements']:
        x, y, z = part['from']; X, Y, Z = part['to']
        quads = {'north': [(x,Y,z),(X,Y,z),(X,y,z),(x,y,z)],
                 'east': [(X,Y,z),(X,Y,Z),(X,y,Z),(X,y,z)],
                 'up': [(x,Y,Z),(X,Y,Z),(X,Y,z),(x,Y,z)]}
        for side, quad in quads.items():
            points = np.array(quad) - 8
            projected = [(center[0] + (p[0]+p[2])*15,
                          center[1] + (p[0]-p[2])*7-p[1]*20) for p in points]
            face = part['faces'][side]
            im = textures[face['texture'][1:]]
            uv = [v * (im.width if i % 2 == 0 else im.height) / 16
                  for i, v in enumerate(face['uv'])]
            # Vanilla face UVs start at the north-west corner on the top face.
            if side == 'up':
                uv[1], uv[3] = uv[3], uv[1]
            faces.append((points @ np.array([1,.7,-1]), projected, im, side, uv))
    for depths, quad, tile, side, uv in faces:
        tile = ImageEnhance.Brightness(tile).enhance({'up':1,'north':.85,'east':.65}[side])
        pts = np.array([[*quad[i],1] for i in (0,1,3)])
        u = np.linalg.solve(pts, [uv[0],uv[2],uv[0]])
        v = np.linalg.solve(pts, [uv[1],uv[1],uv[3]])
        warped = tile.transform(canvas.size, Image.Transform.AFFINE, tuple(u)+tuple(v), Image.Resampling.NEAREST)
        mask = Image.new('L', canvas.size)
        ImageDraw.Draw(mask).polygon(quad, fill=255)
        depth_plane = np.linalg.solve(pts, depths[[0,1,3]])
        depth = depth_plane[0]*grid_x + depth_plane[1]*grid_y + depth_plane[2]
        visible = (np.asarray(mask) > 0) & (depth >= depth_buffer - 1e-5)
        depth_buffer[visible] = depth[visible]
        mask = Image.fromarray((visible * 255).astype('uint8'))
        canvas.paste(warped, (0,0), mask)


render('ship_ai_terminal', (240, 280))
render('starmap_terminal', (860, 280))
draw = ImageDraw.Draw(canvas)
draw.text((110, 520), 'NOVA / WALL TERMINAL', fill='#9de6e8', font_size=22)
draw.text((720, 520), 'STARMAP / NAVIGATION DESK', fill='#9de6e8', font_size=22)
draw.text((110, 574), 'TEXTURED MODEL PREVIEW - NOT AN IN-GAME CAPTURE', fill='#6c8798', font_size=18)
destination = ROOT / 'output/command-deck-model-preview.png'
destination.parent.mkdir(parents=True, exist_ok=True)
canvas.save(destination)
print(destination)
