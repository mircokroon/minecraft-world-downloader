import os, glob, re, json, collections
base = os.path.dirname(os.path.abspath(__file__))
exec(open(os.path.join(base, "scan_locks.py")).read().split("signs = {}")[0])

world = r"C:\Users\cntow\Documents\GitHub\HeapHongShekWorld\world"
region_dir = os.path.join(world, "region")

firstline_counts = collections.Counter()
lock_signs = 0
samples = []
for path in sorted(glob.glob(os.path.join(region_dir, "*.mca"))):
    try: chunks = parse_region(path)
    except Exception: continue
    for c in chunks:
        if c is None: continue
        for be in block_entities(c):
            if not isinstance(be, dict): continue
            bid = be.get("id","").replace("minecraft:","")
            if not (bid.endswith("sign")): continue
            ls = sign_lines(be)
            if not ls: continue
            f = norm(ls[0])
            if f in LOCK_FIRST or f.startswith("private") or f.startswith("moreusers") or f.startswith("everyone"):
                lock_signs += 1
                firstline_counts[ls[0].strip()] += 1
                if len(samples) < 15:
                    samples.append((be.get("x"),be.get("y"),be.get("z"), ls))
print("total lock-style signs (any [Private]/[More Users]/[Everyone] first line):", lock_signs)
print("by first-line text:")
for k,v in firstline_counts.most_common():
    print("  %4d  %r" % (v,k))
print("\nsamples:")
for x,y,z,ls in samples:
    print("  (%s,%s,%s)  %s" % (x,y,z, " | ".join(ls)))
