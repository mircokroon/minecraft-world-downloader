import sys, os, glob, re, json, collections
base = os.path.dirname(os.path.abspath(__file__))
exec(open(os.path.join(base, "scan_locks.py")).read().split("signs = {}")[0])

world = r"C:\Users\cntow\Documents\GitHub\HeapHongShekWorld\world"
# center + radius from argv:  cx cy cz r
cx, cy, cz, rad = int(sys.argv[1]), int(sys.argv[2]), int(sys.argv[3]), int(sys.argv[4]) if len(sys.argv) > 4 else 24

def near(loc):
    x, y, z = loc
    if None in (x, y, z): return False
    return abs(x-cx) <= rad and abs(y-cy) <= rad and abs(z-cz) <= rad

def block_entities(chunk):
    be = chunk.get("block_entities")
    if be is not None: return be
    lvl = chunk.get("Level")
    if lvl and lvl.get("TileEntities") is not None: return lvl.get("TileEntities")
    return []

CONT = {"chest","trapped_chest","barrel","shulker_box","hopper","dropper","dispenser",
        "furnace","blast_furnace","smoker","brewing_stand","campfire","lectern"}

found = []
for path in sorted(glob.glob(os.path.join(world, "region", "*.mca"))):
    try: chunks = parse_region(path)
    except Exception: continue
    for c in chunks:
        if c is None: continue
        for be in block_entities(c):
            if not isinstance(be, dict): continue
            loc = (be.get("x"), be.get("y"), be.get("z"))
            if not near(loc): continue
            bid = be.get("id","").replace("minecraft:","")
            kind = None
            if bid.endswith("sign"):
                ls = [l for l in sign_lines(be)]
                txt = " | ".join(l for l in ls if l.strip())
                if txt.strip(): found.append(("sign", loc, txt))
            elif bid in CONT or bid.endswith("shulker_box"):
                items = be.get("Items") or []
                tot = sum(int(it.get("Count", it.get("count",1)) or 1) for it in items)
                summ = ", ".join("%s x%s"%(it.get("id","?").replace("minecraft:",""), it.get("Count",it.get("count","?"))) for it in items) or "(empty)"
                cn = be.get("CustomName")
                label = (" name=" + decode_line(cn)) if cn else ""
                found.append(("CONTAINER:"+bid, loc, "%s%s" % (summ, label)))

found.sort(key=lambda t: (t[1][1] if t[1][1] is not None else 0, t[1][0], t[1][2]))
print("=== Block entities within %d blocks of (%d,%d,%d) ===" % (rad, cx, cy, cz))
print("containers:", sum(1 for f in found if f[0].startswith("CONTAINER")), " signs:", sum(1 for f in found if f[0]=="sign"))
print()
for kind, loc, txt in found:
    print("  %-18s (%5s,%4s,%5s)  %s" % (kind, loc[0], loc[1], loc[2], txt))
