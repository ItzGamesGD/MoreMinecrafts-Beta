"""Geometric intersection with known structure boxes; not a protection check."""
def intersects(point, radius, box):
    return sum((max(box[i]-point[i],0,point[i]-box[i+3]))**2 for i in range(3)) <= radius**2


def expand_box(box, buffer):
    return tuple(box[i] - buffer if i < 3 else box[i] + buffer for i in range(6))
