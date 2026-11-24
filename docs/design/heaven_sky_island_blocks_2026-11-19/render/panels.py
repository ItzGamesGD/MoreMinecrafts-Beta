import numpy as np, os, sys
from PIL import Image, ImageDraw, ImageFont
import shade, palettes as pl
OUT=sys.argv[1]; os.makedirs(OUT,exist_ok=True)
F=lambda s,b=False: ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans%s.ttf'%('-Bold' if b else ''),s)
V,N=pl.build()
TITLE={'REF':'REFERENCE - current vanilla meadow palette','A':'A - Vanilla extension','B':'B - Bright garden','C':'C - Vivid fantasy','D':'D - Natural strata','E':'E - Gentle heaven'}
TAG='NBT-BASED render | sky_islands/meadow/plain_medium_d | same camera & light | concept textures'
def stamp(im,title):
    d=ImageDraw.Draw(im); d.rectangle((0,0,im.width,44),fill=(20,28,40))
    d.text((14,8),title,font=F(22,True),fill=(255,255,255))
    d.text((14,im.height-26),TAG,font=F(15),fill=(30,40,60)); return im
def swatch(p):
    pal=pl.PALETTES[p]; S=96
    used=[]
    for slot in pl.SLOTS:
        b=pal[slot]
        if b not in [u[0] for u in used]: used.append((b,[slot]))
        else: [u[1].append(slot) for u in used if u[0]==b]
    tiles=[]
    for b,slots in used:
        tex=N.get(b) or V.get(b); new=b in N
        faces=[('top',tex['top']),('side',tex['side'])] if not np.array_equal(tex['top'],tex['side']) else [('all',tex['top'])]
        if 'top2' in tex: faces.insert(1,('top#2',tex['top2']))
        tiles.append((b,slots,new,faces))
    W=sum(14+max(len(f)*(S+6),int(F(12,True).getlength(('NEW ' if n else 'vanilla ')+b))+8,int(F(11).getlength('<- '+', '.join(sl)))+8) for b,sl,n,f in tiles)+14; H=S+86
    im=Image.new('RGB',(max(W,600),H),(245,246,248)); d=ImageDraw.Draw(im); x=14
    for b,slots,new,faces in tiles:
        x0=x
        for fn,t in faces:
            im.paste(Image.fromarray(np.clip(t,0,255).astype(np.uint8)).resize((S,S),Image.NEAREST),(x,30))
            d.text((x,30+S+2),fn,font=F(11),fill=(90,90,90)); x+=S+6
        d.rectangle((x0-3,26,x-3,30+S+3),outline=(214,120,40) if new else (150,150,150),width=3)
        d.text((x0,6),('NEW ' if new else 'vanilla ')+b,font=F(12,True),fill=(170,80,10) if new else (60,60,60))
        d.text((x0,30+S+18),'<- '+', '.join(slots),font=F(11),fill=(40,40,40))
        lab=('NEW ' if new else 'vanilla ')+b
        x=max(x,x0+int(F(12,True).getlength(lab))+8,x0+int(F(11).getlength('<- '+', '.join(slots)))+8)+14
    return im
for p in ['REF','A','B','C','D','E']:
    for v in ['full','edge','underside']:
        im=shade.shade('hb',v,p,V,N)
        stamp(im,TITLE[p]+'  ['+v+']').save(f'{OUT}/{p}_{v}.png',optimize=True)
    swatch(p).save(f'{OUT}/{p}_palette.png')
# comparison board
cells=[]
for p in ['REF','A','B','C','D','E']:
    a=shade.shade('hb','full',p,V,N).crop((290,170,1310,870)).resize((680,467))
    e=shade.shade('hb','edge',p,V,N).resize((340,227)); u=shade.shade('hb','underside',p,V,N).resize((340,227))
    c=Image.new('RGB',(680,467+227+40),(255,255,255)); c.paste(a,(0,40)); c.paste(e,(0,507)); c.paste(u,(340,507))
    ImageDraw.Draw(c).text((10,8),TITLE[p],font=F(20,True),fill=(20,20,20)); cells.append(c)
B=Image.new('RGB',(3*680+40,2*734+90),(255,255,255))
for i,c in enumerate(cells): B.paste(c,(10+(i%3)*690,60+(i//3)*744))
d=ImageDraw.Draw(B); d.text((12,14),'Heaven sky island - natural block palettes A-E (identical geometry, camera, light, vegetation)',font=F(24,True),fill=(10,10,10))
d.text((12,B.height-26),TAG,font=F(15),fill=(60,60,60))
B.save(f'{OUT}/comparison_board.png',optimize=True)
