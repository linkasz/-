import argparse
from collections import deque
import math
from pathlib import Path
from PIL import Image, ImageChops, ImageDraw


ROOT = Path(__file__).resolve().parent / "app" / "src" / "main" / "res"
SOURCE = ROOT.parent / "icon-artwork" / "schedule_plus_1024.png"
ICON_NAMES = (
    "ic_launcher",
    "ic_launcher_light",
    "ic_launcher_dark",
    "ic_launcher_kanban",
    "ic_launcher_kanban_light",
    "ic_launcher_kanban_dark",
)
ICON_SIZES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}
# The widget renders this at 23-24 dp; 128 px still covers xxxhdpi without shipping
# two half-megabyte source bitmaps in every APK.
WIDGET_PREVIEW_SIZE = 128
WIDGET_CORNER_EXPONENT = 4.5
WIDGET_MASK_SUPERSAMPLING = 4


def remove_connected_black_corners(image: Image.Image) -> Image.Image:
    image = image.convert("RGBA")
    width, height = image.size
    pixels = image.load()
    queue = deque()
    seen = set()

    def is_outer_black(pixel: tuple[int, int, int, int]) -> bool:
        red, green, blue, alpha = pixel
        return alpha > 0 and red < 26 and green < 26 and blue < 26

    for x in range(width):
        queue.append((x, 0))
        queue.append((x, height - 1))
    for y in range(height):
        queue.append((0, y))
        queue.append((width - 1, y))

    while queue:
        x, y = queue.popleft()
        if (x, y) in seen or not (0 <= x < width and 0 <= y < height):
            continue
        if not is_outer_black(pixels[x, y]):
            continue
        seen.add((x, y))
        pixels[x, y] = (0, 0, 0, 0)
        queue.extend(((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)))

    return image


def prepare_icon(source: Path) -> Image.Image:
    image = remove_connected_black_corners(Image.open(source))
    content_bounds = image.getchannel("A").getbbox()
    if content_bounds:
        image = image.crop(content_bounds)
    side = max(image.size)
    square = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    square.alpha_composite(image, ((side - image.width) // 2, (side - image.height) // 2))
    return square


def continuous_corner_mask(size: int) -> Image.Image:
    """Return an antialiased superellipse mask with continuous corner curvature."""
    scale = WIDGET_MASK_SUPERSAMPLING
    canvas_size = size * scale
    center = (canvas_size - 1) / 2
    radius = center
    power = 2 / WIDGET_CORNER_EXPONENT
    points = []
    for step in range(1440):
        angle = math.tau * step / 1440
        cosine = math.cos(angle)
        sine = math.sin(angle)
        x = center + radius * math.copysign(abs(cosine) ** power, cosine)
        y = center + radius * math.copysign(abs(sine) ** power, sine)
        points.append((x, y))
    mask = Image.new("L", (canvas_size, canvas_size), 0)
    ImageDraw.Draw(mask).polygon(points, fill=255)
    return mask.resize((size, size), Image.Resampling.LANCZOS)


def write_variant(source: Path, night: bool, launcher_icons: bool) -> None:
    icon = prepare_icon(source)
    qualifier = "night-" if night else ""
    if launcher_icons:
        for density, size in ICON_SIZES.items():
            output_dir = ROOT / f"mipmap-{qualifier}{density}"
            output_dir.mkdir(parents=True, exist_ok=True)
            resized = icon.resize((size, size), Image.Resampling.LANCZOS)
            for name in ICON_NAMES:
                resized.save(output_dir / f"{name}.png", optimize=True)

        foreground = icon.resize((432, 432), Image.Resampling.LANCZOS)
        foreground.save(ROOT / "drawable-nodpi" / "ic_launcher_foreground.png", optimize=True)
        adaptive_dir = ROOT / "mipmap-anydpi-v26"
        adaptive_dir.mkdir(parents=True, exist_ok=True)
        adaptive_xml = '''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
'''
        for name in ICON_NAMES:
            (adaptive_dir / f"{name}.xml").write_text(adaptive_xml, encoding="utf-8")
        (ROOT / "values" / "ic_launcher_background.xml").write_text(
            '''<?xml version="1.0" encoding="utf-8"?>
<resources><color name="ic_launcher_background">#102D8F</color></resources>
''',
            encoding="utf-8",
        )

    preview_dir = ROOT / ("drawable-night" if night else "drawable")
    preview_dir.mkdir(parents=True, exist_ok=True)
    preview = icon.resize((WIDGET_PREVIEW_SIZE, WIDGET_PREVIEW_SIZE), Image.Resampling.LANCZOS)
    preview.putalpha(ImageChops.multiply(preview.getchannel("A"), continuous_corner_mask(WIDGET_PREVIEW_SIZE)))
    preview.save(
        preview_dir / "ic_launcher_preview.png",
        optimize=True,
    )


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--widget-previews-only",
        action="store_true",
        help="Regenerate only the widget header icons, leaving launcher resources untouched.",
    )
    args = parser.parse_args()
    launcher_icons = not args.widget_previews_only
    write_variant(SOURCE, night=False, launcher_icons=launcher_icons)
    if args.widget_previews_only:
        write_variant(SOURCE, night=True, launcher_icons=False)
        print("Generated continuous-corner widget icons.")
    else:
        write_variant(SOURCE, night=True, launcher_icons=False)
        print("Generated density-specific and adaptive launcher resources.")


if __name__ == "__main__":
    main()
