package dev.itzgamesgd.moreminecrafts.progression;

import java.util.List;

/** Seven-sector navigation geometry from the supplied reference layout; it contains no game data. */
public final class ProgressionSectorGeometry {
    public static final int REFERENCE_WIDTH = 640;
    public static final int REFERENCE_HEIGHT = 360;
    private static final List<Sector> SECTORS = List.of(
            sector("resources", "minecraft:iron_pickaxe", 302,139, 270,73, 370,73, 338,139),
            sector("production", "minecraft:crafting_table", 338,139, 370,73, 432,151, 361,168),
            sector("living", "minecraft:wheat", 361,168, 432,151, 410,249, 353,203),
            sector("combat", "minecraft:iron_sword", 353,203, 410,249, 320,292, 320,219),
            sector("ecology", "minecraft:oak_sapling", 320,219, 320,292, 230,249, 287,203),
            sector("world", "minecraft:compass", 287,203, 230,249, 208,151, 279,168),
            sector("alchemy", "minecraft:brewing_stand", 279,168, 208,151, 270,73, 302,139));

    private ProgressionSectorGeometry() { }

    private static Sector sector(String discipline, String iconItemId, double... coordinates) {
        var points = new java.util.ArrayList<Point>(coordinates.length / 2);
        for (int index = 0; index < coordinates.length; index += 2)
            points.add(new Point(coordinates[index], coordinates[index + 1]));
        return new Sector(discipline, "moreminecrafts.discipline." + discipline, iconItemId, List.copyOf(points));
    }

    public static List<Sector> sectors() { return SECTORS; }

    /** Returns the first matching sector index, or -1 for a point outside all click regions. */
    public static int hitTest(double screenX, double screenY, double originX, double originY, double scale) {
        Sector sector = sectorAt(screenX, screenY, originX, originY, scale);
        return sector == null ? -1 : SECTORS.indexOf(sector);
    }

    public static Sector sectorAt(double screenX, double screenY, double originX, double originY, double scale) {
        if (scale <= 0) return null;
        double x = (screenX - originX) / scale;
        double y = (screenY - originY) / scale;
        for (Sector sector : SECTORS) if (sector.contains(x, y)) return sector;
        return null;
    }

    public record Point(double x, double y) { }

    public record Sector(String discipline, String titleKey, String iconItemId, List<Point> polygon) {
        public Point centroid() {
            double x = 0, y = 0;
            for (Point point : polygon) { x += point.x(); y += point.y(); }
            return new Point(x / polygon.size(), y / polygon.size());
        }

        public boolean contains(double x, double y) {
            boolean inside = false;
            for (int current = 0, previous = polygon.size() - 1; current < polygon.size(); previous = current++) {
                Point a = polygon.get(current), b = polygon.get(previous);
                if ((a.y() > y) != (b.y() > y)
                        && x < (b.x() - a.x()) * (y - a.y()) / (b.y() - a.y()) + a.x())
                    inside = !inside;
            }
            return inside;
        }
    }
}
