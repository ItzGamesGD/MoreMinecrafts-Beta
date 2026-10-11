package dev.itzgamesgd.moreminecrafts.client;

import dev.itzgamesgd.moreminecrafts.progression.ProgressionSectorGeometry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Navigation-only selector shared by the Research and Knowledge screens. */
public final class ProgressionDisciplineSelectionScreen extends Screen {
    private static final Identifier VANILLA_WINDOW = Identifier.withDefaultNamespace("textures/gui/advancements/window.png");
    private static final Identifier VANILLA_BACKGROUND = Identifier.withDefaultNamespace(
            "textures/gui/advancements/backgrounds/stone.png");
    private static final Identifier LOCKED_BOX = Identifier.withDefaultNamespace("advancements/box_unobtained");
    private static final Identifier LOCKED_FRAME = Identifier.withDefaultNamespace("advancements/task_frame_unobtained");

    private final ProgressionBrowseScreen.Mode mode;
    private double scale;
    private double originX;
    private double originY;
    private int hoveredSector = -1;
    private int focusedSector;
    private Button backButton;

    public ProgressionDisciplineSelectionScreen(ProgressionBrowseScreen.Mode mode) {
        super(Component.translatable(mode == ProgressionBrowseScreen.Mode.KNOWLEDGE
                ? "moreminecrafts.ui.knowledge.title" : "moreminecrafts.ui.research.title"));
        this.mode = mode;
    }

    public ProgressionBrowseScreen.Mode mode() { return mode; }
    public int validationHoveredSector() { return hoveredSector; }

