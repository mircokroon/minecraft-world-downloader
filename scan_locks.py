import sys, os, glob, zlib, struct, re, json, collections

base = os.path.dirname(os.path.abspath(__file__))
exec(open(os.path.join(base, "inspect_region.py")).read().split("def main()")[0])

world = sys.argv[1] if len(sys.argv) > 1 else r"C:\Users\cntow\Documents\GitHub\HeapHongShekWorld\world"
region_dir = os.path.join(world, "region")

CONTAINER_IDS = {
    "chest", "trapped_chest", "barrel", "shulker_box", "hopper", "dropper",
    "dispenser", "furnace", "blast_furnace", "smoker", "brewing_stand",
    "campfire", "soul_campfire", "lectern", "chiseled_bookshelf",
}
SIGN_IDS = {"sign", "hanging_sign", "wall_sign", "wall_hanging_sign"}
# LockettePro first-line keywords (after stripping brackets/spaces/colors)
LOCK_FIRST = {"private", "more users", "moreusers", "everyone", "additional"}

def _grab(o):
    if isinstance(o, str): return o
    if isinstance(o, dict):
        t = o.get("text", "")
        if not isinstance(t, str): t = ""
        for e in (o.get("extra") or []):
            t += _grab(e)
        return t
    if isinstance(o, list):
        return "".join(_grab(e) for e in o)
    return ""

def decode_line(s):
    if not isinstance(s, str):
        return _grab(s)
    s = s.strip()
    if not s:
        return ""
    if s[0] in "{[":
        try:
            return _grab(json.loads(s))
        except Exception:
            pass
    # modified-UTF8 NBT text-component bytes
    raw = s.encode("latin-1", "ignore").replace(b"\xc0\x80", b" ")
    runs = re.findall(rb"[\x20-\x7e]{2,}", raw)
    runs = [r.decode("ascii") for r in runs if r.decode("ascii") not in ("text", "extra", "color")]
    if runs:
        return max(runs, key=len)
    return "".join(c for c in s if 31 < ord(c) < 127)

def sign_lines(be):
    out = []
    ft = be.get("front_text")
    if isinstance(ft, dict) and isinstance(ft.get("messages"), list):
        out = [decode_line(m) for m in ft["messages"]]
    else:
        for k in ("Text1", "Text2", "Text3", "Text4"):
            if be.get(k) is not None:
                out.append(decode_line(be.get(k)))
    return [x for x in out]

def block_entities(chunk):
    be = chunk.get("block_entities")
    if be is not None: return be
    lvl = chunk.get("Level")
    if lvl and lvl.get("TileEntities") is not None: return lvl.get("TileEntities")
    return []

def norm(line):
    return re.sub(r"[\[\]\s]", "", (line or "")).lower()

signs = {}        # (x,y,z) -> [lines]
containers = []   # (id, (x,y,z), has_items, total_count)

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
            if any(s in bid for s in SIGN_IDS) or bid.endswith("sign"):
                signs[loc] = sign_lines(be)
            base_id = bid
            if base_id in CONTAINER_IDS or base_id.endswith("shulker_box"):
                items = be.get("Items") or []
                total = 0
                for it in items:
                    try: total += int(it.get("Count", it.get("count", 1)))
                    except: total += 1
                containers.append((base_id, loc, len(items) > 0, total))

def is_lock_first(line):
    f = norm(line)
    return f in LOCK_FIRST or f.startswith("private") or f.startswith("moreusers") or f.startswith("everyone")

# index containers by location for quick lookup
cont_by_loc = {loc: (bid, has, total) for bid, loc, has, total in containers}

def adjacent_container(loc):
    x, y, z = loc
    if None in (x, y, z): return None
    for dx, dy, dz in ((1,0,0),(-1,0,0),(0,1,0),(0,-1,0),(0,0,1),(0,0,-1)):
        hit = cont_by_loc.get((x+dx, y+dy, z+dz))
        if hit: return ((x+dx, y+dy, z+dz),) + hit
    return None

# gather every lock sign
lock_signs = []
for loc, ls in signs.items():
    if ls and is_lock_first(ls[0]):
        owner = ls[1].strip().strip('"') if len(ls) > 1 else ""
        users = [l.strip().strip('"') for l in ls[2:] if l.strip().strip('"')]
        lock_signs.append((loc, ls[0].strip(), owner, users))

rows = []   # (sign_loc, locktype, owner, users, target_or_None)
for sloc, ltype, owner, users in lock_signs:
    rows.append((sloc, ltype, owner, users, adjacent_container(sloc)))

captured = [r for r in rows if r[4] and r[4][2]]          # adjacent container has items
empty    = [r for r in rows if r[4] and not r[4][2]]       # adjacent container saved but empty
notdl    = [r for r in rows if not r[4]]                    # protected chest not in downloaded data

owners = collections.Counter((r[2] or "?") for r in rows)

lines = []
lines.append("============ LOCKETTEPRO PROTECTION REPORT ============")
lines.append("world: %s" % world)
lines.append("containers scanned: %d   signs scanned: %d" % (len(containers), len(signs)))
lines.append("LockettePro lock signs ([Private]/[More Users]/[Everyone]): %d" % len(rows))
lines.append("   next to a CAPTURED container (we have its items):      %d" % len(captured))
lines.append("   next to a saved-but-EMPTY container (locked-out):      %d" % len(empty))
lines.append("   protecting a chest NOT in downloaded chunks yet:       %d" % len(notdl))
lines.append("")
lines.append("-- owners (incl. [More Users] additions) --")
all_people = collections.Counter()
for r in rows:
    if r[2]: all_people[r[2]] += 1
    for u in r[3]: all_people[u] += 1
for p, n in all_people.most_common():
    lines.append("   %4d  %s" % (n, p))
lines.append("")

def fmt(r, tag):
    sloc, ltype, owner, users, tgt = r
    who = owner or "?"
    if users: who += " +[" + ", ".join(users) + "]"
    if tgt:
        tloc, tid, thas, ttot = tgt
        status = ("items=%d" % ttot) if thas else "EMPTY"
        return "  %-6s sign@(%6s,%4s,%6s) %-12s -> %-13s @(%s,%s,%s) %-8s  %s" % (
            tag, sloc[0], sloc[1], sloc[2], ltype, tid, tloc[0], tloc[1], tloc[2], status, who)
    return "  %-6s sign@(%6s,%4s,%6s) %-12s -> (no container in downloaded data)        %s" % (
        tag, sloc[0], sloc[1], sloc[2], ltype, who)

lines.append("-- CAPTURED protected containers (you already have these) --")
for r in sorted(captured, key=lambda r: r[0]): lines.append(fmt(r, "[OK]"))
lines.append("")
lines.append("-- saved but EMPTY (locked-out: open on server to grab) --")
for r in sorted(empty, key=lambda r: r[0]): lines.append(fmt(r, "[LOCK]"))
lines.append("")
lines.append("-- protected chests NOT downloaded yet (walk near + open them) --")
for r in sorted(notdl, key=lambda r: r[0]): lines.append(fmt(r, "[????]"))

report = os.path.join(base, "lock_report.txt")
with open(report, "w", encoding="utf-8") as f:
    f.write("\n".join(lines))
print("\n".join(lines))
print("\n--> report written to: %s" % report)
