"""Frames the 5 raw phone screenshots (play/raw/<locale>/*.png) into 1080x1920 Play Store
listing images: flat brand background, a bold caption, and the screenshot presented as a
floating squircle card (status bar + nav bar cropped) — the treatment approved interactively
for the Italian set, applied identically to all 8 locales in CAPTIONS.
"""

from PIL import Image, ImageDraw, ImageFont, ImageFilter
import numpy as np
import os

REPO_ROOT = "/Users/anacardix/AndroidStudioProjects/Jottiq"

CANVAS_W, CANVAS_H = 1080, 1920
BG = (158, 74, 46)            # Primary #9E4A2E, flat
TEXT_COLOR = (255, 243, 236)  # OnPrimaryContainer #FFF3EC
FONT_PATH = "/System/Library/Fonts/Supplemental/Arial Rounded Bold.ttf"
FONT_SIZE = 72
CAPTION_MAX_WIDTH = 900  # px — wraps by measured pixel width, not character count
CAPTION_MARGIN_TOP = 120
CAPTION_GAP_TO_SHOT = 100
SUPERSAMPLE = 4  # for anti-aliased mask/shadow edges
SHOT_SIDE_MARGIN = 96
SHOT_TOP_PADDING = 22  # breathing room above the app's own top icon row
CORNER_RADIUS = 150
SHADOW_PAD = 80
SHADOW_OFFSET_Y = 28
SHADOW_BLUR = 24
SHADOW_RGBA = (30, 12, 4, 130)

# Status bar / nav bar heights read from the device at capture time — see
# play/raw/it/status-bar-height-px.txt / navigation-bar-height-px.txt (same GMD run that
# produced these raw screenshots; re-read if raw screenshots are ever recaptured).
STATUS_BAR_HEIGHT = 132
NAV_BAR_HEIGHT = 126

CAPTIONS = {
    "it": {
        "folders": "Le tue cartelle, organizzate come vuoi tu",
        "editor": "Scrivi note ricche di formattazione",
        "lock": "Le note private restano private",
        "trash": "Hai cancellato per sbaglio? Recuperala in 30 giorni",
        "theme": "Un'app che si adatta al tuo stile",
    },
    "en": {
        "folders": "Your folders, organized your way",
        "editor": "Write notes rich with formatting",
        "lock": "Private notes stay private",
        "trash": "Deleted by mistake? Get it back within 30 days",
        "theme": "An app that adapts to your style",
    },
    "fr": {
        "folders": "Vos dossiers, organisés à votre façon",
        "editor": "Des notes riches en mise en forme",
        "lock": "Vos notes privées restent privées",
        "trash": "Supprimé par erreur ? Récupérez-la sous 30 jours",
        "theme": "Une appli qui s'adapte à votre style",
    },
    "de": {
        "folders": "Deine Ordner, organisiert wie du willst",
        "editor": "Notizen mit reicher Formatierung",
        "lock": "Private Notizen bleiben privat",
        "trash": "Aus Versehen gelöscht? 30 Tage Zeit zum Wiederherstellen",
        "theme": "Eine App, die sich deinem Stil anpasst",
    },
    "es-ES": {
        "folders": "Tus carpetas, organizadas a tu manera",
        "editor": "Notas con formato enriquecido",
        "lock": "Las notas privadas siguen siendo privadas",
        "trash": "¿La borraste sin querer? Recupérala en 30 días",
        "theme": "Una app que se adapta a tu estilo",
    },
    "es-419": {
        "folders": "Tus carpetas, organizadas a tu manera",
        "editor": "Notas con formato enriquecido",
        "lock": "Las notas privadas siguen siendo privadas",
        "trash": "¿La borraste por error? Recupérala en 30 días",
        "theme": "Una app que se adapta a tu estilo",
    },
    "pt-PT": {
        "folders": "As tuas pastas, organizadas como quiseres",
        "editor": "Notas com formatação rica",
        "lock": "As notas privadas mantêm-se privadas",
        "trash": "Apagaste por engano? Recupera-a em 30 dias",
        "theme": "Uma app que se adapta ao teu estilo",
    },
    "pt-BR": {
        "folders": "Suas pastas, organizadas do seu jeito",
        "editor": "Notas com formatação rica",
        "lock": "Notas privadas continuam privadas",
        "trash": "Excluiu por engano? Recupere em até 30 dias",
        "theme": "Um app que se adapta ao seu estilo",
    },
}


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
    """A 'squircle' rounded-rect mask (continuous-curvature corners) built at `supersample`x
    resolution and downscaled for anti-aliased edges — PIL's rounded_rectangle alone is not
    anti-aliased and reads as jagged once zoomed."""
    W, H, R = w * supersample, h * supersample, max(1, radius * supersample)
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float64)
    ex = np.clip(np.maximum(R - xx, xx - (W - R)), 0, None)
    ey = np.clip(np.maximum(R - yy, yy - (H - R)), 0, None)
    inside = (ex / R) ** n + (ey / R) ** n <= 1.0
    arr = np.where(inside, 255, 0).astype(np.uint8)
    return Image.fromarray(arr).resize((w, h), Image.LANCZOS)


