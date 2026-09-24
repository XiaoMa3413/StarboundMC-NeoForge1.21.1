"""Package the actual Blockbench MCP export; never synthesize replacement geometry."""
import base64
import io
import json
import math
import re
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ART = ROOT / "docs/models"
ASSETS = ROOT / "src/main/resources/assets/starboundmc"


def main():
    project = json.loads((ART / "nova-terminal-textured-v1.bbmodel").read_text(encoding="utf-8"))
    textures = {"m_" + t["uuid"]: t for t in project["textures"]}
    used, lines, vertices, texcoords, normals, faces = {}, [], [], [], [], []
    objects = 0
    for line in (ART / "nova-terminal-export.obj").read_text(encoding="utf-8").splitlines():
        if line.startswith("mtllib "):
            line = "mtllib nova_terminal.mtl"
        elif line.startswith("usemtl "):
            texture = textures[line.split()[1]]
            name = texture["name"]
            assert re.fullmatch(r"nova_[a-z_]+", name), name
            used[name] = texture
            line = "usemtl " + name
        elif line.startswith("o "):
            objects += 1
            line = "o " + re.sub(r"[^a-z0-9]+", "_", line[2:].lower()).strip("_")
        elif line.startswith(("v ", "vt ", "vn ")):
            kind, *values = line.split()
            point = tuple(map(float, values))
            assert all(math.isfinite(v) for v in point), line
            if kind == "v":
                assert all(0 <= v <= 1 for v in point), line
                vertices.append(point)
            elif kind == "vt":
                assert all(-1e-6 <= v <= 1.000001 for v in point), line
                texcoords.append(point)
            else:
                normals.append(point)
        elif line.startswith("f "):
            face = [tuple(map(int, corner.split("/"))) for corner in line.split()[1:]]
            assert len(face) in (3, 4), line
            faces.append(face)
        lines.append(line)
    assert objects == len(project["elements"]), "Export must include the entire terminal"
    for face in faces:
        for vertex, uv, normal in face:
            assert 1 <= vertex <= len(vertices)
            assert 1 <= uv <= len(texcoords)
            assert 1 <= normal <= len(normals)
        points = [vertices[v - 1] for v, _, _ in face]
        a, b = [[points[i][j] - points[0][j] for j in range(3)] for i in (1, 2)]
        cross = [a[1]*b[2]-a[2]*b[1], a[2]*b[0]-a[0]*b[2], a[0]*b[1]-a[1]*b[0]]
        assert sum(v*v for v in cross) > 1e-16, "Degenerate face"

    for name, texture in used.items():
        source = texture["source"]
        assert source.startswith("data:image/png;base64,"), name
        data = base64.b64decode(source.split(",", 1)[1])
        image = Image.open(io.BytesIO(data)).convert("RGBA")
        assert image.getchannel("A").getextrema() == (255, 255), "Solid OBJ requires opaque materials"
        (ASSETS / f"textures/block/{name}.png").write_bytes(data)

    animation = {"animation": {"width": 128, "height": 112, "frametime": 2, "interpolate": False}}
    (ASSETS / "textures/block/nova_screen.png.mcmeta").write_text(json.dumps(animation, indent=2) + "\n")
    strip = Image.open(ASSETS / "textures/block/nova_screen.png")
    assert strip.size == (128, 112 * 48)
    frames = [strip.crop((0, i*112, 128, (i+1)*112)) for i in range(48)]
    assert len({f.tobytes() for f in frames}) > 24, "Ambient animation must contain actual changing pixels"
    (ART / "previews").mkdir(exist_ok=True)
    frames[0].resize((512,448), Image.Resampling.NEAREST).save(ART / "previews/nova-screen.png")
    frames[0].save(ART / "previews/nova-screen.gif", save_all=True, append_images=frames[1:], duration=100, loop=0, disposal=2)
    models = ASSETS / "models/block"
    (models / "nova_terminal.obj").write_text("\n".join(lines) + "\n", encoding="utf-8")
    mtl = "# Screen and small status markers use ambient emissivity.\n"
    for name in sorted(used):
        ambient = "1 1 1" if name in ("nova_screen", "nova_cyan") else "0 0 0"
        mtl += f"\nnewmtl {name}\nKa {ambient}\nKd 1 1 1\nmap_Kd starboundmc:block/{name}\n"
    (models / "nova_terminal.mtl").write_text(mtl, encoding="utf-8")
    model = {
        "parent": "minecraft:block/block", "loader": "neoforge:obj",
        "model": "starboundmc:models/block/nova_terminal.obj",
        "automatic_culling": False, "shade_quads": True, "flip_v": True,
        "emissive_ambient": True, "render_type": "minecraft:solid",
        "ambientocclusion": False, "textures": {"particle": "starboundmc:block/nova_titanium"},
    }
    (models / "ship_ai_terminal.json").write_text(json.dumps(model, indent=2) + "\n")
    item = {"parent": "starboundmc:block/ship_ai_terminal", "display": {
        "gui": {"rotation": [12, 155, 0], "translation": [0, 0, 0], "scale": [.78, .78, .78]},
        "ground": {"translation": [0, 3, 0], "scale": [.4, .4, .4]},
        "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, -2], "scale": [.65, .65, .65]},
        "firstperson_righthand": {"rotation": [0, 135, 0], "scale": [.4, .4, .4]},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [.35, .35, .35]},
    }}
    (ASSETS / "models/item/ship_ai_terminal.json").write_text(json.dumps(item, indent=2) + "\n")
    print(f"NOVA terminal: {objects} meshes, {len(faces)} valid faces, {len(used)} materials, 48 animated frames.")


if __name__ == "__main__":
    main()
