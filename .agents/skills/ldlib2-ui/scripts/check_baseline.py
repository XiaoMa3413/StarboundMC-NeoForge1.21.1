#!/usr/bin/env python3
"""Check the StarboundMC/LDLib2 version baseline without modifying the repo."""

from __future__ import annotations

import argparse
from pathlib import Path
import sys


def parse_properties(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        values[key.strip()] = value.strip()
    return values


def find_repo(start: Path) -> Path | None:
    current = start.resolve()
    if current.is_file():
        current = current.parent
    for candidate in (current, *current.parents):
        if (candidate / "gradle.properties").is_file() and (candidate / ".agents").is_dir():
            return candidate
    return None


def value_or_missing(values: dict[str, str], key: str) -> str:
    return values.get(key, "<missing>")


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Compare the project LDLib2 dependency with the local read-only source checkout."
    )
    parser.add_argument(
        "--repo",
        type=Path,
        help="Repository root. If omitted, search upward from the current directory.",
    )
    args = parser.parse_args()

    repo = args.repo.resolve() if args.repo else find_repo(Path.cwd())
    if repo is None or not (repo / "gradle.properties").is_file():
        print("ERROR: could not locate the StarboundMC repository root.", file=sys.stderr)
        return 1

    project = parse_properties(repo / "gradle.properties")
    required_project_keys = ("minecraft_version", "neo_version", "ldlib2_version")
    missing = [key for key in required_project_keys if key not in project]
    if missing:
        print(f"ERROR: missing project properties: {', '.join(missing)}", file=sys.stderr)
        return 1

    print(f"repo:      {repo}")
    print(
        "project:   "
        f"Minecraft {project['minecraft_version']}, "
        f"NeoForge {project['neo_version']}, "
        f"LDLib2 {project['ldlib2_version']}"
    )

    source_properties = repo / "LDLib2" / "gradle.properties"
    if not source_properties.is_file():
        print("SOURCE:    missing local LDLib2 checkout")
        print(
            "FALLBACK:  inspect the Gradle-resolved sources/jar for "
            f"ldlib2-neoforge-{project['minecraft_version']}:{project['ldlib2_version']}"
        )
        return 2

    source = parse_properties(source_properties)
    source_mc = value_or_missing(source, "minecraft_version")
    source_version = value_or_missing(source, "mod_version")
    print(f"source:    Minecraft {source_mc}, LDLib2 {source_version}")

    mismatches: list[str] = []
    if source_mc != project["minecraft_version"]:
        mismatches.append(
            f"Minecraft project={project['minecraft_version']} source={source_mc}"
        )
    if source_version != project["ldlib2_version"]:
        mismatches.append(
            f"LDLib2 project={project['ldlib2_version']} source={source_version}"
        )

    if mismatches:
        print("MISMATCH:  " + "; ".join(mismatches), file=sys.stderr)
        print("ACTION:    use the Gradle-resolved dependency as authority and refresh the checkout.")
        return 2

    print("OK:        local LDLib2 source matches the project dependency baseline")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
