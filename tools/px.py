# Usage: python3 -I tools/px.py image.png x,y [x,y ...]  -> hex colour at each point (full-res coordinates)
import struct,zlib,sys
def png(p):
    d=open(p,'rb').read(); i=8; idat=b''
    while i<len(d):
        l=struct.unpack('>I',d[i:i+4])[0]; t=d[i+4:i+8]; c=d[i+8:i+8+l]
        if t==b'IHDR': w,h,bd,ct=struct.unpack('>IIBB',c[:10])
        if t==b'IDAT': idat+=c
        i+=12+l
    raw=zlib.decompress(idat); bpp=4 if ct==6 else 3; st=w*bpp; rows=[]; prev=bytearray(st); o=0
    for y in range(h):
        f=raw[o]; o+=1; line=bytearray(raw[o:o+st]); o+=st
        if f==1:
            for x in range(bpp,st): line[x]=(line[x]+line[x-bpp])&255
        elif f==2:
            for x in range(st): line[x]=(line[x]+prev[x])&255
        elif f==3:
            for x in range(st): line[x]=(line[x]+((line[x-bpp] if x>=bpp else 0)+prev[x])//2)&255
        elif f==4:
            for x in range(st):
                a=line[x-bpp] if x>=bpp else 0; b=prev[x]; c=prev[x-bpp] if x>=bpp else 0
                p=a+b-c; pa,pb,pc=abs(p-a),abs(p-b),abs(p-c)
                line[x]=(line[x]+(a if pa<=pb and pa<=pc else b if pb<=pc else c))&255
        rows.append(line); prev=line
    return bpp,rows
bpp,R=png(sys.argv[1])
for a in sys.argv[2:]:
    x,y=map(int,a.split(',')); print(a, '#%02X%02X%02X'%tuple(R[y][x*bpp:x*bpp+3]))
