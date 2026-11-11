#!/usr/bin/env python3
"""Frozen 03B boundary audit; no structure generation. Python stdlib only."""
import csv, hashlib, json, math
from pathlib import Path

HERE=Path(__file__).resolve().parent
SOURCE=HERE.parent/'sky_tower_geometry_03b/data/best/blocks_C1.csv'
EXPECTED='b6748c4023d794dae01d729ea95af870e7b63316'

def center_inside(x,z,d):
    return 4*(x*x+z*z)<d*d

def corner_inside(x,z,d):
    return (2*abs(x)+1)**2+(2*abs(z)+1)**2<=d*d

def main():
    raw=SOURCE.read_bytes();sha=hashlib.sha1(b'blob '+str(len(raw)).encode()+b'\0'+raw).hexdigest()
    assert sha==EXPECTED,(sha,EXPECTED)
    cells={}
    with SOURCE.open() as f:
        for r in csv.DictReader(f):
            x,y,z=map(int,(r['x'],r['y'],r['z']));cells.setdefault((x,z),{}).setdefault(r['group'],set()).add(y)
    normal={};balcony={}
    for (x,z),groups in sorted(cells.items()):
        for name,ys in sorted(groups.items()):
            local=name.startswith('balcony_');d=51 if local else 45
            record={'x':x,'z':z,'group':name,'y':sorted(ys),'diameter':d,
                'center_distance':math.hypot(x,z),'center_excess':max(0,math.hypot(x,z)-d/2),
                'farthest_corner_distance':math.hypot(abs(x)+.5,abs(z)+.5),
                'center_inside':center_inside(x,z,d),'whole_block_inside':corner_inside(x,z,d)}
            (balcony if local else normal).setdefault((x,z),[]).append(record)
    # Independent set-membership enumeration matching the original GEOMETRY-02 disc.
    disc45={(x,z) for x in range(-30,31) for z in range(-30,31) if math.hypot(x,z)<22.5}
    disc51={(x,z) for x in range(-30,31) for z in range(-30,31) if math.hypot(x,z)<25.5}
    out1={p for p,v in normal.items() if not v[0]['center_inside']};out2=set(normal)-disc45
    b1={p for p,v in balcony.items() if not v[0]['center_inside']};b2=set(balcony)-disc51
    assert out1==out2 and b1==b2 and len(out1)==259
    whole={p for p,v in normal.items() if not v[0]['whole_block_inside']}
    bwhole={p for p,v in balcony.items() if not v[0]['whole_block_inside']}
    rows=[r for values in normal.values() for r in values if not r['center_inside']]
    with (HERE/'outside_D45.csv').open('w',newline='') as f:
        w=csv.DictWriter(f,fieldnames=list(rows[0]));w.writeheader()
        for r in rows:w.writerow({**r,'y':'|'.join(map(str,r['y']))})
    details={'base_commit':'bf873bdd818070473e25292d2f6c33de521fcf69','source_blob':sha,
      'definition':'block cube [x,x+1)×[z,z+1), tower axis (0.5,0.5); block-center d=hypot(x,z); D disc uses d<D/2',
      'normal_outside_center_columns':sorted(out1),'normal_outside_whole_block_columns':sorted(whole),
      'balcony_outside_D51_center_columns':sorted(b1),'balcony_outside_D51_whole_block_columns':sorted(bwhole),
      'normal_records':[r for values in normal.values() for r in values],
      'balcony_records':[r for values in balcony.values() for r in values],
      'per_group_outside':{g:len({(r['x'],r['z']) for r in rows if r['group']==g}) for g in sorted({r['group'] for r in rows})},
      'method_agreement':True,'center_boundary_is_strict':True,'whole_block_boundary_is_inclusive':True,
      'whole_block_test_is_alternative_not_approved_design':True}
    (HERE/'results.json').write_text(json.dumps(details,ensure_ascii=False,separators=(',',':'),sort_keys=True)+'\n')
    print('normal center outside',len(out1),'whole-block outside',len(whole),'balcony center',len(b1),'balcony whole-block',len(bwhole))
    print('group counts',details['per_group_outside'])

if __name__=='__main__':main()
