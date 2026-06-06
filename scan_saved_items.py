import sys, os, glob, zlib, struct, collections

base = os.path.dirname(os.path.abspath(__file__))
exec(open(os.path.join(base, "inspect_region.py")).read().split("def main()")[0])

world = sys.argv[1] if len(sys.argv) > 1 else r"C:\Users\cntow\Documents\GitHub\HeapHongShekWorld\world"
region_dir = os.path.join(world, "region")

def block_entities(chunk):
    be = chunk.get("block_entities")
    if be is not None: return be
    lvl = chunk.get("Level")
    if lvl and lvl.get("TileEntities") is not None: return lvl.get("TileEntities")
    return []

item_totals = collections.Counter()      # item id -> total count
item_containers = collections.Counter()   # item id -> # of containers holding it
by_container_type = collections.Counter()  # block id -> # filled
per_container = []                          # (blockid, loc, [(item,count)...])
filled = 0

for path in sorted(glob.glob(os.path.join(region_dir, "*.mca"))):
    try:
        chunks = parse_region(path)
    except Exception:
        continue
    for c in chunks:
        if c is None: continue
        for be in block_entities(c):
            if not isinstance(be, dict): continue
            bid = be.get("id", "").replace("minecraft:", "")
            loc = (be.get("x"), be.get("y"), be.get("z"))
            slots = []
            items = be.get("Items")
            if items:
                for it in items:
                    iid = it.get("id", "?").replace("minecraft:", "")
                    cnt = it.get("Count", it.get("count", 1))
                    try: cnt = int(cnt)
                    except: cnt = 1
                    slots.append((iid, cnt))
            for single_key in ("Item", "RecordItem"):
                one = be.get(single_key)
                if isinstance(one, dict):
                    iid = one.get("id", "?").replace("minecraft:", "")
                    cnt = one.get("Count", one.get("count", 1))
                    try: cnt = int(cnt)
                    except: cnt = 1
                    slots.append((iid, cnt))
            if be.get("Book") is not None:
                slots.append(("written_book(lectern)", 1))
            if not slots:
                continue
            filled += 1
            by_container_type[bid] += 1
            seen_here = set()
            for iid, cnt in slots:
                item_totals[iid] += cnt
                if iid not in seen_here:
                    item_containers[iid] += 1
                    seen_here.add(iid)
            per_container.append((bid, loc, slots))

lines = []
lines.append("================ SAVED ITEM TOTALS ================")
lines.append("world: %s" % world)
lines.append("filled containers: %d   distinct item types: %d   total items: %d"
             % (filled, len(item_totals), sum(item_totals.values())))
lines.append("")
lines.append("%-32s %12s %12s" % ("ITEM", "TOTAL", "CONTAINERS"))
lines.append("-" * 58)
for iid, total in item_totals.most_common():
    lines.append("%-32s %12d %12d" % (iid, total, item_containers[iid]))

lines.append("")
lines.append("================ CONTAINERS BY TYPE ================")
for bid, n in by_container_type.most_common():
    lines.append("  %-18s %d" % (bid, n))

lines.append("")
lines.append("================ EVERY FILLED CONTAINER ================")
for bid, loc, slots in sorted(per_container, key=lambda t: (t[1][1] is None, t[1])):
    summ = ", ".join("%s x%d" % (i, c) for i, c in slots)
    lines.append("  %-16s (%6s,%4s,%6s)  %s" % (bid, loc[0], loc[1], loc[2], summ))

report = os.path.join(base, "saved_items_report.txt")
with open(report, "w", encoding="utf-8") as f:
    f.write("\n".join(lines))

# print the digestible part (totals + container types) to console
TOTALS_END = lines.index("================ EVERY FILLED CONTAINER ================")
print("\n".join(lines[:TOTALS_END]))
print("\n--> full per-container breakdown (%d containers) written to:\n    %s" % (filled, report))
