import sys, zlib, struct, io

# Minimal NBT reader
class R:
    def __init__(self, b): self.b=b; self.i=0
    def u1(self): v=self.b[self.i]; self.i+=1; return v
    def i1(self): v=self.b[self.i]; self.i+=1; return v-256 if v>=128 else v
    def u2(self): v=struct.unpack('>H', self.b[self.i:self.i+2])[0]; self.i+=2; return v
    def i2(self): v=struct.unpack('>h', self.b[self.i:self.i+2])[0]; self.i+=2; return v
    def i4(self): v=struct.unpack('>i', self.b[self.i:self.i+4])[0]; self.i+=4; return v
    def i8(self): v=struct.unpack('>q', self.b[self.i:self.i+8])[0]; self.i+=8; return v
    def f4(self): v=struct.unpack('>f', self.b[self.i:self.i+4])[0]; self.i+=4; return v
    def f8(self): v=struct.unpack('>d', self.b[self.i:self.i+8])[0]; self.i+=8; return v
    def s(self):
        n=self.u2(); v=self.b[self.i:self.i+n].decode('latin-1'); self.i+=n; return v

def read_payload(r, t):
    if t==1: return r.i1()
    if t==2: return r.i2()
    if t==3: return r.i4()
    if t==4: return r.i8()
    if t==5: return r.f4()
    if t==6: return r.f8()
    if t==7:
        n=r.i4(); v=r.b[r.i:r.i+n]; r.i+=n; return list(v)
    if t==8: return r.s()
    if t==9:
        et=r.u1(); n=r.i4(); return [read_payload(r,et) for _ in range(n)]
    if t==10:
        d={}
        while True:
            tt=r.u1()
            if tt==0: break
            name=r.s(); d[name]=read_payload(r,tt)
        return d
    if t==11:
        n=r.i4(); return [r.i4() for _ in range(n)]
    if t==12:
        n=r.i4(); return [r.i8() for _ in range(n)]
    raise Exception("bad tag "+str(t))

def read_nbt(b):
    r=R(b)
    tt=r.u1()
    if tt==0: return None
    r.s()
    return read_payload(r,tt)

def parse_region(path):
    with open(path,'rb') as f: data=f.read()
    chunks=[]
    for idx in range(1024):
        off=struct.unpack('>I', b'\x00'+data[idx*4:idx*4+3])[0]
        cnt=data[idx*4+3]
        if off==0: continue
        start=off*4096
        length=struct.unpack('>I', data[start:start+4])[0]
        comp=data[start+4]
        raw=data[start+5:start+4+length]
        try:
            if comp==2: nbt_bytes=zlib.decompress(raw)
            elif comp==1: import gzip; nbt_bytes=gzip.decompress(raw)
            else: continue
            chunks.append(read_nbt(nbt_bytes))
        except Exception as e:
            pass
    return chunks

def main():
    path=sys.argv[1]
    chunks=parse_region(path)
    total_be=0
    containers=0
    with_items=0
    print("chunks present:", len(chunks))
    for c in chunks:
        if c is None: continue
        bes = c.get("block_entities")
        if bes is None:
            lvl=c.get("Level")
            if lvl: bes=lvl.get("TileEntities")
        if not bes: continue
        for be in bes:
            total_be+=1
            bid=be.get("id","")
            items=be.get("Items")
            if items is not None:
                containers+=1
                if len(items)>0:
                    with_items+=1
                    x,y,z=be.get("x"),be.get("y"),be.get("z")
                    summary=", ".join("%s x%s"%(it.get("id",it.get("Slot")),it.get("Count","?")) for it in items[:9])
                    print("  %-28s (%s,%s,%s) items=%d : %s"%(bid,x,y,z,len(items),summary))
    print("total block_entities=%d  containers=%d  containers_with_items=%d"%(total_be,containers,with_items))

main()
