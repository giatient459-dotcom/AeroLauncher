#!/usr/bin/env python3
"""Reproducible AeroLauncher identity artwork.

Replaces the inherited Turtle Launcher artwork that was still visible in the
About-page title, the splash logo and the home-screen icon. Everything is drawn
on a coarse logical grid, so the output keeps the pixel-art style the launcher
already uses and is byte-for-byte reproducible.

Generated files
  res/drawable/app_name_title.webp            About-page wordmark (pixel text)
  res/raw/app_name_title.webp                 its one-shot light-sweep animation
  res/drawable/ic_turtle_full.webp            splash / logo picker / artwork logo
  res/raw/ic_turtle_full_animated.webp        its one-shot light-sweep animation
  res/mipmap-<density>/ic_launcher.png        legacy square launcher icon
  res/mipmap-<density>/ic_launcher_round.png  legacy round launcher icon
  res/mipmap-<density>/ic_launcher_foreground.webp  adaptive-icon foreground

The animation contract (<= 24 frames, finite, plays once) is asserted by
tests/check_launcher_art.py.

The adaptive-icon background is res/drawable/ic_launcher_background.xml, a plain
gradient, and is edited by hand.

Run from the repository root:
    python3 tools/generate_aero_identity.py
"""
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1] / "TurtleLauncher/src/main/res"

# Palette: the launcher's own accent family (colors.xml accent_primary /
# pastel_green_200/400/glass), so the artwork matches the themed UI.
ACCENT = (0xC7, 0xE8, 0xB0, 255)          # #C7E8B0 accent_primary
ACCENT_BRIGHT = (0xE3, 0xF2, 0xD9, 255)   # #E3F2D9 pastel_green_200
MOON = (0xF2, 0xFA, 0xF4, 255)
STAR = (0xCF, 0xEC, 0xDD, 255)
SKY_TOP = (0x12, 0x30, 0x3A)              # deep navy-teal
SKY_BOTTOM = (0x06, 0x14, 0x1A)

DENSITIES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}

# Everything below is laid out on this logical grid; output is that grid scaled
# up with nearest-neighbour so the pixel-art edges stay hard.
GRID = 48
GLYPH_SCALE = 2
GLYPH_SIZE = 20      # px size the font is rasterised at before thresholding
GLYPH_ROW_SPLIT = 0.5


def font_glyph(char, size=GLYPH_SIZE):
    """Threshold one character of the bundled font into a 1-bit bitmap."""
    font = ImageFont.truetype(str(ROOT / "font/proxima_nova_extrabold.ttf"), size)
    canvas = Image.new("L", (size * 6, size * 3), 0)
    ImageDraw.Draw(canvas).text((0, 0), char, font=font, fill=255)
    bbox = canvas.getbbox()
    assert bbox, f"glyph {char!r} rendered empty"
    mask = canvas.crop((0, bbox[1], bbox[2], bbox[3]))
    return [[mask.getpixel((x, y)) >= 128 for x in range(mask.width)] for y in range(mask.height)]


def new_grid(background=None):
    return [[background for _ in range(GRID)] for _ in range(GRID)]


def sky_grid():
    grid = new_grid()
    for y in range(GRID):
        t = y / (GRID - 1)
        color = tuple(int(SKY_TOP[i] + (SKY_BOTTOM[i] - SKY_TOP[i]) * t) for i in range(3)) + (255,)
        for x in range(GRID):
            grid[y][x] = color
    return grid


def put(grid, x, y, color):
    if 0 <= x < GRID and 0 <= y < GRID:
        grid[y][x] = color


def draw_moon(grid):
    cx, cy, r = 0.145, 0.155, 0.088
    ox, oy, orad = 0.175, 0.135, 0.070
    for y in range(GRID):
        for x in range(GRID):
            px, py = (x + 0.5) / GRID, (y + 0.5) / GRID
            inside = (px - cx) ** 2 + (py - cy) ** 2 <= r * r
            cut = (px - ox) ** 2 + (py - oy) ** 2 <= orad * orad
            if inside and not cut:
                put(grid, x, y, MOON)


def draw_stars(grid, positions=((0.86, 0.12), (0.92, 0.34), (0.78, 0.90), (0.14, 0.90))):
    for sx, sy in positions:
        gx, gy = int(sx * GRID), int(sy * GRID)
        for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
            put(grid, gx + dx, gy + dy, STAR)


def draw_a(grid, two_tone=False):
    glyph = font_glyph("A")
    height, width = len(glyph), len(glyph[0])
    origin_x = (GRID - width * GLYPH_SCALE) // 2
    origin_y = (GRID - height * GLYPH_SCALE) // 2 + 1
    for y in range(height):
        color = ACCENT_BRIGHT if (two_tone and y < height * GLYPH_ROW_SPLIT) else ACCENT
        for x in range(width):
            if not glyph[y][x]:
                continue
            for dy in range(GLYPH_SCALE):
                for dx in range(GLYPH_SCALE):
                    put(grid, origin_x + x * GLYPH_SCALE + dx, origin_y + y * GLYPH_SCALE + dy, color)


