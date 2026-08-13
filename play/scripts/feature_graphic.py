"""Composes the 1024x500 Play Store feature graphic: same flat brand background and
wordmark treatment as the phone screenshots in frame.py, plus the app icon. No tagline
text — kept locale-agnostic so one graphic serves all 8 store listings.

Output: 24-bit PNG, no alpha channel (Play Console spec forbids alpha on this asset,
unlike the hi-res icon which requires it).
"""

from PIL import Image, ImageDraw, ImageFont

REPO_ROOT = "/Users/anacardix/AndroidStudioProjects/Jottiq"

CANVAS_W, CANVAS_H = 1024, 500
BG = (158, 74, 46)            # Primary #9E4A2E, flat — matches frame.py
TEXT_COLOR = (255, 243, 236)  # OnPrimaryContainer #FFF3EC — matches frame.py
FONT_PATH = "/System/Library/Fonts/Supplemental/Arial Rounded Bold.ttf"
FONT_SIZE = 132

ICON_PATH = f"{REPO_ROOT}/icons/jottiq-512.png"
ICON_SIZE = 300  # rendered square size on canvas
ICON_LEFT_MARGIN = 130

OUT_PATH = f"{REPO_ROOT}/play/feature-graphic.png"


def main():
    canvas = Image.new("RGB", (CANVAS_W, CANVAS_H), BG)

    icon = Image.open(ICON_PATH).convert("RGBA").resize(
        (ICON_SIZE, ICON_SIZE), Image.LANCZOS
    )
    icon_y = (CANVAS_H - ICON_SIZE) // 2
    canvas.paste(icon, (ICON_LEFT_MARGIN, icon_y), icon)  # icon's own alpha as mask

    draw = ImageDraw.Draw(canvas)
    font = ImageFont.truetype(FONT_PATH, FONT_SIZE)
    text = "Jottiq"
    text_bbox = draw.textbbox((0, 0), text, font=font)
    text_w = text_bbox[2] - text_bbox[0]
    text_h = text_bbox[3] - text_bbox[1]
    text_x = ICON_LEFT_MARGIN + ICON_SIZE + 70
    text_y = (CANVAS_H - text_h) // 2 - text_bbox[1]
    draw.text((text_x, text_y), text, font=font, fill=TEXT_COLOR)

    canvas.save(OUT_PATH, "PNG")
    print(f"Wrote {OUT_PATH} ({canvas.size[0]}x{canvas.size[1]}, mode={canvas.mode})")


if __name__ == "__main__":
    main()
