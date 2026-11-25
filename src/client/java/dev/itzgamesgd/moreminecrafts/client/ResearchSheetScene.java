package dev.itzgamesgd.moreminecrafts.client;

import com.google.gson.Gson;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Owner-approved sheet presentation for one research scene (2026-11-25 pilot: Research / Resources).
 * Positions come from {@code assets/moreminecrafts/progression/research_sheets/<discipline>.json} in sheet pixels
 * and are exposed here in GUI units (world units of the tree canvas). Scenes without a sheet file keep the
 * existing presentation.
 */
final class ResearchSheetScene {
    static final String KIT = "textures/gui/research_tree/";

    private record Banner(int x, int y, int w, int h) {}
    private record Zone(String branch, int y0, int y1, Banner banner) {}
    private record Slot(String id, int x, int y) {}
    private record Edge(String from, String to, int[][] route) {}
    private record Size(int w, int h) {}
    private record File(int schemaVersion, String screen, String discipline, String texture, int artPxPerGuiPx,
                        Size sheet, List<Zone> zones, List<Slot> slots, List<Edge> edges) {}

    record BannerBox(String branch, int x, int y, int width, int height) {}
    record Route(String from, String to, int[][] points) {}

    private final Identifier texture;
    private final int artWidth;
    private final int artHeight;
    private final int width;
    private final int height;
    private final Map<String, int[]> slots = new LinkedHashMap<>();
    private final List<BannerBox> banners;
    private final List<Route> routes;

    private ResearchSheetScene(File file) {
        int s = file.artPxPerGuiPx();
        if (s <= 0) throw new IllegalArgumentException("artPxPerGuiPx must be positive");
        texture = Identifier.parse(file.texture());
        artWidth = file.sheet().w();
        artHeight = file.sheet().h();
        width = artWidth / s;
        height = artHeight / s;
        for (Slot slot : file.slots()) slots.put(slot.id(), new int[]{slot.x() / s, slot.y() / s});
        banners = file.zones().stream().map(z -> new BannerBox(z.branch(), z.banner().x() / s, z.banner().y() / s,
                z.banner().w() / s, z.banner().h() / s)).toList();
        routes = file.edges().stream().map(e -> {
            int[][] pts = new int[e.route().length][];
            for (int i = 0; i < pts.length; i++) pts[i] = new int[]{e.route()[i][0] / s, e.route()[i][1] / s};
            return new Route(e.from(), e.to(), pts);
        }).toList();
    }

    /** The sheet for a research discipline, or null when that scene has no approved sheet yet. */
    static ResearchSheetScene load(String screen, String discipline) {
        if (!"research".equals(screen)) return null;
        var stream = ResearchSheetScene.class.getResourceAsStream(
                "/assets/moreminecrafts/progression/research_sheets/" + discipline + ".json");
        if (stream == null) return null;
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            File file = new Gson().fromJson(reader, File.class);
            if (file == null || !"research".equals(file.screen()) || !discipline.equals(file.discipline())) return null;
            return new ResearchSheetScene(file);
        } catch (Exception e) {
            dev.itzgamesgd.moreminecrafts.MoreMinecrafts.LOGGER.error("Invalid research sheet for {}", discipline, e);
            return null;
        }
    }

    static Identifier kit(String name) { return ModIds.id(KIT + name + ".png"); }

    Identifier texture() { return texture; }
    int artWidth() { return artWidth; }
    int artHeight() { return artHeight; }
    int width() { return width; }
    int height() { return height; }
    int[] slot(String id) { return slots.get(id); }
    boolean covers(java.util.Collection<String> ids) { return slots.keySet().containsAll(ids); }
    List<BannerBox> banners() { return banners; }
    List<Route> routes() { return routes; }

    /**
     * Draws a horizontally three-sliced or fully nine-sliced kit texture at a destination rectangle.
     * {@code cap} is the slice size in art pixels; edges and centre are stretched (the kit's centres are flat).
     */
    static void nineSlice(GuiGraphicsExtractor g, Identifier tex, int texW, int texH, int capArt, int capGui,
                          int x, int y, int w, int h, boolean vertical) {
        if (w <= 0 || h <= 0) return;
        int cx = Math.min(capGui, w / 2);
        int cy = vertical ? Math.min(capGui, h / 2) : 0;
        int capV = vertical ? capArt : 0;
        int[][] cols = {{0, capArt, x, cx}, {capArt, texW - 2 * capArt, x + cx, w - 2 * cx}, {texW - capArt, capArt, x + w - cx, cx}};
        int[][] rows = vertical
                ? new int[][]{{0, capV, y, cy}, {capV, texH - 2 * capV, y + cy, h - 2 * cy}, {texH - capV, capV, y + h - cy, cy}}
                : new int[][]{{0, texH, y, h}};
        for (int[] c : cols) {
            if (c[3] <= 0 || c[1] <= 0) continue;
            for (int[] r : rows) {
                if (r[3] <= 0 || r[1] <= 0) continue;
                g.blit(RenderPipelines.GUI_TEXTURED, tex, c[2], r[2], c[0], r[0], c[3], r[3], c[1], r[1], texW, texH);
            }
        }
    }
}
