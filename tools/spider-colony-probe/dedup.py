"""Deduplicate overlapping clearance balls; still only a chamber proxy."""
import math


def merge_overlapping_cores(cores):
    parent=list(range(len(cores)))
    def root(i):
        while parent[i] != i:
            parent[i]=parent[parent[i]]
            i=parent[i]
        return i
    for i, a in enumerate(cores):
        for j in range(i):
            b=cores[j]
            if a['component'] != b['component']:
                continue
            distance=math.dist(a['point'], b['point'])
            if distance <= (a['diameter_blocks']+b['diameter_blocks'])/2:
                parent[root(i)]=root(j)
    groups={}
    for i, c in enumerate(cores): groups.setdefault(root(i),[]).append(c)
    return [max(group, key=lambda x:x['diameter_blocks']) for group in groups.values()]