    @Override protected void init() {
        double panelWidth = Math.min(width - 16.0, height * (252.0 / 140.0));
        double panelHeight = panelWidth * (140.0 / 252.0);
        double panelX = (width - panelWidth) / 2.0;
        double panelY = (height - panelHeight) / 2.0;
        double innerWidth = Math.max(1.0, panelWidth - 48.0);
        double innerHeight = Math.max(1.0, panelHeight - 58.0);
        scale = Math.min(innerWidth / 224.0, innerHeight / 219.0);
        double centerX = width / 2.0;
        double centerY = panelY + 27.0 + innerHeight / 2.0;
        originX = centerX - 320.0 * scale;
        originY = centerY - 177.0 * scale;
        backButton = addRenderableWidget(Button.builder(Component.translatable("moreminecrafts.ui.back"), button -> onClose())
                .bounds((int) Math.round(panelX + 13), (int) Math.round(panelY + 10), 70, 20).build());
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        double panelWidth = Math.min(width - 16.0, height * (252.0 / 140.0));
        double panelHeight = panelWidth * (140.0 / 252.0);
        int panelX = (int) Math.round((width - panelWidth) / 2.0);
        int panelY = (int) Math.round((height - panelHeight) / 2.0);
        graphics.fill(0, 0, width, height, 0xFF272727);
        graphics.blit(RenderPipelines.GUI_TEXTURED, VANILLA_WINDOW, panelX, panelY, 0, 0,
                (int) Math.round(panelWidth), (int) Math.round(panelHeight), 252, 140, 256, 256);
        int innerLeft = panelX + 13, innerTop = panelY + 31;
        int innerRight = panelX + (int) Math.round(panelWidth) - 13;
        int innerBottom = panelY + (int) Math.round(panelHeight) - 18;
        tileAdvancementBackground(graphics, innerLeft, innerTop, innerRight, innerBottom);
        graphics.centeredText(font, title, width / 2, panelY + 10, 0xFF181818);

        hoveredSector = ProgressionSectorGeometry.hitTest(mouseX, mouseY, originX, originY, scale);
        for (int index = 0; index < ProgressionSectorGeometry.sectors().size(); index++) {
            var sector = ProgressionSectorGeometry.sectors().get(index);
            int fill = index == hoveredSector || index == focusedSector ? 0xFF55524B : 0xFF333333;
            fillPolygon(graphics, sector.polygon(), originX, originY, scale, fill);
        }
        fillPolygon(graphics, List.of(new ProgressionSectorGeometry.Point(302,139),
                new ProgressionSectorGeometry.Point(338,139), new ProgressionSectorGeometry.Point(361,168),
                new ProgressionSectorGeometry.Point(353,203), new ProgressionSectorGeometry.Point(320,219),
                new ProgressionSectorGeometry.Point(287,203), new ProgressionSectorGeometry.Point(279,168)),
                originX, originY, scale, 0xFF242424);
        for (var sector : ProgressionSectorGeometry.sectors())
            outlinePolygon(graphics, sector.polygon(), originX, originY, scale, 0xFFB4A57D);

        for (int index = 0; index < ProgressionSectorGeometry.sectors().size(); index++) {
            var sector = ProgressionSectorGeometry.sectors().get(index);
            var center = sector.centroid();
            int x = (int) Math.round(originX + center.x() * scale);
            int y = (int) Math.round(originY + center.y() * scale);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, LOCKED_BOX, x - 13, y - 13, 26, 26);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, LOCKED_FRAME, x - 13, y - 13, 26, 26);
            graphics.item(icon(sector.iconItemId()), x - 8, y - 8);
        }

        int active = hoveredSector >= 0 ? hoveredSector : focusedSector;
        var activeSector = ProgressionSectorGeometry.sectors().get(active);
        graphics.centeredText(font, Component.translatable(activeSector.titleKey()), width / 2,
                panelY + (int) Math.round(panelHeight) - 15, 0xFF181818);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void tileAdvancementBackground(GuiGraphicsExtractor graphics, int left, int top, int right, int bottom) {
        for (int y = top; y < bottom; y += 16) for (int x = left; x < right; x += 16) {
            int tileWidth = Math.min(16, right - x), tileHeight = Math.min(16, bottom - y);
            graphics.blit(RenderPipelines.GUI_TEXTURED, VANILLA_BACKGROUND, x, y, 0, 0,
                    tileWidth, tileHeight, 16, 16, 16, 16);
        }
    }

    private static void fillPolygon(GuiGraphicsExtractor graphics, List<ProgressionSectorGeometry.Point> polygon,
                                    double originX, double originY, double scale, int color) {
        int top = (int) Math.floor(polygon.stream().mapToDouble(p -> originY + p.y() * scale).min().orElse(0));
        int bottom = (int) Math.ceil(polygon.stream().mapToDouble(p -> originY + p.y() * scale).max().orElse(0));
        for (int y = top; y < bottom; y++) {
            double designY = (y + 0.5 - originY) / scale;
            List<Double> intersections = new ArrayList<>();
            for (int i = 0; i < polygon.size(); i++) {
                var first = polygon.get(i);
                var second = polygon.get((i + 1) % polygon.size());
                if ((first.y() > designY) == (second.y() > designY)) continue;
                double x = first.x() + (designY - first.y()) * (second.x() - first.x()) / (second.y() - first.y());
                intersections.add(originX + x * scale);
            }
            intersections.sort(Comparator.naturalOrder());
            for (int i = 0; i + 1 < intersections.size(); i += 2) {
                int left = (int) Math.ceil(intersections.get(i));
                int right = (int) Math.floor(intersections.get(i + 1));
                if (right > left) graphics.fill(left, y, right, y + 1, color);
            }
        }
    }

    private static void outlinePolygon(GuiGraphicsExtractor graphics, List<ProgressionSectorGeometry.Point> polygon,
                                       double originX, double originY, double scale, int color) {
        for (int index = 0; index < polygon.size(); index++) {
            var a = polygon.get(index);
            var b = polygon.get((index + 1) % polygon.size());
            int x0 = (int) Math.round(originX + a.x() * scale);
            int y0 = (int) Math.round(originY + a.y() * scale);
            int x1 = (int) Math.round(originX + b.x() * scale);
            int y1 = (int) Math.round(originY + b.y() * scale);
            int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
            for (int step = 0; step <= steps; step++) {
                double progress = steps == 0 ? 0 : step / (double) steps;
                int x = (int) Math.round(x0 + (x1 - x0) * progress);
                int y = (int) Math.round(y0 + (y1 - y0) * progress);
                graphics.fill(x, y, x + 1, y + 1, color);
            }
        }
    }

    private static ItemStack icon(String itemId) {
        var id = Identifier.parse(itemId);
        var item = BuiltInRegistries.ITEM.getValue(id);
        return item == null || item == Items.AIR ? new ItemStack(Items.BOOK) : new ItemStack(item);
    }

    public boolean validationOpenSector(String discipline) {
        for (int index = 0; index < ProgressionSectorGeometry.sectors().size(); index++) {
            if (!ProgressionSectorGeometry.sectors().get(index).discipline().equals(discipline)) continue;
            var center = ProgressionSectorGeometry.sectors().get(index).centroid();
            double x = originX + center.x() * scale;
            double y = originY + center.y() * scale;
            focusedSector = index;
            return mouseClicked(new MouseButtonEvent(x, y, new net.minecraft.client.input.MouseButtonInfo(0, 0)), false);
        }
        return false;
    }

    public boolean validationVisitAllSectors() {
        for (var sector : ProgressionSectorGeometry.sectors()) {
            if (!validationOpenSector(sector.discipline())) return false;
            if (!(minecraft.gui.screen() instanceof ProgressionBrowseScreen browse)
                    || !sector.discipline().equals(browse.validationDiscipline())) return false;
            browse.onClose();
            if (!(minecraft.gui.screen() instanceof ProgressionDisciplineSelectionScreen)) return false;
        }
        return true;
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int index = ProgressionSectorGeometry.hitTest(event.x(), event.y(), originX, originY, scale);
            if (index >= 0) {
                focusedSector = index;
                String discipline = ProgressionSectorGeometry.sectors().get(index).discipline();
                minecraft.gui.setScreen(new ProgressionBrowseScreen(mode, discipline));
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override public boolean keyPressed(KeyEvent event) {
        if (event.isLeft() || event.isUp()) focusedSector = Math.floorMod(focusedSector - 1, 7);
        else if (event.isRight() || event.isDown()) focusedSector = (focusedSector + 1) % 7;
        else if (event.isConfirmation()) {
            String discipline = ProgressionSectorGeometry.sectors().get(focusedSector).discipline();
            minecraft.gui.setScreen(new ProgressionBrowseScreen(mode, discipline));
            return true;
        } else return super.keyPressed(event);
        return true;
    }

    @Override public void onClose() {
        if (minecraft != null) minecraft.gui.setScreen(new ProgressionSelectionScreen());
    }
}
