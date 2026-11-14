"""Exploratory voxel analysis; no production generation or gameplay thresholds."""
import numpy as np
from scipy import ndimage


def components(air):
    """Label six-connected traversable air; fluids are excluded by caller."""
    return ndimage.label(air, structure=ndimage.generate_binary_structure(3, 1))


def clearance(air):
    """Euclidean distance in blocks to solid or an unknown sample boundary."""
    return ndimage.distance_transform_edt(np.pad(air, 1, constant_values=False))[1:-1, 1:-1, 1:-1]


def boundary_component_ids(labels):
    values = np.unique(np.concatenate([labels[0,:,:].ravel(), labels[-1,:,:].ravel(), labels[:,0,:].ravel(), labels[:,-1,:].ravel(), labels[:,:,0].ravel(), labels[:,:,-1].ravel()]))
    return set(map(int, values)) - {0}


def core_summary(air, radius=10):
    """Separate clearance cores, retaining their enclosing six-connected air IDs."""
    labels, n_air = components(air)
    boundary = boundary_component_ids(labels)
    distance = clearance(air)
    cores, n_cores = components(distance >= radius)
    found = []
    for core_id, slices in enumerate(ndimage.find_objects(cores), 1):
        if slices is None:
            continue
        local = cores[slices] == core_id
        loc = np.unravel_index(np.argmax(np.where(local, distance[slices], -1)), local.shape)
        point = tuple(int(loc[i] + slices[i].start) for i in range(3))
        found.append({'component': int(labels[point]), 'point': point,
                      'diameter_blocks': float(2 * distance[point]), 'component_touches_boundary': int(labels[point]) in boundary})
    return {'air_components': n_air, 'boundary_air_components': len(boundary), 'core_count': n_cores, 'cores': found}

if __name__ == '__main__':
    import gzip, json, sys
    from pathlib import Path
    p = Path(sys.argv[1]); meta = json.loads(p.read_text()); n = meta['side_blocks']
    with gzip.open(str(p)+'.vox.gz', 'rb') as f:
        cells = np.frombuffer(f.read(), dtype=np.uint8).reshape((n,n,97))
    air = cells == 1
    data = core_summary(air)
    # 1.4 x 0.9 vanilla spider dimensions, multiplied by 2.3: 3.22 x 2.07.
    # 4 x 4 x 3 occupied cells is a conservative grid approximation only.
    fit = ndimage.binary_erosion(air, structure=np.ones((4,4,3), dtype=bool), border_value=0)
    paths, n_paths = components(fit)
    for core in data['cores']:
        point = tuple(core['point'])
        core['body_path_component'] = int(paths[point])
        core['point'] = [point[0], point[1], point[2]-48]
    data.update({'seed': meta['seed'], 'side_blocks': n, 'fluid_cells': int(np.count_nonzero(cells == 2)),
                 'body_path_components': n_paths, 'body_dimensions_blocks': [3.22,2.07],
                 'caveats': 'clearance cores are proxies, not qualified chambers or colonies; window boundaries truncate caves; no structure buffer, foliage protection, pathfinding or inter-core bottleneck certification'})
    Path(str(p)+'.analysis.json').write_text(json.dumps(data,indent=2))
