#!/usr/bin/env python3
"""Generate the deterministic, tileable equirectangular Lush cloud shell map."""

from pathlib import Path

import numpy as np
from PIL import Image


WIDTH = 2048
HEIGHT = 1024
SEED = 20260928
OUTPUT = (Path(__file__).resolve().parents[1]
          / "src/main/resources/assets/starboundmc/textures/planet/lush_clouds.png")


def smoothstep(edge0: float, edge1: float, value: np.ndarray) -> np.ndarray:
    t = np.clip((value - edge0) / (edge1 - edge0), 0.0, 1.0)
    return t * t * (3.0 - 2.0 * t)


def periodic_cloud_noise(
        width: int, height: int, sigma_x: float, sigma_y: float,
        rng: np.random.Generator) -> np.ndarray:
    """Gaussian-smoothed noise with a periodic longitude axis."""
    noise = rng.standard_normal((height, width), dtype=np.float32)
    frequencies_y = np.fft.fftfreq(height).astype(np.float32)[:, None]
    frequencies_x = np.fft.rfftfreq(width).astype(np.float32)[None, :]
    filter_kernel = np.exp(-2.0 * np.pi * np.pi * (
        (sigma_x * frequencies_x) ** 2 + (sigma_y * frequencies_y) ** 2))
    filtered = np.fft.irfft2(np.fft.rfft2(noise) * filter_kernel, s=(height, width))
    filtered = filtered.astype(np.float32)
    filtered -= filtered.mean()
    filtered /= max(float(filtered.std()), 1.0e-6)
    return filtered


def sample_periodic(field: np.ndarray, x: np.ndarray, y: np.ndarray) -> np.ndarray:
    """Bilinear sample, wrapping longitude and clamping at the poles."""
    height, width = field.shape
    x = np.mod(x, width)
    y = np.clip(y, 0.0, height - 1.0)
    x0 = np.floor(x).astype(np.int32)
    y0 = np.floor(y).astype(np.int32)
    x1 = (x0 + 1) % width
    y1 = np.minimum(y0 + 1, height - 1)
    tx = x - x0
    ty = y - y0
    top = field[y0, x0] * (1.0 - tx) + field[y0, x1] * tx
    bottom = field[y1, x0] * (1.0 - tx) + field[y1, x1] * tx
    return top * (1.0 - ty) + bottom * ty


def main() -> None:
    rng = np.random.default_rng(SEED)
    broad = periodic_cloud_noise(WIDTH, HEIGHT, 190.0, 78.0, rng)
    warp_x = periodic_cloud_noise(WIDTH, HEIGHT, 330.0, 125.0, rng)
    warp_y = periodic_cloud_noise(WIDTH, HEIGHT, 285.0, 105.0, rng)
    medium = periodic_cloud_noise(WIDTH, HEIGHT, 42.0, 17.0, rng)
    fine = periodic_cloud_noise(WIDTH, HEIGHT, 11.0, 4.5, rng)

    x, y = np.meshgrid(np.arange(WIDTH, dtype=np.float32),
                       np.arange(HEIGHT, dtype=np.float32))
    medium = sample_periodic(medium, x + warp_x * 115.0, y + warp_y * 30.0)
    fine = sample_periodic(fine, x + warp_x * 48.0, y + warp_y * 13.0)

    field = 0.50 * broad + 0.34 * medium + 0.16 * fine
    latitude = (np.arange(HEIGHT, dtype=np.float32) + 0.5) / HEIGHT * np.pi - np.pi / 2.0
    cos_latitude = np.cos(latitude)
    polar_detail = np.sqrt(smoothstep(0.0, 0.12, cos_latitude))[:, None]
    row_mean = field.mean(axis=1, keepdims=True)
    field = row_mean + (field - row_mean) * polar_detail
    field[0, :] = row_mean[0, 0]
    field[-1, :] = row_mean[-1, 0]

    threshold = float(np.quantile(field, 0.64))
    density = smoothstep(threshold - 0.09, threshold + 0.17, field)
    alpha = np.clip(density * 236.0, 0.0, 236.0).astype(np.uint8)

    shade = np.clip(0.94 + 0.025 * medium + 0.010 * fine, 0.82, 1.0)
    red = np.clip(shade * 255.0, 0, 255).astype(np.uint8)
    green = np.clip(shade * 0.99 * 255.0, 0, 255).astype(np.uint8)
    blue = np.clip(shade * 0.97 * 255.0, 0, 255).astype(np.uint8)
    rgba = np.stack((red, green, blue, alpha), axis=-1)

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    Image.fromarray(rgba, mode="RGBA").save(OUTPUT, format="PNG", optimize=False)

    covered = alpha > 25
    print(f"Wrote {OUTPUT}")
    print(f"size={WIDTH}x{HEIGHT}, alpha coverage={covered.mean():.3f}, "
          f"nonzero alpha={(alpha > 0).mean():.3f}, max alpha={int(alpha.max())}")
    print(f"mean longitude seam delta={np.abs(alpha[:, 0].astype(np.int16) - alpha[:, -1]).mean():.2f}, "
          f"north pole alpha range={int(alpha[0].min())}..{int(alpha[0].max())}, "
          f"south pole alpha range={int(alpha[-1].min())}..{int(alpha[-1].max())}")


if __name__ == "__main__":
    main()
