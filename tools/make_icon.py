# Builds the Launcher3TV icon and TV banner as Android vector drawables from the pixel-art
# reference (assets-src/icon-reference.png, 512 x 512): every 14.2 px cell of the house becomes
# one vector square. The reference's grey disc is left out. Crisp at any size.
#
# Usage: python3 -I tools/make_icon.py <res/drawable dir> [preview.ppm]
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
REF = os.path.join(HERE, "..", "assets-src", "icon-reference.png")
CELL = 14.2          # fitted from the colour edges in the reference
OX, OY = 0.4, 0.0    # grid offset, px
SIZE = 512

# Decode the PNG with the small reader in px.py (no dependencies).
_src = open(os.path.join(HERE, "px.py")).read().split("bpp,R=png")[0]
_ns = {}
exec(_src, _ns)
bpp, R = _ns["png"](REF)


def pixel(x, y):
    p = R[int(y)][int(x) * bpp:int(x) * bpp + bpp]
    return tuple(p[:3]), (p[3] if bpp == 4 else 255)


DISC = pixel(SIZE // 2, 40)[0]      # the reference's grey disc, sampled to tell it from the house


def close(a, b, tol=24):
    return all(abs(i - j) <= tol for i, j in zip(a, b))


# Sample every cell; keep the house (anything that isn't the disc or the transparent outside).
n = int((SIZE - OX) // CELL)
cells = {}
for gy in range(n):
    for gx in range(n):
        rgb, a = pixel(OX + (gx + 0.5) * CELL, OY + (gy + 0.5) * CELL)
        if a < 128 or close(rgb, DISC):
            continue
        cells[(gx, gy)] = "#FF%02X%02X%02X" % rgb


def paths(ox=0.0, oy=0.0, s=1.0):
    """Cell squares merged into horizontal runs, one path per colour."""
    by_color = {}
    for (gx, gy), col in cells.items():
        by_color.setdefault(col, set()).add((gx, gy))
    out = []
    for col in sorted(by_color, key=lambda c: -len(by_color[c])):
        pts = by_color[col]
        d = []
        for gy in range(n):
            gx = 0
            while gx < n:
                if (gx, gy) in pts:
                    x0 = gx
                    while (gx, gy) in pts:
                        gx += 1
                    x = ox + (OX + x0 * CELL) * s
                    y = oy + (OY + gy * CELL) * s
                    w = (gx - x0) * CELL * s
                    h = CELL * s
                    d.append(f"M{x:.2f},{y:.2f}h{w:.2f}v{h:.2f}h{-w:.2f}z")
                else:
                    gx += 1
        out.append(f'    <path android:fillColor="{col}" android:pathData="{"".join(d)}" />')
    return "\n".join(out)


# Crop to the house plus one cell of margin, as a centred square, so it fills the icon.
gxs = [gx for gx, _ in cells]
gys = [gy for _, gy in cells]
bx0, bx1 = OX + min(gxs) * CELL, OX + (max(gxs) + 1) * CELL
by0, by1 = OY + min(gys) * CELL, OY + (max(gys) + 1) * CELL
side = max(bx1 - bx0, by1 - by0) + 2 * CELL
cx0 = (bx0 + bx1) / 2 - side / 2
cy0 = (by0 + by1) / 2 - side / 2

res = sys.argv[1]
open(os.path.join(res, "ic_launcher.xml"), "w").write(f'''<?xml version="1.0" encoding="utf-8"?>
<!-- Launcher3TV icon, traced from assets-src/icon-reference.png by tools/make_icon.py. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="48dp" android:height="48dp"
    android:viewportWidth="{side:.2f}" android:viewportHeight="{side:.2f}">
{paths(-cx0, -cy0)}
</vector>
''')

# TV banner, 16:9: the house centred on the launcher's background, 70 of 90 units tall.
s = 70 / side
ox, oy = (160 - side * s) / 2 - cx0 * s, (90 - side * s) / 2 - cy0 * s
open(os.path.join(res, "banner.xml"), "w").write(f'''<?xml version="1.0" encoding="utf-8"?>
<!-- Launcher3TV banner, traced from assets-src/icon-reference.png by tools/make_icon.py. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="160dp" android:height="90dp"
    android:viewportWidth="160" android:viewportHeight="90">
    <path android:fillColor="#FF0E0E0F" android:pathData="M0,0h160v90h-160z" />
{paths(ox, oy, s)}
</vector>
''')

# Optional preview of what the icon draws (PPM, 256 px; convert with ffmpeg).
if len(sys.argv) > 2:
    P = 256
    with open(sys.argv[2], "wb") as f:
        f.write(f"P6 {P} {P} 255\n".encode())
        for y in range(P):
            row = bytearray()
            for x in range(P):
                sx, sy = cx0 + x * side / P, cy0 + y * side / P
                gx, gy = int((sx - OX) // CELL), int((sy - OY) // CELL)
                col = cells.get((gx, gy))
                if col:
                    row += bytes(int(col[i:i + 2], 16) for i in (3, 5, 7))
                else:
                    row += b"\xff\xff\xff"
            f.write(row)
print(f"ok: {len(cells)} cells, {len(set(cells.values()))} colours")
