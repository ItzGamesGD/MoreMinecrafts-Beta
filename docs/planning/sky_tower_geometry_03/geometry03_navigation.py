"""Independent collision-space BFS. Does not import the route builder.

Quarter-block horizontal samples, 0.6x1.8 player AABB, <=0.5 upward step,
<=1 downward step. No jumping, flying, diagonal clipping or route-index edges.
Coordinates use 1/40 block internally; stair collision boxes are explicit.
This is a conservative geometric witness, not a Minecraft runtime simulation.
"""
from collections import deque
from functools import lru_cache
import math


class Scene:
    def __init__(self, blocks):
        self.columns = {}
        for (x, y, z), (kind, facing) in blocks.items():
            x0, y0, z0 = 40*x, 40*y, 40*z
            if kind == 'stair':
                boxes = [(x0, y0, z0, x0+40, y0+20, z0+40)]
                dx0, dz0, dx1, dz1 = {
                    'east': (20,0,40,40), 'west': (0,0,20,40),
                    'south': (0,20,40,40), 'north': (0,0,40,20)}[facing]
                boxes.append((x0+dx0, y0+20, z0+dz0, x0+dx1, y0+40, z0+dz1))
            else:
                boxes = [(x0,y0,z0,x0+40,y0+40,z0+40)]
            self.columns.setdefault((x,z), []).extend(boxes)

    def boxes(self, xmin, zmin, xmax, zmax):
        for x in range(math.floor(xmin/40), math.ceil(xmax/40)):
            for z in range(math.floor(zmin/40), math.ceil(zmax/40)):
                yield from self.columns.get((x,z), ())

    def clear_box(self, bounds):
        a,b,c,d,e,f = bounds
        return not any(a < X and d > x and b < Y and e > y and c < Z and f > z
                       for x,y,z,X,Y,Z in self.boxes(a,c,d,f))

    @lru_cache(maxsize=500000)
    def surfaces(self, u, v):
        x,z = 10*u+5, 10*v+5
        return tuple(sorted({Y for a,b,c,A,Y,C in self.boxes(x-12,z-12,x+12,z+12)
                             if x-12 < A and x+12 > a and z-12 < C and z+12 > c}))

    @lru_cache(maxsize=500000)
    def stand(self, u, v, h):
        x,z = 10*u+5, 10*v+5
        return h in self.surfaces(u,v) and self.clear_box((x-12,h,z-12,x+12,h+72,z+12))

    def search(self, start, goals, limit=300000):
        # Public triples: (quarter-grid x index, feet y in blocks, quarter-grid z index).
        initial = (start[0],start[2],round(start[1]*40))
        targets = [(g[0],g[2],round(g[1]*40)) for g in goals]
        wanted = set(targets)
        parent = {initial: None}
        queue = deque([initial]) if self.stand(*initial) else deque()
        reached = {initial} & wanted if queue else set()
        while queue and len(reached) < len(wanted) and len(parent) < limit:
            u,v,h = queue.popleft()
            for du,dv in ((1,0),(-1,0),(0,1),(0,-1)):
                U,V = u+du,v+dv
                for H in self.surfaces(U,V):
                    nxt = (U,V,H)
                    if nxt in parent or not h-40 <= H <= h+20 or not self.stand(U,V,H):
                        continue
                    x,z,X,Z = 10*u+5,10*v+5,10*U+5,10*V+5
                    topfeet = max(h,H)
                    # Swept horizontal body after lifting (or before lowering).
                    if not self.clear_box((min(x,X)-12,topfeet,min(z,Z)-12,
                                           max(x,X)+12,topfeet+72,max(z,Z)+12)):
                        continue
                    if H > h and not self.clear_box((x-12,h,z-12,x+12,H+72,z+12)):
                        continue
                    parent[nxt] = (u,v,h)
                    reached.update({nxt} & wanted)
                    queue.append(nxt)
        witnesses = []
        for target in targets:
            path = []
            if target in reached:
                cur = target
                while cur is not None:
                    u,v,h = cur
                    path.append([u/4+0.125,h/40,v/4+0.125])
                    cur = parent[cur]
                path.reverse()
            witnesses.append(path)
        result = {'start_standable': self.stand(*initial), 'reached': [t in reached for t in targets],
                  'visited_states': len(parent), 'limit': limit, 'search_exhausted': not queue,
                  'limit_hit': len(parent) >= limit, 'witness_paths': witnesses,
                  'player_width': 0.6, 'player_height': 1.8, 'grid_step': 0.25,
                  'max_step_up': 0.5, 'jump_allowed': False}
        self.surfaces.cache_clear()
        self.stand.cache_clear()
        return result
