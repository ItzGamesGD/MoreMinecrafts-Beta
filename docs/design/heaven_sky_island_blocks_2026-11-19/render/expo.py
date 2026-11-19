import nbtlib, collections, numpy as np, json, sys
base='../../../../src/main/resources/data/moreminecrafts/structure/sky_islands/'
f=sys.argv[1]
n=nbtlib.load(base+f); pal=[str(p['id']) for p in n['palette']]
SX,SY,SZ=[int(x) for x in n['size']]
blocks={tuple(int(x) for x in b['pos']):pal[int(b['state'])] for b in n['blocks']}
FULL={'minecraft:stone','minecraft:dirt','minecraft:andesite','minecraft:tuff','minecraft:grass_block','minecraft:coarse_dirt','minecraft:oak_log'}
TERR=['minecraft:grass_block','minecraft:dirt','minecraft:coarse_dirt','minecraft:stone','minecraft:andesite','minecraft:tuff']
dirs={'up':(0,1,0),'down':(0,-1,0),'n':(0,0,-1),'s':(0,0,1),'w':(-1,0,0),'e':(1,0,0)}
exp=collections.defaultdict(lambda: collections.Counter())
anyexp=collections.Counter()
# column top surface
top={}
for (x,y,z),b in blocks.items():
    if b in TERR: top[(x,z)]=max(top.get((x,z),-1),y)
bot={}
for (x,y,z),b in blocks.items():
    if b in TERR: bot[(x,z)]=min(bot.get((x,z),999),y)
depth=collections.defaultdict(collections.Counter)
ys=collections.defaultdict(list)
for p,b in blocks.items():
    if b not in TERR: continue
    x,y,z=p
    e=False
    for d,(dx,dy,dz) in dirs.items():
        nb=blocks.get((x+dx,y+dy,z+dz))
        if nb not in FULL:
            exp[b][d]+=1; e=True
    if e: anyexp[b]+=1
    depth[b][min(top[(x,z)]-y,12)]+=1
    ys[b].append(y)
tot=sum(1 for b in blocks.values() if b in TERR)
print(f,'size',SX,SY,SZ,'terrain',tot,'all',len(blocks))
sumside={b:sum(exp[b][d] for d in 'nswe') for b in TERR}
T=lambda k:sum(exp[b][k] for b in TERR)
tu=T('up');td=T('down');ts=sum(sumside.values())
print('%-24s %6s %6s %6s %6s %6s  up%%  side%% down%%  ymin ymax ymean'%('block','count','%','exposed','up','side'))
for b in TERR:
    c=len(ys[b]); 
    print('%-24s %6d %5.1f %6d %6d %6d %5d | %4.1f %5.1f %5.1f | %3d %3d %5.1f'%(b,c,100*c/tot,anyexp[b],exp[b]['up'],sumside[b],exp[b]['down'],100*exp[b]['up']/tu,100*sumside[b]/ts,100*exp[b]['down']/td,min(ys[b]),max(ys[b]),np.mean(ys[b])))
print('faces up/side/down', tu,ts,td)
print('depth below column top (0=top block):')
for b in TERR: print('  %-22s'%b, ' '.join('%d:%d'%(k,v) for k,v in sorted(depth[b].items())))
# y-layer composition
print('y layer composition (terrain only):')
for y in range(SY-1,-1,-1):
    c=collections.Counter(b for (x,yy,z),b in blocks.items() if yy==y and b in TERR)
    if c: print('  y%2d n=%4d '%(y,sum(c.values()))+' '.join('%s:%d'%(k.split(':')[1][:6],v) for k,v in c.most_common()))
others=collections.Counter(b for b in blocks.values() if b not in TERR)
print('non-terrain', others.most_common())
# water positions
wy=[p[1] for p,b in blocks.items() if b=='minecraft:water']; print('water y',min(wy) if wy else None,max(wy) if wy else None)
