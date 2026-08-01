import math
from PIL import Image, ImageDraw

# Exact palette from preview.html's :root custom properties
INK    = (13, 11, 24)      # --ink   background
RUNE   = (58, 50, 96)      # --rune  ring / borders
GOLD   = (255, 182, 72)    # --gold  primary accent (gear + spokes)
MIST   = (242, 238, 255)   # --mist  light text (tiny highlight only)

CANVAS = 1024
CX = CY = CANVAS / 2

def gear_polygon(cx, cy, n_teeth, outer_r, root_r, tooth_frac=0.62):
    """Trapezoidal-tooth gear silhouette as a point list."""
    pts = []
    step = 360 / n_teeth
    tooth_half = step * tooth_frac / 2
    root_half  = step * 0.5
    for i in range(n_teeth):
        center = i * step
        a0 = math.radians(center - root_half)
        a1 = math.radians(center - tooth_half)
        a2 = math.radians(center + tooth_half)
        a3 = math.radians(center + root_half)
        pts.append((cx + root_r*math.cos(a0), cy + root_r*math.sin(a0)))
        pts.append((cx + outer_r*math.cos(a1), cy + outer_r*math.sin(a1)))
        pts.append((cx + outer_r*math.cos(a2), cy + outer_r*math.sin(a2)))
        pts.append((cx + root_r*math.cos(a3), cy + root_r*math.sin(a3)))
    return pts

def draw_spoke(draw, cx, cy, angle_deg, r_start, r_end, width, color):
    a = math.radians(angle_deg)
    x0, y0 = cx + r_start*math.cos(a), cy + r_start*math.sin(a)
    x1, y1 = cx + r_end*math.cos(a),   cy + r_end*math.sin(a)
    draw.line([(x0,y0),(x1,y1)], fill=color, width=width)
    # round the far tip so it doesn't look like a chopped-off rectangle
    r = width/2
    draw.ellipse([x1-r,y1-r,x1+r,y1+r], fill=color)

def build(spoke_end_r, transparent_bg, ring_r=225, ring_w=38, tooth_outer=195,
          tooth_root=150, spoke_w=48, hole_r=54, hub_ring_r=70, hub_ring_w=10):
    mode = "RGBA" if transparent_bg else "RGB"
    bg = (0,0,0,0) if transparent_bg else INK
    img = Image.new(mode, (CANVAS, CANVAS), bg)
    d = ImageDraw.Draw(img)

    for i in range(8):
        draw_spoke(d, CX, CY, i*45, 30, spoke_end_r, spoke_w, GOLD)

    d.ellipse([CX-ring_r, CY-ring_r, CX+ring_r, CY+ring_r], outline=RUNE, width=ring_w)

    d.polygon(gear_polygon(CX, CY, 8, tooth_outer, tooth_root), fill=GOLD)
    d.ellipse([CX-hub_ring_r, CY-hub_ring_r, CX+hub_ring_r, CY+hub_ring_r], outline=RUNE, width=hub_ring_w)
    d.ellipse([CX-hole_r, CY-hole_r, CX+hole_r, CY+hole_r],
              fill=(0,0,0,0) if transparent_bg else INK)
    return img

# Full-bleed version for the legacy launcher icon (opaque ink background)
full = build(spoke_end_r=500, transparent_bg=False)
full.save('/home/claude/build/icon/icon_full_master.png')

# Safe-zone version for the adaptive icon foreground (transparent, content
# kept inside the ~66% centre so nothing important gets masked off)
fg = build(spoke_end_r=300, transparent_bg=True, ring_r=225)
fg.save('/home/claude/build/icon/icon_fg_master.png')

print("done", full.size, fg.size)
