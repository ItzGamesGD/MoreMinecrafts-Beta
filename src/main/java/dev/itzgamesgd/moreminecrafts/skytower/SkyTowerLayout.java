package dev.itzgamesgd.moreminecrafts.skytower;

/** Immutable anchor map of approved 16ad9d5 NBT. Origin means unrotated template minimum corner;
 * rotations are quarter turns about template (0,0,0), with no mirror, matching template transform.
 */
public final class SkyTowerLayout {
    private SkyTowerLayout() {}
    public static final String TEMPLATE = "moreminecrafts:sky_tower/full_01";
    public static final String SHA256 = "cec5bced051fdddcf5ebe80ed267ea3c5749126559ef982f82ac1b4dd435cd27";
    private static final int[] FLOORS = {10,21,33,47,64};
    public record Point(int x, int y, int z) {}
    public record Site(String dimension, int x, int y, int z, int rotation) {
        public Site {
            if (dimension == null || !dimension.matches("[a-z0-9_.-]+:[a-z0-9/._-]+"))
                throw new IllegalArgumentException("invalid dimension identifier");
            if (rotation < 0 || rotation > 3) throw new IllegalArgumentException("invalid rotation");
        }
        public String id() { return dimension + "|" + x + "," + y + "," + z + "|" + rotation; }
        public Point at(int localX, int localY, int localZ) {
            int X=localX,Z=localZ;
            for(int i=0;i<rotation;i++){int old=X;X=-Z;Z=old;}
            return new Point(Math.addExact(x,X),Math.addExact(y,localY),Math.addExact(z,Z));
        }
        public Point spawner(int index) {
            if(index<0 || index>=16)throw new IllegalArgumentException("invalid spawner");
            int q=index%4;
            return at(q<2?17:31,FLOORS[index/4]+2,q%2==0?17:31);
        }
        public Point vault(int index) {
            if(index<0 || index>=8)throw new IllegalArgumentException("invalid vault");
            return at(16,FLOORS[index/2]+1,index%2==0?15:33);
        }
        public int spawnerIndex(Point point) {
            for(int i=0;i<16;i++)if(spawner(i).equals(point))return i;
            return -1;
        }
        public Point floorEntry(int floor) {
            if(floor<0 || floor>4)throw new IllegalArgumentException("invalid floor");
            return at(38,FLOORS[floor]+1,24);
        }
        public boolean overlaps(Site other) {
            if(!dimension.equals(other.dimension))return false;
            int[] a=bounds(),b=other.bounds();
            return a[0]<=b[3] && a[3]>=b[0] && a[1]<=b[4] && a[4]>=b[1] && a[2]<=b[5] && a[5]>=b[2];
        }
        public int[] bounds() {
            Point a=at(0,0,0),b=at(49,79,48);
            return new int[]{Math.min(a.x,b.x),y,Math.min(a.z,b.z),Math.max(a.x,b.x),Math.addExact(y,79),Math.max(a.z,b.z)};
        }
    }
}
