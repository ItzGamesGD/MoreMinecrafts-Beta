"""Independent exact ray/AABB reproduction of P5 witness; no generator imports."""
import math
angle=math.radians(246.75)
dx,dz=math.cos(angle),math.sin(angle)
def intersect(x,z):
    intervals=[sorted(((x-.5)/dx,(x+.5)/dx)),sorted(((z-.5)/dz,(z+.5)/dz))]
    return max(v[0] for v in intervals),min(v[1] for v in intervals)
wall=intersect(-6,-14)
pillar=intersect(-8,-18)
width=pillar[0]-wall[1]
print(f'wall={wall}, pillar={pillar}, width={width:.15f}')
assert abs(width-3.265159665071046)<1e-12
