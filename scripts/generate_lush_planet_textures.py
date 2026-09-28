#!/usr/bin/env python3
"""Prepare the cloud-free Lush surface and its separate cloud shell texture.

Sources are the 8K Earth Day Map and Earth Clouds images from Solar System Scope:
https://www.solarsystemscope.com/textures/ (CC BY 4.0).

Example:
    python scripts/generate_lush_planet_textures.py \
        --surface-source 8k_earth_daymap.jpg \
        --cloud-source 8k_earth_clouds.jpg
"""

import argparse
from pathlib import Path

import numpy as np
from PIL import Image


WIDTH = 4096
HEIGHT = 2048
CLOUD_BLACK_POINT = 32.0
TEXTURE_DIRECTORY = (Path(__file__).resolve().parents[1]
                     / "src/main/resources/assets/starboundmc/textures/planet")


def load_equirectangular(path: Path, label: str) -> Image.Image:
    with Image.open(path) as source:
        if source.width != source.height * 2:
            raise ValueError(f"{label} must be an equirectangular 2:1 image: {path}")
        return source.convert("RGB")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--surface-source", required=True, type=Path,
                        help="Solar System Scope Earth Day Map source image")
    parser.add_argument("--cloud-source", required=True, type=Path,
                        help="Solar System Scope Earth Clouds source image")
    args = parser.parse_args()

    surface = load_equirectangular(args.surface_source, "surface source")
    surface = surface.resize((WIDTH, HEIGHT), Image.Resampling.LANCZOS).convert("RGBA")
    surface_path = TEXTURE_DIRECTORY / "lush.png"
    surface.save(surface_path, format="PNG", optimize=True)

    cloud_rgb = load_equirectangular(args.cloud_source, "cloud source")
    cloud_rgb = cloud_rgb.resize((WIDTH, HEIGHT), Image.Resampling.LANCZOS)
    cloud_pixels = np.asarray(cloud_rgb, dtype=np.float32)
    luminance = (0.2126 * cloud_pixels[..., 0]
                 + 0.7152 * cloud_pixels[..., 1]
                 + 0.0722 * cloud_pixels[..., 2])

    # The source is an opaque grayscale density map. Remove its JPEG black floor,
    # then preserve the remaining brightness as straight alpha over white RGB.
    alpha = np.clip((luminance - CLOUD_BLACK_POINT)
                    * (255.0 / (255.0 - CLOUD_BLACK_POINT)), 0.0, 255.0)
    rgba = np.full((HEIGHT, WIDTH, 4), 255, dtype=np.uint8)
    rgba[..., 3] = np.rint(alpha).astype(np.uint8)
    cloud_path = TEXTURE_DIRECTORY / "lush_clouds.png"
    Image.fromarray(rgba, mode="RGBA").save(cloud_path, format="PNG", optimize=True)

    visible_coverage = float((rgba[..., 3] > 25).mean())
    nonzero_coverage = float((rgba[..., 3] > 0).mean())
    seam_delta = float(np.abs(rgba[:, 0, 3].astype(np.int16)
                              - rgba[:, -1, 3].astype(np.int16)).mean())
    print(f"Wrote {surface_path} ({WIDTH}x{HEIGHT})")
    print(f"Wrote {cloud_path} ({WIDTH}x{HEIGHT}), "
          f"visible coverage={visible_coverage:.3f}, "
          f"nonzero coverage={nonzero_coverage:.3f}, "
          f"mean longitude seam delta={seam_delta:.2f}")


if __name__ == "__main__":
    main()
