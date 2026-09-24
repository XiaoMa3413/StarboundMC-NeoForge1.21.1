"""Package the actual Blockbench MCP export without regenerating its geometry."""
import base64
import json
import math
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ART = ROOT / "docs/models"
ASSETS = ROOT / "src/main/resources/assets/starboundmc"


def main():
    project = json.loads((ART / "captain-chair-textured-v1.bbmodel").read_text(encoding="utf-8"))
    textures = {"m_" + t["uuid"]: t for t in project["textures"]}
    used, lines, vertices, texcoords, normals, faces = {}, [], [], [], [], []
    objects = 0
    for line in (ART / "captain-chair-export.obj").read_text(encoding="utf-8").splitlines():
        if line.startswith("mtllib "):
            line = "mtllib captain_chair.mtl"
        elif line.startswith("usemtl "):
            texture = textures[line.split()[1]]
            name = texture["name"]
            assert re.fullmatch(r"chair_[a-z_]+", name), name
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
                assert 0 <= point[0] <= 1 and 0 <= point[2] <= 1, line
                assert 0 <= point[1] <= 1.42, line
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

    assert objects == len(project["elements"]), "OBJ export must include the whole chair"
    for face in faces:
        for vertex, uv, normal in face:
            assert 1 <= vertex <= len(vertices)
            assert 1 <= uv <= len(texcoords)
            assert 1 <= normal <= len(normals)

    for name, texture in used.items():
        source = texture["source"]
        assert source.startswith("data:image/png;base64,"), name
        (ASSETS / f"textures/block/{name}.png").write_bytes(base64.b64decode(source.split(",", 1)[1]))
    models = ASSETS / "models/block"
    (models / "captain_chair.obj").write_text("\n".join(lines) + "\n", encoding="utf-8")
    mtl = "# Blockbench materials; only the small instrument displays are emissive.\n"
    for name in sorted(used):
        ambient = "1 1 1" if name == "chair_instruments" else "0 0 0"
        mtl += f"\nnewmtl {name}\nKa {ambient}\nKd 1 1 1\nmap_Kd starboundmc:block/{name}\n"
    (models / "captain_chair.mtl").write_text(mtl, encoding="utf-8")
    model = {
        "parent": "minecraft:block/block", "loader": "neoforge:obj",
        "model": "starboundmc:models/block/captain_chair.obj",
        "automatic_culling": False, "shade_quads": True, "flip_v": True,
        "emissive_ambient": True, "render_type": "minecraft:solid",
        "ambientocclusion": False, "textures": {"particle": "starboundmc:block/chair_titanium"},
    }
    (models / "captain_chair.json").write_text(json.dumps(model, indent=2) + "\n")
    item = {"parent": "starboundmc:block/captain_chair", "display": {
        "gui": {"rotation": [25, 225, 0], "translation": [0, -2.5, 0], "scale": [.54, .54, .54]},
        "ground": {"translation": [0, 2, 0], "scale": [.3, .3, .3]},
        "fixed": {"rotation": [0, 180, 0], "translation": [0, -2, -2], "scale": [.5, .5, .5]},
        "firstperson_righthand": {"rotation": [0, 135, 0], "translation": [0, 0, 0], "scale": [.35, .35, .35]},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [.3, .3, .3]},
    }}
    (ASSETS / "models/item/captain_chair.json").write_text(json.dumps(item, indent=2) + "\n")
    print(f"Captain chair: {objects} meshes, {len(faces)} faces, {len(used)} materials; indices, UVs and bounds valid.")


if __name__ == "__main__":
    main()
