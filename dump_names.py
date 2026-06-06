import sys, os, glob
base = os.path.dirname(os.path.abspath(__file__))
exec(open(os.path.join(base, "inspect_region.py")).read().split("def main()")[0])

ent_dir = sys.argv[1]
def find_entities(chunk):
    e = chunk.get("Entities")
    if e is not None: return e
    lvl = chunk.get("Level")
    if lvl:
        e = lvl.get("Entities")
        if e is not None: return e
    return []

shown = 0
seen_kinds = set()
for path in sorted(glob.glob(os.path.join(ent_dir, "*.mca"))):
    chunks = parse_region(path)
    for c in chunks:
        if c is None: continue
        for ent in find_entities(c):
            cn = ent.get("CustomName")
            if cn is None: continue
            kind = type(cn).__name__
            tag = (kind, str(cn)[:4])
            if tag in seen_kinds: continue
            seen_kinds.add(tag)
            print("type=%s  id=%s" % (kind, ent.get("id")))
            if isinstance(cn, str):
                b = cn.encode('latin-1')
                print("  len=%d hex=%s" % (len(b), b.hex()))
                print("  ascii=%r" % cn)
            else:
                print("  value=%r" % cn)
            print()
            shown += 1
            if shown >= 12:
                sys.exit(0)
