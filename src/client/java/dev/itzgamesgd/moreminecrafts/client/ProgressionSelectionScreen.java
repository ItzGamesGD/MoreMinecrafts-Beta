package dev.itzgamesgd.moreminecrafts.client;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** The approved static selector image with hitboxes transformed from its source coordinates. */
public final class ProgressionSelectionScreen extends Screen {
    private static final Identifier PANEL = ModIds.id("textures/gui/selector/approved_full_panel.png");
    private static final int IMAGE_WIDTH = 1326;
    private static final int IMAGE_HEIGHT = 739;
    private static final int PADDING = 8;
    private static final Target[] TARGETS = {
            new Target("advancements", 46, 129, 299, 494, true),
            new Target("knowledge", 361, 129, 295, 494, true),
            new Target("research", 673, 129, 296, 494, true),
            new Target("xp_quest", 985, 129, 296, 494, true),
            new Target("close", 487, 656, 350, 63, true)
    };
    private static volatile String lastClickedTarget;
    private float imageScale = 1.0F;
    private float imageLeft;
    private float imageTop;

    public ProgressionSelectionScreen() {
        super(Component.translatable("moreminecrafts.ui.selector.title"));
        lastClickedTarget = null;
    }

    @Override
    protected void init() {
        imageScale = Math.min((width - PADDING * 2.0F) / IMAGE_WIDTH,
                (height - PADDING * 2.0F) / IMAGE_HEIGHT);
        imageLeft = (width - IMAGE_WIDTH * imageScale) / 2.0F;
        imageTop = (height - IMAGE_HEIGHT * imageScale) / 2.0F;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(imageLeft, imageTop);
        graphics.pose().scale(imageScale, imageScale);
        graphics.blit(RenderPipelines.GUI_TEXTURED, PANEL, 0, 0, 0, 0,
                IMAGE_WIDTH, IMAGE_HEIGHT, IMAGE_WIDTH, IMAGE_HEIGHT);
        drawEnabledQuestCard(graphics);
        graphics.pose().popMatrix();
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void drawEnabledQuestCard(GuiGraphicsExtractor graphics) {
        int left = 985, top = 129, right = 1281, bottom = 623;
        graphics.fill(left, top, right, bottom, 0xFF173326);
        graphics.outline(left, top, right - left, bottom - top, 0xFF86B995);
        graphics.fill(left + 5, top + 5, right - 5, top + 5 + 285, 0xFF202B26);
        graphics.item(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SLIME_BALL), 1117, top + 95);
        graphics.fill(left + 14, top + 282, right - 14, top + 332, 0xFF29463A);
        graphics.outline(left + 14, top + 282, right - left - 28, 50, 0xFFB7D6BE);
        graphics.centeredText(font, Component.translatable("moreminecrafts.ui.selector.quests"),
                (left + right) / 2, top + 299, 0xFFE5F0E7);
        var description = font.split(Component.translatable("moreminecrafts.ui.selector.quests.description"), right - left - 40);
        int y = top + 364;
        for (var line : description) {
            graphics.text(font, line, (left + right - font.width(line)) / 2, y, 0xFFD7E1D9, false);
            y += 15;
        }
        graphics.fill(left + 52, bottom - 54, right - 52, bottom - 23, 0xFF345C45);
        graphics.centeredText(font, Component.translatable("moreminecrafts.ui.selector.active"),
                (left + right) / 2, bottom - 44, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if ("1".equals(System.getenv("MOREMINECRAFTS_VISUAL_SELECTOR_PHYSICAL"))) {
            dev.itzgamesgd.moreminecrafts.MoreMinecrafts.LOGGER.info(
                    "SELECTOR_MOUSE_EVENT x={} y={} button={} scale={} window={}x{}",
                    event.x(), event.y(), event.button(), minecraft.getWindow().getGuiScale(),
                    minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight());
        }
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        for (Target target : TARGETS) {
            if (!contains(target, event.x(), event.y())) continue;
            lastClickedTarget = target.id();
            String screenBefore = screenName();
            if (!target.enabled()) {
                logDispatch(target, screenBefore, event.x(), event.y());
                if ("1".equals(System.getenv("MOREMINECRAFTS_VISUAL_SELECTOR_PHYSICAL"))) {
                    dev.itzgamesgd.moreminecrafts.MoreMinecrafts.LOGGER.info(
                            "SELECTOR_SCREEN_TRANSITION target={} from={} to={} consumed=true",
                            target.id(), screenBefore, screenName());
                }
                return true;
            }
            logDispatch(target, screenBefore, event.x(), event.y());
            switch (target.id()) {
                case "advancements" -> openAdvancements();
                case "knowledge" -> minecraft.gui.setScreen(
                        new ProgressionDisciplineSelectionScreen(ProgressionBrowseScreen.Mode.KNOWLEDGE));
                case "research" -> minecraft.gui.setScreen(
                        new ProgressionDisciplineSelectionScreen(ProgressionBrowseScreen.Mode.RESEARCH));
                case "xp_quest" -> minecraft.gui.setScreen(
                        new ProgressionBrowseScreen(ProgressionBrowseScreen.Mode.QUEST));
                case "close" -> onClose();
                default -> { }
            }
            if ("1".equals(System.getenv("MOREMINECRAFTS_VISUAL_SELECTOR_PHYSICAL"))) {
                dev.itzgamesgd.moreminecrafts.MoreMinecrafts.LOGGER.info(
                        "SELECTOR_SCREEN_TRANSITION target={} from={} to={} consumed=true",
                        target.id(), screenBefore, screenName());
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void logDispatch(Target target, String screenBefore, double mouseX, double mouseY) {
        if ("1".equals(System.getenv("MOREMINECRAFTS_VISUAL_SELECTOR_PHYSICAL"))) {
            dev.itzgamesgd.moreminecrafts.MoreMinecrafts.LOGGER.info(
                    "SELECTOR_INPUT_DISPATCH target={} enabled={} from={} x={} y={} consumed=true",
                    target.id(), target.enabled(), screenBefore, mouseX, mouseY);
        }
    }

    private String screenName() {
        var screen = minecraft == null ? null : minecraft.gui.screen();
        return screen == null ? "none" : screen.getClass().getSimpleName();
    }

    private void openAdvancements() {
        if (minecraft != null && minecraft.player != null) {
            minecraft.gui.setScreen(new AdvancementsScreen(minecraft.player.connection.getAdvancements()));
        }
    }

    private boolean contains(Target target, double mouseX, double mouseY) {
        float left = imageLeft + target.x() * imageScale;
        float top = imageTop + target.y() * imageScale;
        float right = left + target.width() * imageScale;
        float bottom = top + target.height() * imageScale;
        return mouseX >= left && mouseX < right && mouseY >= top && mouseY < bottom;
    }

    /** The validation harness dispatches through Screen.mouseClicked, the same route as mouse input. */
    public boolean validationClick(String targetId) {
        Target target = findTarget(targetId);
        if (target == null) return false;
        double centerX = imageLeft + (target.x() + target.width() / 2.0) * imageScale;
        double centerY = imageTop + (target.y() + target.height() / 2.0) * imageScale;
        return mouseClicked(new MouseButtonEvent(centerX, centerY, new MouseButtonInfo(0, 0)), false);
    }

    /** Root-window pixel coordinates used by the xdotool physical-input validation. */
    public int validationPhysicalX(String targetId) {
        Target target = findTarget(targetId);
        if (target == null || minecraft == null) return -1;
        double logicalX = imageLeft + (target.x() + target.width() / 2.0) * imageScale;
        return (int) Math.round(logicalX * minecraft.getWindow().getGuiScale());
    }

    public int validationPhysicalY(String targetId) {
        Target target = findTarget(targetId);
        if (target == null || minecraft == null) return -1;
        double logicalY = imageTop + (target.y() + target.height() / 2.0) * imageScale;
        return (int) Math.round(logicalY * minecraft.getWindow().getGuiScale());
    }

    public static String lastClickedTarget() {
        return lastClickedTarget;
    }

    private static Target findTarget(String id) {
        for (Target target : TARGETS) if (target.id().equals(id)) return target;
        return null;
    }

    private record Target(String id, int x, int y, int width, int height, boolean enabled) { }
}
