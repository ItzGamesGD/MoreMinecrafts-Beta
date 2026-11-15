"""Discrete conservative isotropic clearance of a widest 6-connected path."""
from analysis import clearance, components


def widest_clearance(air, points, thresholds):
    dist=clearance(air)
    result={(i,j):0 for i in range(len(points)) for j in range(i+1,len(points))}
    for radius in sorted(thresholds):
        labels,_=components(dist >= radius)
        values=[int(labels[p]) for p in points]
        for pair in result:
            a,b=pair
            if values[a] and values[a] == values[b]: result[pair]=radius
    return result

if __name__ == '__main__':
    import gzip, json, sys
    from pathlib import Path
    import numpy as np
    from dedup import merge_overlapping_cores
    p=Path(sys.argv[1]); meta=json.loads(p.read_text()); n=meta['side_blocks']
    data=json.loads(Path(str(p)+'.analysis.json').read_text())
    cores=merge_overlapping_cores(data['cores'])
    with gzip.open(str(p)+'.vox.gz', 'rb') as f:
        air=np.frombuffer(f.read(),dtype=np.uint8).reshape((n,n,97)) == 1
    points=[(c['point'][0],c['point'][1],c['point'][2]+48) for c in cores]
    bottlenecks=widest_clearance(air,points,range(1,11))
    result=[{'a':i,'b':j,'sphere_clearance_diameter_at_least':2*r}
            for (i,j),r in bottlenecks.items() if r]
    Path(str(p)+'.bottleneck.json').write_text(json.dumps({'cores':cores,'paths':result,
        'caveat':'discrete spherical clearance is not physical corridor width or mob pathfinding; values capped at diameter 20; sample boundary treated as blocked'},indent=2))
