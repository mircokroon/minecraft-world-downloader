import sys, os, glob, zlib, struct

base = os.path.dirname(os.path.abspath(__file__))
exec(open(os.path.join(base, "inspect_region.py")).read().split("def main()")[0])

ent_dir = sys.argv[1]

def find_entities(chunk):
    # 1.17+ separate entity file: top-level "Entities"
    e = chunk.get("Entities")
    if e is not None: return e
    lvl = chunk.get("Level")
    if lvl:
        e = lvl.get("Entities")
        if e is not None: return e
    return []

def name_of(ent):
    cn = ent.get("CustomName")
    return cn if cn is not None else None

hopper_carts = []
named = []          # (id, customname, x,y,z)
total_ent = 0
type_counts = {}

def pos(ent):
    p = ent.get("Pos")
    if p and len(p) == 3:
        return tuple(round(v) for v in p)
    return (ent.get("x"), ent.get("y"), ent.get("z"))

def walk(ent):
    global total_ent
    total_ent += 1
    eid = ent.get("id","")
    type_counts[eid] = type_counts.get(eid,0)+1
    cn = name_of(ent)
    if cn:
        x,y,z = pos(ent)
        named.append((eid, cn, x,y,z))
    if eid == "minecraft:hopper_minecart":
        items = ent.get("Items") or []
        x,y,z = pos(ent)
        summ = ", ".join("%s x%s"%(it.get("id","?"), it.get("Count","?")) for it in items) if items else "(empty)"
        hopper_carts.append((x,y,z, cn, summ))
    # recurse into passengers
    for p in (ent.get("Passengers") or []):
        walk(p)

for path in sorted(glob.glob(os.path.join(ent_dir, "*.mca"))):
    try:
        chunks = parse_region(path)
    except Exception as e:
        print("  !! failed", os.path.basename(path), e); continue
    for c in chunks:
        if c is None: continue
        for ent in find_entities(c):
            walk(ent)

import json, re, collections

def _grab_json(o):
    if isinstance(o, str): return o
    if isinstance(o, dict):
        t = o.get("text","")
        for e in o.get("extra",[]) or []:
            t += _grab_json(e)
        return t
    if isinstance(o, list):
        return "".join(_grab_json(e) for e in o)
    return ""

def _grab_nbt(o):
    # text component stored as nested NBT (dict / list / str)
    if isinstance(o, str): return o
    if isinstance(o, dict):
        t = o.get("text","")
        if not isinstance(t, str): t = ""
        for e in (o.get("extra") or []):
            t += _grab_nbt(e)
        return t
    if isinstance(o, list):
        return "".join(_grab_nbt(e) for e in o)
    return ""

def clean_name(cn):
    if isinstance(cn, (dict, list)):
        txt = _grab_nbt(cn)
        return txt or repr(cn)[:60]
    if not isinstance(cn, str):
        return str(cn)
    s = cn.strip()
    # plain clean JSON?
    if s.startswith("{") or s.startswith("["):
        try:
            return _grab_json(json.loads(s)) or s
        except Exception:
            pass
    # raw modified-UTF8 NBT text-component bytes: decode null framing then pull readable runs
    raw = s.encode('latin-1').replace(b"\xc0\x80", b"\x00")
    runs = re.findall(rb"[\x20-\x7e]{2,}", raw)
    runs = [r.decode("ascii") for r in runs]
    # drop the literal NBT key names so we keep the actual value
    runs = [r for r in runs if r not in ("text", "extra", "color", "bold", "italic")]
    if runs:
        return max(runs, key=len)
    if all(31 < ord(c) < 127 for c in s) and s:
        return s
    return repr(s)[:40]

out = []
out.append("=== HOPPER MINECARTS: %d ===" % len(hopper_carts))
nonempty = [h for h in hopper_carts if h[4] != "(empty)"]
out.append("(%d empty, %d with items)" % (len(hopper_carts)-len(nonempty), len(nonempty)))
for x,y,z,cn,summ in hopper_carts:
    label = (" name=%s"%clean_name(cn)) if cn else ""
    out.append("  (%s,%s,%s)%s  %s" % (x,y,z,label,summ))

out.append("")
out.append("=== NAME-TAGGED (custom-named) ENTITIES: %d ===" % len(named))
# group by (mob type, name)
grp = collections.Counter()
for eid,cn,x,y,z in named:
    grp[(eid.replace("minecraft:",""), clean_name(cn))] += 1
out.append("-- grouped by type + name --")
for (eid,name),n in sorted(grp.items(), key=lambda kv:(-kv[1], kv[0])):
    out.append("  %4dx  %-22s '%s'" % (n, eid, name))
out.append("")
out.append("-- individual locations --")
for eid,cn,x,y,z in named:
    out.append("  %-22s '%s'  (%s,%s,%s)" % (eid.replace("minecraft:",""), clean_name(cn), x,y,z))

out.append("")
out.append("total entities scanned: %d" % total_ent)

with open(os.path.join(base, "entities_report.txt"), "w", encoding="utf-8") as f:
    f.write("\n".join(out))
print("wrote entities_report.txt (%d lines)" % len(out))
print("hopper_minecarts=%d (nonempty=%d)  named=%d" % (len(hopper_carts), len(nonempty), len(named)))
