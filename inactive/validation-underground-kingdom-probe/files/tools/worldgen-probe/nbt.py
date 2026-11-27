"""Minimal NBT reader/writer (gzip, big-endian) for structure templates. No third-party deps."""
import gzip
import struct

END, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, BYTE_ARRAY, STRING, LIST, COMPOUND, INT_ARRAY, LONG_ARRAY = range(13)


class Tag:
    """A typed value: lets the writer distinguish ints/longs/bytes that Python merges."""

    def __init__(self, tid, value, elem=None):
        self.tid, self.value, self.elem = tid, value, elem

    def __repr__(self):
        return f"Tag({self.tid},{self.value!r})"


def byte(v): return Tag(BYTE, v)
def integer(v): return Tag(INT, v)
def string(v): return Tag(STRING, v)
def compound(d): return Tag(COMPOUND, d)
def lst(elem, items): return Tag(LIST, items, elem)


def _w_str(out, s):
    b = s.encode("utf-8")
    out += struct.pack(">H", len(b)) + b


def _w_payload(out, tag):
    t, v = tag.tid, tag.value
    if t == BYTE: out += struct.pack(">b", v)
    elif t == SHORT: out += struct.pack(">h", v)
    elif t == INT: out += struct.pack(">i", v)
    elif t == LONG: out += struct.pack(">q", v)
    elif t == FLOAT: out += struct.pack(">f", v)
    elif t == DOUBLE: out += struct.pack(">d", v)
    elif t == STRING: _w_str(out, v)
    elif t == LIST:
        out += struct.pack(">bi", tag.elem, len(v))
        for item in v: _w_payload(out, item)
    elif t == COMPOUND:
        for k, child in v.items():
            out += struct.pack(">b", child.tid)
            _w_str(out, k)
            _w_payload(out, child)
        out += b"\x00"
    elif t == INT_ARRAY:
        out += struct.pack(">i", len(v)) + b"".join(struct.pack(">i", x) for x in v)
    else:
        raise ValueError(f"unsupported tag {t}")


def dumps(root: Tag) -> bytes:
    out = bytearray()
    out += struct.pack(">b", COMPOUND)
    _w_str(out, "")
    _w_payload(out, root)
    return gzip.compress(bytes(out), mtime=0)  # mtime=0 -> byte-reproducible output


class _R:
    def __init__(self, b): self.b, self.i = b, 0
    def take(self, n):
        r = self.b[self.i:self.i + n]; self.i += n; return r
    def s(self):
        n = struct.unpack(">H", self.take(2))[0]; return self.take(n).decode("utf-8")


def _r_payload(r, t, elem=None):
    if t == BYTE: return Tag(t, struct.unpack(">b", r.take(1))[0])
    if t == SHORT: return Tag(t, struct.unpack(">h", r.take(2))[0])
    if t == INT: return Tag(t, struct.unpack(">i", r.take(4))[0])
    if t == LONG: return Tag(t, struct.unpack(">q", r.take(8))[0])
    if t == FLOAT: return Tag(t, struct.unpack(">f", r.take(4))[0])
    if t == DOUBLE: return Tag(t, struct.unpack(">d", r.take(8))[0])
    if t == STRING: return Tag(t, r.s())
    if t == BYTE_ARRAY:
        n = struct.unpack(">i", r.take(4))[0]; return Tag(t, r.take(n))
    if t == INT_ARRAY:
        n = struct.unpack(">i", r.take(4))[0]
        return Tag(t, [struct.unpack(">i", r.take(4))[0] for _ in range(n)])
    if t == LONG_ARRAY:
        n = struct.unpack(">i", r.take(4))[0]
        return Tag(t, [struct.unpack(">q", r.take(8))[0] for _ in range(n)])
    if t == LIST:
        et, n = struct.unpack(">bi", r.take(5))
        return Tag(t, [_r_payload(r, et) for _ in range(n)], et)
    if t == COMPOUND:
        d = {}
        while True:
            ct = struct.unpack(">b", r.take(1))[0]
            if ct == END: break
            k = r.s(); d[k] = _r_payload(r, ct)
        return Tag(t, d)
    raise ValueError(f"bad tag {t}")


def loads(data: bytes) -> Tag:
    r = _R(gzip.decompress(data))
    t = struct.unpack(">b", r.take(1))[0]; r.s()
    return _r_payload(r, t)


def to_py(tag):
    if tag.tid == COMPOUND: return {k: to_py(v) for k, v in tag.value.items()}
    if tag.tid == LIST: return [to_py(v) for v in tag.value]
    return tag.value
