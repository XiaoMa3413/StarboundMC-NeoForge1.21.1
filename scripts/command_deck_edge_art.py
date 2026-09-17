"""Dimension-specific original edge artwork, drawn directly on each face's canvas.

No source image crops: every panel has its own complete border, seam and fittings.
The atlas only packs finished rectangular panels with padding for mipmaps.
"""
import math
from PIL import Image, ImageDraw


class EdgeAtlas:
    SIZE = 1024
    CELL = 128
    DENSITY = 8

    def __init__(self):
        self.image = Image.new('RGB', (self.SIZE, self.SIZE), '#1c2936')
        self.regions = {}

    def region(self, kind, width, height):
        key = kind, round(width, 5), round(height, 5)
        if key not in self.regions:
            index = len(self.regions)
            assert index < 64, 'Edge atlas capacity exceeded'
            x = (index % 8) * self.CELL + 4
            y = (index // 8) * self.CELL + 4
            w, h = width * self.DENSITY, height * self.DENSITY
            assert max(w, h) <= self.CELL - 8
            tile = self.paint(kind, math.ceil(w), math.ceil(h))
            self.image.paste(tile, (x, y))
            # Extrude the completed panel's edge into a four-pixel sampling gutter.
            self.image.paste(tile.crop((0, 0, 1, tile.height)).resize((4, tile.height)), (x - 4, y))
            self.image.paste(tile.crop((tile.width - 1, 0, tile.width, tile.height)).resize((4, tile.height)), (x + tile.width, y))
            self.image.paste(tile.crop((0, 0, tile.width, 1)).resize((tile.width, 4)), (x, y - 4))
            self.image.paste(tile.crop((0, tile.height - 1, tile.width, tile.height)).resize((tile.width, 4)), (x, y + tile.height))
            self.regions[key] = [x / 64, y / 64, (x + w) / 64, (y + h) / 64]
        return self.regions[key]

    @staticmethod
    def paint(kind, width, height):
        vertical = height > width
        length, thickness = max(width, height), min(width, height)
        panel = Image.new('RGB', (length, thickness), '#293c4d' if kind == 'frame' else '#223240')
        d = ImageDraw.Draw(panel)
        # Even the thinnest face gets a complete, quiet material treatment.
        if thickness < 5:
            d.line((0, 0, length - 1, 0), fill='#5b7182')
        else:
            d.rectangle((0, 0, length - 1, thickness - 1), outline='#101c27')
            d.line((1, 1, length - 2, 1), fill='#718595')
            d.line((1, 2, 1, thickness - 2), fill='#425c6e')
            d.line((2, thickness - 2, length - 2, thickness - 2), fill='#152330')
            if kind == 'controls':
                if thickness >= 12 and length >= 24:
                    for x in range(6, length - 7, 12):
                        d.rectangle((x, 4, x + 6, min(thickness - 5, 10)), fill='#8295a0')
                        d.line((x, 4, x + 6, 4), fill='#c1cdd0')
                    d.rectangle((length - 7, 4, length - 4, thickness - 5), fill='#dda85d')
                else:
                    d.rectangle((2, 2, length - 3, thickness - 3), fill='#102630')
                    inset = 4 if thickness >= 12 else 2
                    d.rectangle((inset, max(2, thickness // 2 - 1),
                                 length - inset - 1, min(thickness - 3, thickness // 2 + 1)), fill='#72d9d9')
            elif kind == 'casing' and thickness >= 20 and length >= 36:
                # Complete recessed vent bank, with margins on every side.
                d.rectangle((7, 5, length - 8, thickness - 6), fill='#111f2a')
                for x in range(10, length - 10, 8):
                    d.rectangle((x, 7, x + 2, thickness - 8), fill='#415b6b')
                    d.line((x, 7, x, thickness - 8), fill='#6a7e8b')
            elif length >= 20:
                mid = thickness // 2
                d.line((5, mid, length - 6, mid), fill='#111f2a', width=3 if thickness >= 9 else 1)
                if kind == 'frame' and thickness >= 8:
                    d.line((length // 3, mid, 2 * length // 3, mid), fill='#59b9c5')
            if thickness >= 12 and length >= 24:
                for x in (3, length - 5):
                    for y in (3, thickness - 5):
                        d.rectangle((x, y, x + 1, y + 1), fill='#9cabb3')
        return panel.transpose(Image.Transpose.ROTATE_90) if vertical else panel
