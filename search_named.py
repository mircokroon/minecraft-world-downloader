import sys, os, glob, zlib, struct, re

base = os.path.dirname(os.path.abspath(__file__))
exec(open(os.path.join(base, "inspect_region.py")).read().split("def main()")[0])

terms = [t.lower() for t in sys.argv[2:]] or ["bunker", "emergency", "key"]
roots = sys.argv[1].split(";")

def walk_strings(o, path):
    # yield (jsonpath, string) for every string anywhere in the NBT tree
    if isinstance(o, str):
        yield path, o
    elif isinstance(o, dict):
        for k, v in o.items():
            yield from walk_strings(v, path + "/" + str(k))
    elif isinstance(o, list):
        for i, v in enumerate(o):
            yield from walk_strings(v, path + "[%d]" % i)

def find_entities(chunk):
    e = chunk.get("Entities")
    if e is not None: return e
    lvl = chunk.get("Level")
    if lvl and lvl.get("Entities") is not None: return lvl.get("Entities")
    return []

def block_entities(chunk):
    be = chunk.get("block_entities")
    if be is not None: return be
    lvl = chunk.get("Level")
    if lvl and lvl.get("TileEntities") is not None: return lvl.get("TileEntities")
    return []

hits = []
for root in roots:
    for path in sorted(glob.glob(os.path.join(root, "*.mca"))):
        try:
            chunks = parse_region(path)
        except Exception as e:
            continue
        for c in chunks:
            if c is None: continue
            # block entities (containers) + entities (mobs, item frames, armor stands, dropped items)
            for be in block_entities(c):
                if not isinstance(be, dict): continue
                loc = (be.get("x"), be.get("y"), be.get("z"))
                bid = be.get("id","")
                for jp, s in walk_strings(be, ""):
                    low = s.lower()
                    if any(t in low for t in terms):
                        hits.append((bid, loc, jp, s))
            for ent in find_entities(c):
                if not isinstance(ent, dict): continue
                p = ent.get("Pos")
                loc = tuple(round(v) for v in p) if (p and len(p)==3) else (ent.get("x"),ent.get("y"),ent.get("z"))
                eid = ent.get("id","")
                for jp, s in walk_strings(ent, ""):
                    low = s.lower()
                    if any(t in low for t in terms):
                        hits.append((eid, loc, jp, s))

# clean display of mod-utf8 / json strings
def disp(s):
    raw = s.encode('latin-1','ignore').replace(b'\xc0\x80', b' ')
    runs = re.findall(rb"[\x20-\x7e]{2,}", raw)
    return " | ".join(r.decode('ascii') for r in runs) if runs else s

print("search terms:", terms)
print("hits:", len(hits))
seen = set()
for bid, loc, jp, s in hits:
    key = (bid, loc, disp(s))
    if key in seen: continue
    seen.add(key)
    print("  %-22s %-22s %s" % (bid.replace("minecraft:",""), str(loc), disp(s)))
    print("        field: %s" % jp)
