"""Normalize authored imagegen assets for Minecraft; no procedural artwork generation.

Usage: python scripts/import-command-deck-art.py ATLAS_PNG NOVA_BODY_PNG
The portrait's existing eye registration is retained at its original 96x112 size.
"""
import sys
import json
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
TEXTURES = ROOT / 'src/main/resources/assets/starboundmc/textures'

atlas = Image.open(sys.argv[1]).convert('RGB')
# Preserve the detailed material art; face proportions are handled by model UVs.
atlas.resize((256, 256), Image.Resampling.NEAREST).save(TEXTURES / 'block/command_deck_atlas.png')
body = Image.open(sys.argv[2]).convert('RGBA').resize((96, 112), Image.Resampling.NEAREST)
body.save(TEXTURES / 'gui/ship_ai/nova_body.png')
eyes = Image.open(TEXTURES / 'gui/ship_ai/nova_eyes.png').convert('RGBA')
assert eyes.size == body.size, 'Portrait layers must share the same registration'
Image.alpha_composite(body, eyes).save(TEXTURES / 'gui/ship_ai/nova_bust.png')
# Bake the existing eye-layer animation into the in-world screen sprite.
strip = Image.new('RGBA', (48, 96), body.getpixel((0, 0)))
for index, opacity in enumerate((1.0, 0.55, 0.08)):
    layer = eyes.copy()
    layer.putalpha(layer.getchannel('A').point(lambda alpha: round(alpha * opacity)))
    portrait = Image.alpha_composite(body, layer).resize((27, 32), Image.Resampling.NEAREST)
    strip.paste(portrait, (10, index * 32))
strip.save(TEXTURES / 'block/ship_ai_terminal_screen.png')
(TEXTURES / 'block/ship_ai_terminal_screen.png.mcmeta').write_text(json.dumps({
    'animation': {'width': 48, 'height': 32, 'interpolate': False,
                  'frames': [{'index': 0, 'time': 64}, {'index': 1, 'time': 2},
                             {'index': 2, 'time': 2}, {'index': 1, 'time': 2}]}
}, indent=2) + '\n', encoding='utf-8')
print('Imported command atlas, layered NOVA portrait, fallback and animated terminal screen.')
