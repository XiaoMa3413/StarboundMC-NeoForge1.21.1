"""Package the actual Blockbench MCP fuel control panel export.

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
    project = json.loads((ART / "fuel-panel-textured-v1.bbmodel").read_text(encoding="utf-8"))
    textures = {"m_" + texture["uuid"]: texture for texture in project["textures"]}
    used = {}
    lines = []
    objects = 0
    vertices = []
    uvs = []
    normals = []
    faces = []
    for source_line in (ART / "fuel-panel-export.obj").read_text(encoding="utf-8").splitlines():
        line = source_line
        if line.startswith("mtllib "):
            line = "mtllib fuel_panel.mtl"
        elif line.startswith("usemtl "):
            material_id = line.split()[1]
            texture = textures[material_id]
            name = texture["name"]
            assert re.fullmatch(r"fuel_panel_[a-z]+", name), name
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
                assert 0.125 - 1e-6 <= point[1] <= 0.875 + 1e-6, source_line
                assert 12.35 / 16 - 1e-6 <= point[2] <= 1.000001, source_line
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
        "fuel_panel_amber", "fuel_panel_rubber", "fuel_panel_edge", "fuel_panel_graphite",
        "fuel_panel_screen", "fuel_panel_markings", "fuel_panel_titanium",
    }, used

    MODELS.mkdir(parents=True, exist_ok=True)
    for name, texture in used.items():
        source = texture["source"]
        assert source.startswith("data:image/png;base64,"), name
        (ASSETS / f"textures/block/{name}.png").write_bytes(
            base64.b64decode(source.split(",", 1)[1])
        )
    (MODELS / "fuel_panel.obj").write_text("\n".join(lines) + "\n", encoding="utf-8")

    mtl = "# Blockbench MCP materials; only the amber display surface is emissive.\n"
    for name in sorted(used):
        ambient = "1 1 1" if name in {"fuel_panel_screen"} else "0 0 0"
        mtl += f"\nnewmtl {name}\nKa {ambient}\nKd 1 1 1\nmap_Kd starboundmc:block/{name}\n"
    (MODELS / "fuel_panel.mtl").write_text(mtl, encoding="utf-8")

    model = {
        "parent": "minecraft:block/block",
        "loader": "neoforge:obj",
        "model": "starboundmc:models/block/fuel_panel.obj",
        "automatic_culling": False,
        "shade_quads": True,
        "flip_v": True,
        "emissive_ambient": True,
        "render_type": "minecraft:solid",
        "ambientocclusion": False,
        "textures": {"particle": "starboundmc:block/fuel_panel_titanium"},
    }
    (MODELS / "fuel_controller.json").write_text(json.dumps(model, indent=2) + "\n", encoding="utf-8")

    item = {
        "parent": "starboundmc:block/fuel_controller",
        "display": {
            "gui": {"rotation": [25, 225, 0], "translation": [0, 0, 0], "scale": [0.62, 0.62, 0.62]},
            "ground": {"translation": [0, 2, 0], "scale": [0.34, 0.34, 0.34]},
            "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, -5], "scale": [0.58, 0.58, 0.58]},
        },
    }
    (ASSETS / "models/item/fuel_controller.json").write_text(json.dumps(item, indent=2) + "\n", encoding="utf-8")
    print(f"Fuel control panel: {objects} meshes, {len(faces)} faces, {len(used)} materials; OBJ indices, UVs and bounds valid.")


if __name__ == "__main__":
    main()
