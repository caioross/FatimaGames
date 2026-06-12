#!/usr/bin/env python3
# Gera os ativos gráficos da ficha da Play Store a partir de icon.png.
#   - icon-512.png            (ícone de alta resolução 512x512, recortado e limpo)
#   - icon-512-original.png   (resize direto, para comparação)
#   - feature-graphic-1024x500.png  (gráfico de destaque da loja)
# Requisitos: Pillow.  Rode:  python docs/play-store/_generate_assets.py

import os
from PIL import Image, ImageDraw, ImageFont, ImageFilter

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT = os.path.join(ROOT, "docs", "play-store")
SRC = os.path.join(ROOT, "icon.png")
FONTS = r"C:/Windows/Fonts"
os.makedirs(OUT, exist_ok=True)

# Paleta da marca
INK = (44, 42, 38)
INK_SOFT = (107, 102, 94)
MINT_DEEP = (79, 106, 83)
MINT = (122, 155, 126)
CREAM = (250, 246, 240)
WELL = (233, 225, 211)
BORDER = (199, 184, 159)
TILES = [(169, 196, 172), (196, 173, 208), (232, 183, 158), (229, 201, 122)]


def font(name, size):
    try:
        return ImageFont.truetype(os.path.join(FONTS, name), size)
    except Exception:
        return ImageFont.load_default()


def serif_bold(s):  return font("georgiab.ttf", s)
def sans(s):        return font("segoeui.ttf", s)
def sans_semi(s):   return font("seguisb.ttf", s)


def rounded(img, radius):
    """Aplica cantos arredondados (retorna RGBA)."""
    img = img.convert("RGBA")
    mask = Image.new("L", img.size, 0)
    d = ImageDraw.Draw(mask)
    d.rounded_rectangle([0, 0, img.size[0], img.size[1]], radius=radius, fill=255)
    img.putalpha(mask)
    return img


# ─── 1. Recorta o quadrado claro do ícone (descarta o fundo/sombra escuros) ───
src = Image.open(SRC).convert("RGB")
gray = src.convert("L")
# Threshold alto isola só o quadrado pastel (a sombra escura fica de fora).
mask = gray.point(lambda p: 255 if p > 150 else 0)
bbox = mask.getbbox()
crop = src.crop(bbox)
w, h = crop.size
side = min(w, h)
icon_sq = crop.crop(((w - side) // 2, (h - side) // 2,
                     (w - side) // 2 + side, (h - side) // 2 + side))
# Inset de 5% para eliminar qualquer resíduo escuro dos cantos arredondados.
inset = int(side * 0.05)
icon_sq = icon_sq.crop((inset, inset, side - inset, side - inset))

# ─── 2. Ícone 512x512 (full-bleed, limpo) + versão original p/ comparação ───
icon_sq.resize((512, 512), Image.LANCZOS).save(os.path.join(OUT, "icon-512.png"))
src.resize((512, 512), Image.LANCZOS).save(os.path.join(OUT, "icon-512-original.png"))

# ─── 3. Feature graphic 1024x500 ───
W, H = 1024, 500
grad = Image.new("RGB", (1, H))
for y in range(H):
    t = y / (H - 1)
    grad.putpixel((0, y), tuple(int(CREAM[i] + (WELL[i] - CREAM[i]) * t) for i in range(3)))
fg = grad.resize((W, H)).convert("RGB")
draw = ImageDraw.Draw(fg)

# blobs decorativos suaves
blob = Image.new("RGBA", (W, H), (0, 0, 0, 0))
bd = ImageDraw.Draw(blob)
bd.ellipse([-80, -120, 220, 200], fill=(196, 173, 208, 70))
bd.ellipse([W - 240, H - 220, W + 60, H + 120], fill=(169, 196, 172, 70))
blob = blob.filter(ImageFilter.GaussianBlur(60))
fg.paste(blob, (0, 0), blob)

# ícone à direita, com sombra
isz = 320
ic = rounded(icon_sq.resize((isz, isz), Image.LANCZOS), 64)
shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
sh = Image.new("RGBA", (isz, isz), (0, 0, 0, 0))
ImageDraw.Draw(sh).rounded_rectangle([0, 0, isz, isz], radius=64, fill=(44, 42, 38, 110))
ix, iy = W - isz - 80, (H - isz) // 2
shadow.paste(sh, (ix, iy + 16), sh)
shadow = shadow.filter(ImageFilter.GaussianBlur(22))
fg.paste(shadow, (0, 0), shadow)
fg.paste(ic, (ix, iy), ic)

# texto à esquerda
x = 80
# pílula
pill_font = sans_semi(24)
pill_text = "Sem anúncios · Offline · Grátis"
tb = draw.textbbox((0, 0), pill_text, font=pill_font)
pw, ph = tb[2] - tb[0], tb[3] - tb[1]
px0, py0 = x, 70
draw.rounded_rectangle([px0, py0, px0 + pw + 64, py0 + ph + 28], radius=999,
                       outline=BORDER, width=2, fill=(255, 255, 255, 0))
draw.ellipse([px0 + 22, py0 + ph // 2 + 1, px0 + 34, py0 + ph // 2 + 13], fill=MINT)
draw.text((px0 + 44, py0 + 14), pill_text, font=pill_font, fill=INK_SOFT)

# título
draw.text((x, 150), "Fatima Games", font=serif_bold(78), fill=INK)
# tagline
draw.text((x, 252), "Jogos calmos para relaxar", font=sans_semi(38), fill=MINT_DEEP)
# subtítulo
draw.text((x, 312), "8 jogos · 100% offline · Android 8.0+", font=sans(26), fill=INK_SOFT)

# tiles de acento
ty = 380
for i, c in enumerate(TILES):
    bx = x + i * 60
    draw.rounded_rectangle([bx, ty, bx + 46, ty + 46], radius=12, fill=c)

fg.convert("RGB").save(os.path.join(OUT, "feature-graphic-1024x500.png"))

print("OK ->", os.listdir(OUT))
