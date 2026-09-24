"""Package the actual Blockbench MCP starmap terminal export.

Geometry is authored and exported by Blockbench. This script only validates the
export, extracts embedded textures, and writes the NeoForge OBJ model wrapper.
"""
import base64
import json
import math
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ART = ROOT / "docs/models"
ASSETS = ROOT / "src/main/resources/assets/starboundmc"
MODELS = ASSETS / "models/block"


def main():
    project = json.loads((ART / "starmap-console-textured-v1.bbmodel").read_text(encoding="utf-8"))
    textures = {"m_" + texture["uuid"]: texture for texture in project["textures"]}
    used = {}
    lines = []
    objects = 0
    vertices = []
    uvs = []
    normals = []
    faces = []
    for source_line in (ART / "starmap-console-export.obj").read_text(encoding="utf-8").splitlines():
        line = source_line
        if line.startswith("mtllib "):
            line = "mtllib starmap_console.mtl"
        elif line.startswith("usemtl "):
            material_id = line.split()[1]
            texture = textures[material_id]
            name = texture["name"]
            assert re.fullmatch(r"terminal_[a-z]+", name), name
            used[name] = texture
            line = "usemtl " + name
        elif line.startswith("o "):
            objects += 1
            line = "o " + re.sub(r"[^a-z0-9]+", "_", line[2:].lower()).strip("_")
        elif line.startswith(("v ", "vt ", "vn ")):
            kind, *values = line.split()
            point = tuple(float(value) for value in values)
            assert all(math.isfinite(value) for value in point), source_line
            if kind == "v":
                assert all(-1e-6 <= point[i] <= 1.000001 for i in (0, 2)), source_line
                assert -1e-6 <= point[1] <= 1.001, source_line
                vertices.append(point)
            elif kind == "vt":
                assert all(-1e-6 <= value <= 1.000001 for value in point), source_line
                uvs.append(point)
            else:
                normals.append(point)
        elif line.startswith("f "):
            face = [tuple(int(index) for index in corner.split("/")) for corner in line.split()[1:]]
            assert len(face) in (3, 4), source_line
            faces.append(face)
        lines.append(line)

    assert objects == len(project["elements"]), (objects, len(project["elements"]))
    for face in faces:
        for vertex, uv, normal in face:
            assert 1 <= vertex <= len(vertices)
            assert 1 <= uv <= len(uvs)
            assert 1 <= normal <= len(normals)
    assert set(used) == {
        "terminal_amber", "terminal_cyan", "terminal_edge", "terminal_graphite",
        "terminal_chart", "terminal_instruments", "terminal_titanium",
    }, used

    MODELS.mkdir(parents=True, exist_ok=True)
    for name, texture in used.items():
        source = texture["source"]
        assert source.startswith("data:image/png;base64,"), name
        (ASSETS / f"textures/block/{name}.png").write_bytes(
            base64.b64decode(source.split(",", 1)[1])
        )
    (MODELS / "starmap_console.obj").write_text("\n".join(lines) + "\n", encoding="utf-8")

    mtl = "# Blockbench MCP materials; chart and instrument surfaces are emissive.\n"
    for name in sorted(used):
        ambient = "1 1 1" if name in {"terminal_cyan", "terminal_chart", "terminal_instruments"} else "0 0 0"
        mtl += f"\nnewmtl {name}\nKa {ambient}\nKd 1 1 1\nmap_Kd starboundmc:block/{name}\n"
    (MODELS / "starmap_console.mtl").write_text(mtl, encoding="utf-8")

    model = {
        "parent": "minecraft:block/block",
        "loader": "neoforge:obj",
        "model": "starboundmc:models/block/starmap_console.obj",
        "automatic_culling": False,
        "shade_quads": True,
        "flip_v": True,
        "emissive_ambient": True,
        "render_type": "minecraft:solid",
        "ambientocclusion": False,
        "textures": {"particle": "starboundmc:block/terminal_titanium"},
    }
    (MODELS / "starmap_terminal.json").write_text(json.dumps(model, indent=2) + "\n", encoding="utf-8")

    item = {
        "parent": "starboundmc:block/starmap_terminal",
        "display": {
            "gui": {"rotation": [25, 225, 0], "translation": [0, -1.5, 0], "scale": [0.62, 0.62, 0.62]},
            "ground": {"translation": [0, 2, 0], "scale": [0.34, 0.34, 0.34]},
            "fixed": {"rotation": [0, 180, 0], "translation": [0, -1.5, -2], "scale": [0.58, 0.58, 0.58]},
        },
    }
    (ASSETS / "models/item/starmap_terminal.json").write_text(json.dumps(item, indent=2) + "\n", encoding="utf-8")
    print(f"Starmap terminal: {objects} meshes, {len(faces)} faces, {len(used)} materials; OBJ indices, UVs and bounds valid.")


if __name__ == "__main__":
    main()