def frame_screenshot(raw_path, caption, out_path, font):
    canvas = Image.new("RGB", (CANVAS_W, CANVAS_H), BG)
    draw = ImageDraw.Draw(canvas)

    lines = wrap_by_pixel_width(caption, font, draw, CAPTION_MAX_WIDTH)
    y = CAPTION_MARGIN_TOP
    line_bottom = y
    line_gap = round(FONT_SIZE * 0.28)
    for line in lines:
        bbox = draw.textbbox((0, 0), line, font=font)
        lw, lh = bbox[2] - bbox[0], bbox[3] - bbox[1]
        x = (CANVAS_W - lw) / 2
        draw.text((x, y), line, font=font, fill=TEXT_COLOR)
        y += lh + line_gap
        line_bottom = y
    caption_bottom = line_bottom + CAPTION_GAP_TO_SHOT

    shot_full = Image.open(raw_path).convert("RGB")
    shot_cropped = shot_full.crop(
        (0, STATUS_BAR_HEIGHT, shot_full.width, shot_full.height - NAV_BAR_HEIGHT),
    )
    bg_sample = shot_cropped.getpixel((shot_cropped.width // 2, 4))
    shot = Image.new("RGB", (shot_cropped.width, shot_cropped.height + SHOT_TOP_PADDING), bg_sample)
    shot.paste(shot_cropped, (0, SHOT_TOP_PADDING))

    new_w = CANVAS_W - 2 * SHOT_SIDE_MARGIN
    scale = new_w / shot.width
    new_h = round(shot.height * scale)
    shot_resized = shot.resize((new_w, new_h), Image.LANCZOS)

    mask = squircle_mask(new_w, new_h, CORNER_RADIUS)
    shot_x = SHOT_SIDE_MARGIN
    shot_y = caption_bottom

    shadow = Image.new("RGBA", (new_w + SHADOW_PAD * 2, new_h + SHADOW_PAD * 2), (0, 0, 0, 0))
    shadow_layer = Image.new("RGBA", (new_w, new_h), SHADOW_RGBA)
    shadow.paste(shadow_layer, (SHADOW_PAD, SHADOW_PAD), mask)
    shadow = shadow.filter(ImageFilter.GaussianBlur(SHADOW_BLUR))
    canvas.paste(shadow, (shot_x - SHADOW_PAD, shot_y - SHADOW_PAD + SHADOW_OFFSET_Y), shadow)

    canvas.paste(shot_resized, (shot_x, shot_y), mask)
    canvas.save(out_path)
    return lines


def main():
    font = ImageFont.truetype(FONT_PATH, FONT_SIZE)
    for locale, captions in CAPTIONS.items():
        raw_dir = os.path.join(REPO_ROOT, "play", "raw", locale)
        out_dir = os.path.join(REPO_ROOT, "play", "framed", locale)
        os.makedirs(out_dir, exist_ok=True)
        for slug, caption in captions.items():
            raw_path = os.path.join(raw_dir, f"{slug}.png")
            out_path = os.path.join(out_dir, f"{slug}.png")
            lines = frame_screenshot(raw_path, caption, out_path, font)
            print(f"[{locale}] {slug}: {lines} -> {out_path}")


if __name__ == "__main__":
    main()
