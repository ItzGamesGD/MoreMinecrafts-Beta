package dev.itzgamesgd.moreminecrafts.client;

import com.google.gson.Gson;
import dev.itzgamesgd.moreminecrafts.progression.ProgressionCanvasGeometry;
import dev.itzgamesgd.moreminecrafts.progression.ProgressionSectorGeometry;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;

/** Opt-in observation for external physical-input tests. It never changes screens, input, progress or XP. */
final class BatchAClientProbe {
    private static int ticks;
    static void observe(Minecraft client) {
        if (++ticks % 5 != 0) return;
        Map<String, Object> data = new LinkedHashMap<>();
        var screen = client.gui.screen();
        double scale = client.getWindow().getGuiScale();
        data.put("screen", screen == null ? "world" : screen.getClass().getSimpleName());
        data.put("joined", client.player != null && client.level != null);
        data.put("revision", ProgressionClientApi.revision());
        data.put("xp", ProgressionClientApi.availableXp());
        if (screen instanceof ProgressionSelectionScreen selector) {
            Map<String, int[]> cards = new LinkedHashMap<>();
            for (String id : new String[]{"advancements", "knowledge", "research", "xp_quest", "close"})
                cards.put(id, new int[]{selector.validationPhysicalX(id), selector.validationPhysicalY(id)});
            data.put("cards", cards);
        }
        if (screen instanceof ProgressionDisciplineSelectionScreen sectors) {
            data.put("mode", sectors.mode().name());
            double pw = Math.min(screen.width - 16.0, screen.height * (252.0 / 140.0));
            double ph = pw * 140.0 / 252.0, py = (screen.height - ph) / 2.0;
            double s = Math.min(Math.max(1, pw - 48) / 224.0, Math.max(1, ph - 58) / 219.0);
            double ox = screen.width / 2.0 - 320 * s, oy = py + 27 + Math.max(1, ph - 58) / 2.0 - 177 * s;
            Map<String, int[]> points = new LinkedHashMap<>();
            for (var sector : ProgressionSectorGeometry.sectors()) points.put(sector.discipline(), new int[]{
                    (int)Math.round((ox + sector.centroid().x() * s) * scale),
                    (int)Math.round((oy + sector.centroid().y() * s) * scale)});
            data.put("sectors", points);
        }
        if (screen instanceof ProgressionBrowseScreen browse) {
            data.put("mode", browse.mode().name());
            data.put("discipline", browse.validationDiscipline());
            var canvas = browse.validationCanvas();
            if (canvas != null && canvas.viewport() != null) {
                data.put("nodes", canvas.nodes().size());
                data.put("level", browse.validationLevel());
                Map<String, int[]> points = new LinkedHashMap<>();
                for (var node : canvas.nodes()) {
                    double x = ProgressionCanvasGeometry.worldToScreenX(canvas.camera(), canvas.viewport().x(), node.x());
                    double y = ProgressionCanvasGeometry.worldToScreenY(canvas.camera(), canvas.viewport().y(), node.y());
                    if (canvas.viewport().contains(x,y) && node.id().equals(canvas.hitTest(x,y)))
                        points.put(node.id(), new int[]{(int)Math.round(x * scale), (int)Math.round(y * scale)});
                }
                data.put("clickable", points);
                Map<String, int[]> chapters = new LinkedHashMap<>();
                for (var chapter : dev.itzgamesgd.moreminecrafts.progression.QuestPresentationData.registeredTree().chapters()) {
                    int[] xy = browse.validationChapterCardCentre(chapter.id());
                    if (xy != null) chapters.put(chapter.id(), new int[]{(int)Math.round(xy[0]*scale), (int)Math.round(xy[1]*scale)});
                }
                data.put("chapters", chapters);
            }
        }
        data.put("quests", ProgressionClientApi.quests(null));
        try { Files.writeString(client.gameDirectory.toPath().resolve("batch-a-client-probe.json"), new Gson().toJson(data)); }
        catch (java.io.IOException failure) { throw new IllegalStateException("Batch A observation failed", failure); }
    }
}