def render(grid, size):
    image = Image.new("RGBA", (GRID, GRID), (0, 0, 0, 0))
    for y in range(GRID):
        for x in range(GRID):
            color = grid[y][x]
            if color is not None:
                image.putpixel((x, y), color)
    if size != GRID:
        image = image.resize((size, size), Image.Resampling.NEAREST)
    return image


def emblem(size):
    """Full icon composition: night sky, crescent, stars, pixel 'A'."""
    grid = sky_grid()
    draw_moon(grid)
    draw_stars(grid)
    draw_a(grid)
    return render(grid, size)


def emblem_foreground(size, coverage=0.72):
    """Adaptive-icon foreground: art only, inside the launcher's safe zone."""
    grid = new_grid()
    draw_moon(grid)
    draw_stars(grid)
    draw_a(grid)
    art = render(grid, size)
    inner = int(size * coverage)
    foreground = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    foreground.alpha_composite(art.resize((inner, inner), Image.Resampling.NEAREST),
                               ((size - inner) // 2, (size - inner) // 2))
    return foreground


def wordmark(scale=6, word_gap=2):
    """'AeroLauncher' as a two-tone pixel wordmark.

    The camel-case boundary between "Aero" and "Launcher" is widened by
    [word_gap] cells so the compound reads as two words at launcher sizes, and
    the same boundary is where the bright accent hands over to the accent colour.
    """
    bits = font_glyph("AeroLauncher", size=18)
    height, width = len(bits), len(bits[0])

    empty = [x for x in range(width) if not any(row[x] for row in bits)]
    runs, start = [], None
    for x in range(width):
        if x in empty and start is None:
            start = x
        elif x not in empty and start is not None:
            runs.append((start, x - 1))
            start = None
    # The word space is the widest empty run; single empty columns are just
    # letter spacing inside a glyph pair.
    split = max(runs, key=lambda run: run[1] - run[0])[0] if runs else width

    padded = []
    for y in range(height):
        row = bits[y][:split] + [False] * word_gap + bits[y][split:]
        padded.append(row)
    width = len(padded[0])
    split += word_gap // 2

    art = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    for y in range(height):
        for x in range(width):
            if padded[y][x]:
                art.putpixel((x, y), ACCENT_BRIGHT if x < split else ACCENT)
    return art.resize((width * scale, height * scale), Image.Resampling.NEAREST)


def animate(source, destination, frames=20, duration=85):
    """One-shot light sweep, matching the contract in tools/generate_launcher_art.py."""
    width, height = source.size
    out = []
    for index in range(frames):
        sweep = Image.new("RGBA", source.size)
        draw = ImageDraw.Draw(sweep)
        x = int(-width * 0.4 + index / (frames - 1) * width * 1.8)
        band = max(8, width // 9)
        draw.polygon([(x, 0), (x + band, 0), (x - height // 3 + band, height), (x - height // 3, height)],
                     fill=(198, 255, 216, 65))
        sweep.putalpha(ImageChops.multiply(sweep.getchannel("A"), source.getchannel("A")))
        out.append(Image.alpha_composite(source.copy(), sweep))
    destination.parent.mkdir(parents=True, exist_ok=True)
    out[0].save(destination, save_all=True, append_images=out[1:], duration=duration,
                loop=1, lossless=True, method=6)


def main():
    drawable = ROOT / "drawable"
    raw = ROOT / "raw"
    drawable.mkdir(parents=True, exist_ok=True)

    title = wordmark()
    title.save(drawable / "app_name_title.webp", lossless=True, method=6)
    animate(title, raw / "app_name_title.webp")

    # res/drawable/ic_turtle_full.webp keeps its original resource name so the
    # existing R.drawable/R.raw wiring (AnimatedLogo, AnimatedIcon, logo picker,
    # splash, About artwork row) needs no changes; only the artwork is replaced.
    logo = emblem(512)
    logo.save(drawable / "ic_turtle_full.webp", lossless=True, method=6)
    # The animated frames are capped at 192 px by tests/check_launcher_art.py.
    animate(emblem(192), raw / "ic_turtle_full_animated.webp")

    for density, size in DENSITIES.items():
        folder = ROOT / f"mipmap-{density}"
        folder.mkdir(parents=True, exist_ok=True)
        emblem(size).save(folder / "ic_launcher.png", optimize=True)
        emblem(size).save(folder / "ic_launcher_round.png", optimize=True)
        emblem_foreground(size).save(folder / "ic_launcher_foreground.webp", quality=92, method=6)

    print("Generated AeroLauncher wordmark, emblem, launcher icons and one-shot animations.")


if __name__ == "__main__":
    main()
