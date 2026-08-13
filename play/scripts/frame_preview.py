from PIL import Image, ImageDraw, ImageFont, ImageFilter
import numpy as np

CANVAS_W, CANVAS_H = 1080, 1920
BG = (158, 74, 46)            # Primary #9E4A2E, flat (no gradient this time)
TEXT_COLOR = (255, 243, 236)  # OnPrimaryContainer #FFF3EC
FONT_PATH = "/System/Library/Fonts/Supplemental/Arial Rounded Bold.ttf"
STATUS_BAR_HEIGHT = 132  # from status-bar-height-px.txt
NAV_BAR_HEIGHT = 126  # from navigation-bar-height-px.txt (this run's device metric)
FONT_SIZE = 72
CAPTION_MAX_WIDTH = 900  # px — wraps by measured pixel width, not character count
SUPERSAMPLE = 4  # for anti-aliased mask edges

CAPTION = "Le tue cartelle, organizzate come vuoi tu"
SHOT_PATH = "/Users/anacardix/AndroidStudioProjects/Jottiq/play/raw/it/folders.png"
OUT_PATH = "/Users/anacardix/AndroidStudioProjects/Jottiq/play/scripts/preview_folders.png"


def wrap_by_pixel_width(text, font, draw, max_width):
    """Greedy word-wrap measured by actual rendered pixel width, then rebalanced so no
    line is left much shorter than the others (avoids a short orphan last line)."""
    words = text.split()
    lines, current = [], []
    for word in words:
        trial = " ".join(current + [word])
        if draw.textbbox((0, 0), trial, font=font)[2] <= max_width or not current:
            current.append(word)
        else:
            lines.append(" ".join(current))
            current = [word]
    if current:
        lines.append(" ".join(current))
    if len(lines) > 1:
        last_w = draw.textbbox((0, 0), lines[-1], font=font)[2]
        prev_w = draw.textbbox((0, 0), lines[-2], font=font)[2]
        if last_w < prev_w * 0.55:
            prev_words = lines[-2].split()
            if len(prev_words) > 1:
                moved = prev_words.pop()
                lines[-2] = " ".join(prev_words)
                lines[-1] = moved + " " + lines[-1]
    return lines


def squircle_mask(w, h, radius, n=5.0, supersample=SUPERSAMPLE):
    """A 'squircle' rounded-rect mask (continuous-curvature corners, like iOS app icons /
    Anthropic's own product shots) rather than PIL's plain circular-arc rounded_rectangle —
    built at `supersample`x resolution and downscaled for anti-aliased edges throughout."""
    W, H, R = w * supersample, h * supersample, max(1, radius * supersample)
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float64)
    ex = np.clip(np.maximum(R - xx, xx - (W - R)), 0, None)
    ey = np.clip(np.maximum(R - yy, yy - (H - R)), 0, None)
    inside = (ex / R) ** n + (ey / R) ** n <= 1.0
    arr = np.where(inside, 255, 0).astype(np.uint8)
    return Image.fromarray(arr, mode="L").resize((w, h), Image.LANCZOS)


canvas = Image.new("RGB", (CANVAS_W, CANVAS_H), BG)
draw = ImageDraw.Draw(canvas)

font = ImageFont.truetype(FONT_PATH, FONT_SIZE)

margin_top = 120
lines = wrap_by_pixel_width(CAPTION, font, draw, CAPTION_MAX_WIDTH)

y = margin_top
line_bottom = y
line_gap = round(FONT_SIZE * 0.28)
for line in lines:
    bbox = draw.textbbox((0, 0), line, font=font)
    lw = bbox[2] - bbox[0]
    lh = bbox[3] - bbox[1]
    x = (CANVAS_W - lw) / 2
    draw.text((x, y), line, font=font, fill=TEXT_COLOR)
    y += lh + line_gap
    line_bottom = y

caption_bottom = line_bottom + 100

# Crop both the status bar (top) and nav bar (bottom) off the raw screenshot before
# compositing — only the app's own UI remains.
shot_full = Image.open(SHOT_PATH).convert("RGB")
shot_cropped = shot_full.crop((0, STATUS_BAR_HEIGHT, shot_full.width, shot_full.height - NAV_BAR_HEIGHT))

# A little extra breathing room above the top icon row — without it, that row sits right
# against the crop edge (previously the status bar). Padded with the screen's own
# background color (sampled from the shot itself) so it reads as part of the app, not an
# added bar.
TOP_PADDING = 22
bg_sample = shot_cropped.getpixel((shot_cropped.width // 2, 4))
shot = Image.new("RGB", (shot_cropped.width, shot_cropped.height + TOP_PADDING), bg_sample)
shot.paste(shot_cropped, (0, TOP_PADDING))

side_margin = 96
new_w = CANVAS_W - 2 * side_margin
scale = new_w / shot.width
new_h = round(shot.height * scale)
shot_resized = shot.resize((new_w, new_h), Image.LANCZOS)

radius = 150
mask = squircle_mask(new_w, new_h, radius)

shot_x = side_margin
shot_y = caption_bottom

# Soft drop shadow behind the card — same squircle shape, blurred and offset down, so the
# screenshot reads as a floating card rather than a flat cutout pasted on the background.
shadow_pad = 80
shadow_offset_y = 28
shadow = Image.new("RGBA", (new_w + shadow_pad * 2, new_h + shadow_pad * 2), (0, 0, 0, 0))
shadow_layer = Image.new("RGBA", (new_w, new_h), (30, 12, 4, 130))
shadow.paste(shadow_layer, (shadow_pad, shadow_pad), mask)
shadow = shadow.filter(ImageFilter.GaussianBlur(24))
canvas.paste(shadow, (shot_x - shadow_pad, shot_y - shadow_pad + shadow_offset_y), shadow)

canvas.paste(shot_resized, (shot_x, shot_y), mask)

canvas.save(OUT_PATH)
print("saved", OUT_PATH, canvas.size, "shot bottom would be at y =", shot_y + new_h, "(canvas h =", CANVAS_H, ")")
print("caption lines:", lines)
