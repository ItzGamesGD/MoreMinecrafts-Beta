import numpy as np, collections, shade, palettes as pl, raycast as rc, json
V,N=pl.build()
res={}
for v in ['full','edge','underside']:
    im,blk,face,w=shade.shade('hb',v,'REF',V,N,out_ids=True)
    b=blk.ravel(); f=face.ravel()
    terr=[s for s in pl.SLOTS]
    tot=sum((b==s).sum() for s in terr)
    allhit=(b!='').sum()
    row={}
    for s in terr:
        m=b==s
        row[s]=dict(pct=round(100*m.sum()/tot,1), top=int((m&(f==3)).sum()), side=int((m&np.isin(f,[0,1,4,5])).sum()), bottom=int((m&(f==2)).sum()))
    row['_terrain_px']=int(tot); row['_veg_px']=int(allhit-tot)
    res[v]=row
    print(v, 'terrain px',tot,'nonterrain',allhit-tot)
    for s in terr: print('   %-12s %5.1f%%  top %7d side %7d bottom %7d'%(s,row[s]['pct'],row[s]['top'],row[s]['side'],row[s]['bottom']))
json.dump(res,open('pixstats.json','w'),indent=1)
