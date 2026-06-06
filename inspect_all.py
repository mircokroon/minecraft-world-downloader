import sys, os, glob, zlib, struct

base = os.path.dirname(os.path.abspath(__file__))
# load parser functions (everything before main()) from inspect_region.py
exec(open(os.path.join(base, "inspect_region.py")).read().split("def main()")[0])

region_dir = sys.argv[1]

grand_be = 0
grand_cont = 0
findings = []
for path in sorted(glob.glob(os.path.join(region_dir, "*.mca"))):
    try:
        chunks = parse_region(path)
    except Exception as e:
        print("  !! failed", os.path.basename(path), e); continue
    for c in chunks:
        if c is None: continue
        bes = c.get("block_entities")
        if bes is None:
            lvl = c.get("Level")
            if lvl: bes = lvl.get("TileEntities")
        if not bes: continue
        for be in bes:
            grand_be += 1
            bid = be.get("id","")
            items = be.get("Items")
            book = be.get("Book")
            rec = be.get("RecordItem")
            single = be.get("Item")
            x,y,z = be.get("x"),be.get("y"),be.get("z")
            if items is not None:
                grand_cont += 1
                if len(items) > 0:
                    summ = ", ".join("%s x%s"%(it.get("id","?"), it.get("Count","?")) for it in items)
                    findings.append("%-22s (%5s,%4s,%5s)  %d slot(s): %s"%(bid.replace("minecraft:",""),x,y,z,len(items),summ))
            elif book is not None or rec is not None or single is not None:
                what = "lectern book" if book is not None else ("jukebox record" if rec is not None else "item")
                findings.append("%-22s (%5s,%4s,%5s)  %s"%(bid.replace("minecraft:",""),x,y,z,what))

print("=== Saved contents in %s ===" % region_dir)
for f in findings:
    print(" ", f)
print()
print("totals: block_entities=%d  containers=%d  entities_with_contents=%d"%(grand_be, grand_cont, len(findings)))
