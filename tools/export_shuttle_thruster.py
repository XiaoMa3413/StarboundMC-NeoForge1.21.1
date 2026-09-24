"""Package the Blockbench-authored OBJ and embedded textures for NeoForge.

Export project + OBJ through Blockbench MCP before running this script.
"""
import base64
import json
import math
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
AUTHORING = ROOT / "docs/models"
ASSETS = ROOT / "src/main/resources/assets/starboundmc"


def main():
    project = json.loads((AUTHORING / "shuttle-thruster-textured-v1.bbmodel").read_text())
    materials = {}
    for texture in project["textures"]:
        name = texture["name"].removesuffix(".png")
        assert re.fullmatch(r"shuttle_[a-z_]+", name), name
        source = texture["source"]
        assert source.startswith("data:image/png;base64,"), name
        folder = "effect" if name == "shuttle_exhaust" else "block"
        destination = ASSETS / f"textures/{folder}/{name}.png"
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_bytes(base64.b64decode(source.split(",", 1)[1]))
        materials["m_" + texture["uuid"]] = name

    obj = (AUTHORING / "shuttle-thruster-export.obj").read_text()
    lines = []
    used = set()
    for line in obj.splitlines():
        if line.startswith("mtllib "):
            line = "mtllib shuttle_thruster.mtl"
        elif line.startswith("o "):
            line = "o " + re.sub(r"[^a-z0-9]+", "_", line[2:].lower()).strip("_")
        elif line.startswith("usemtl "):
            name = materials[line.split()[1]]
            used.add(name)
            line = "usemtl " + name
        elif line.startswith(("v ", "vt ", "vn ")):
            values = [float(v) for v in line.split()[1:]]
            assert all(math.isfinite(v) for v in values), line
            if line.startswith(("v ", "vt ")):
                assert all(-1e-6 <= v <= 1.000001 for v in values), line
        lines.append(line)
    (ASSETS / "models/block/shuttle_thruster.obj").write_text("\n".join(lines) + "\n")
    mtl = "# Authored in Blockbench; texture paths refer to the Minecraft block atlas.\n"
    for name in sorted(used):
        mtl += f"\nnewmtl {name}\nKa 0 0 0\nKd 1 1 1\nmap_Kd starboundmc:block/{name}\n"
    (ASSETS / "models/block/shuttle_thruster.mtl").write_text(mtl)
    print(f"Exported {len(used)} materials, {len(project['textures'])} textures and {len(lines)} OBJ lines")


if __name__ == "__main__":
    main()
