package dev.itzgamesgd.moreminecrafts.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

/** The approved static selector image with hitboxes transformed from its source coordinates. */
public final class ProgressionSelectionScreen extends Screen {
    private static final Identifier PANEL = ModIds.id("textures/gui/selector/approved_full_panel.png");
    private static final Identifier XP_QUEST_CARD = ModIds.id("textures/gui/selector/xp_quest_card.png");
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
    private static final int XP_TARGET = 3;
    // Text boxes inside the 296x494 card texture (card-local source pixels).
    private static final int TITLE_CENTER_Y = 295;
    private static final int TITLE_MAX_WIDTH = 236;
    private static final int TITLE_MAX_HEIGHT = 50;
    private static final int DESC_TOP = 346;
    private static final int DESC_HEIGHT = 100;
    private static final int DESC_MAX_WIDTH = 244;
    private static volatile String lastClickedTarget;
    private int hoveredTarget = -1;
    private int focusedTarget = -1;
    private int forcedHoverTarget = -1;
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
        drawQuestCard(graphics);
        hoveredTarget = forcedHoverTarget >= 0 ? forcedHoverTarget : targetAt(mouseX, mouseY);
        for (int i = 0; i < TARGETS.length; i++) {
            if (TARGETS[i].enabled()) drawInteractionState(graphics, TARGETS[i], i == hoveredTarget, i == focusedTarget);
        }
        graphics.pose().popMatrix();
        drawQuestText(graphics);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    /** Texture-backed XP Quest card; all text is drawn natively so it follows the language setting. */
    private void drawQuestCard(GuiGraphicsExtractor graphics) {
        Target card = TARGETS[XP_TARGET];
        graphics.blit(RenderPipelines.GUI_TEXTURED, XP_QUEST_CARD, card.x(), card.y(), 0, 0,
                card.width(), card.height(), card.width(), card.height());
    }

    /**
     * Card text is laid out in GUI pixels (not the scaled source space) with integer font scales,
     * so glyphs stay crisp at every GUI scale. Scales adapt to the card's current on-screen size.
     */
    private void drawQuestText(GuiGraphicsExtractor graphics) {
        Target card = TARGETS[XP_TARGET];
        float centerX = imageLeft + (card.x() + card.width() / 2.0F) * imageScale;
        Component title = Component.translatable("moreminecrafts.ui.selector.quests");
        Component description = Component.translatable("moreminecrafts.ui.selector.quests.description");
        TextLayout layout = layoutQuestText(title, description);
        drawScaledText(graphics, title.getVisualOrderText(), centerX,
                imageTop + (card.y() + TITLE_CENTER_Y) * imageScale - font.lineHeight * layout.titleScale() / 2.0F,
                layout.titleScale());
        float lineHeight = (font.lineHeight + 2) * layout.descScale();
        float boxHeight = DESC_HEIGHT * imageScale;
        float y = imageTop + (card.y() + DESC_TOP) * imageScale + (boxHeight - layout.lines().size() * lineHeight) / 2.0F;
        for (FormattedCharSequence line : layout.lines()) {
            drawScaledText(graphics, line, centerX, y, layout.descScale());
            y += lineHeight;
        }
    }

    private record TextLayout(int titleScale, int descScale, List<FormattedCharSequence> lines, boolean fits) { }

    private TextLayout layoutQuestText(Component title, Component description) {
        float titleMaxWidth = TITLE_MAX_WIDTH * imageScale;
        float titleMaxHeight = TITLE_MAX_HEIGHT * imageScale;
        int titleScale = 1;
        for (int candidate = 4; candidate > 1; candidate--) {
            if (font.width(title) * candidate <= titleMaxWidth && font.lineHeight * candidate <= titleMaxHeight) {
                titleScale = candidate;
                break;
            }
        }
        float boxWidth = DESC_MAX_WIDTH * imageScale;
        float boxHeight = DESC_HEIGHT * imageScale;
        int scale = 1;
        List<FormattedCharSequence> lines = font.split(description, Math.max(1, (int) boxWidth));
        for (int candidate = 3; candidate > 1; candidate--) {
            List<FormattedCharSequence> trial = font.split(description, Math.max(1, (int) (boxWidth / candidate)));
            if (trial.size() * (font.lineHeight + 2) * candidate <= boxHeight) {
                scale = candidate;
                lines = trial;
                break;
            }
        }
        boolean fits = font.width(title) * titleScale <= titleMaxWidth
                && lines.size() * (font.lineHeight + 2) * scale <= boxHeight;
        for (FormattedCharSequence line : lines) fits &= font.width(line) * scale <= boxWidth;
        return new TextLayout(titleScale, scale, lines, fits);
    }

    /** True when the given title/description fit the card's text boxes at the current GUI scale. */
    public boolean validationQuestTextFits(Component title, Component description) {
        return layoutQuestText(title, description).fits();
    }

    private void drawScaledText(GuiGraphicsExtractor graphics, FormattedCharSequence text, float centerX, float top, int scale) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(Math.round(centerX - font.width(text) * scale / 2.0F), Math.round(top));
        graphics.pose().scale(scale, scale);
        graphics.text(font, text, 0, 0, 0xFFFFFFFF, true);
        graphics.pose().popMatrix();
    }

    /** Hover: warm tint + gold inner line. Keyboard focus: white outer outline. They never share a look. */
    private static void drawInteractionState(GuiGraphicsExtractor graphics, Target target, boolean hovered, boolean focused) {
        int x = target.x(), y = target.y(), w = target.width(), h = target.height();
        if (hovered) {
            graphics.fill(x, y, x + w, y + h, 0x1FFFE08A);
            graphics.outline(x + 5, y + 5, w - 10, h - 10, 0xFFFFD75E);
            graphics.outline(x + 6, y + 6, w - 12, h - 12, 0x80FFD75E);
        }
        if (focused) {
            graphics.outline(x - 4, y - 4, w + 8, h + 8, 0xFFFFFFFF);
            graphics.outline(x - 3, y - 3, w + 6, h + 6, 0xFFFFFFFF);
        }
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
        int index = targetAt(event.x(), event.y());
        if (index >= 0) {
            activate(TARGETS[index]);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void activate(Target target) {
        lastClickedTarget = target.id();
        String screenBefore = screenName();
        logDispatch(target, screenBefore, 0, 0);
        if (target.enabled()) {
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
        }
        if ("1".equals(System.getenv("MOREMINECRAFTS_VISUAL_SELECTOR_PHYSICAL"))) {
            dev.itzgamesgd.moreminecrafts.MoreMinecrafts.LOGGER.info(
                    "SELECTOR_SCREEN_TRANSITION target={} from={} to={} consumed=true",
                    target.id(), screenBefore, screenName());
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int count = TARGETS.length;
        if (event.input() == InputConstants.KEY_TAB) {
            focusedTarget = focusedTarget < 0 ? (event.hasShiftDown() ? count - 1 : 0)
                    : Math.floorMod(focusedTarget + (event.hasShiftDown() ? -1 : 1), count);
            return true;
        }
        if (event.isLeft() || event.isRight()) {
            int cards = count - 1;
            focusedTarget = focusedTarget < 0 || focusedTarget >= cards ? 0
                    : Math.floorMod(focusedTarget + (event.isRight() ? 1 : -1), cards);
            return true;
        }
        if (event.isUp() || event.isDown()) {
            focusedTarget = event.isDown() ? count - 1 : (focusedTarget == count - 1 ? 0 : Math.max(focusedTarget, 0));
            return true;
        }
        if ((event.isConfirmation() || event.isSelection()) && focusedTarget >= 0) {
            activate(TARGETS[focusedTarget]);
            return true;
        }
        return super.keyPressed(event);
    }

    private int targetAt(double mouseX, double mouseY) {
        for (int i = 0; i < TARGETS.length; i++) if (contains(TARGETS[i], mouseX, mouseY)) return i;
        return -1;
    }

    /** Validation hooks: force a hover/focus state without a physical pointer (-1 clears). */
    public void validationSetHover(String targetId) { forcedHoverTarget = targetId == null ? -1 : indexOf(targetId); }
    public void validationSetFocus(String targetId) { focusedTarget = indexOf(targetId); }
    public boolean validationKey(int key, boolean shift) {
        return keyPressed(new KeyEvent(key, 0, shift ? 1 : 0));
    }
    public String validationFocusedTarget() { return focusedTarget < 0 ? "none" : TARGETS[focusedTarget].id(); }

    /** Every card's inner corners hit that card; points just outside its edges hit nothing. */
    public boolean validationHitboxesAligned() {
        for (int i = 0; i < TARGETS.length; i++) {
            Target t = TARGETS[i];
            double[][] inside = {{t.x() + 1, t.y() + 1}, {t.x() + t.width() - 2, t.y() + t.height() - 2},
                    {t.x() + t.width() / 2.0, t.y() + t.height() / 2.0}};
            for (double[] point : inside) {
                if (targetAt(imageLeft + point[0] * imageScale, imageTop + point[1] * imageScale) != i) return false;
            }
            if (targetAt(imageLeft + (t.x() - 2) * imageScale, imageTop + (t.y() + t.height() / 2.0) * imageScale) == i) return false;
            if (targetAt(imageLeft + (t.x() + t.width() + 1) * imageScale, imageTop + (t.y() + t.height() / 2.0) * imageScale) == i) return false;
        }
        return true;
    }

    private static int indexOf(String id) {
        for (int i = 0; i < TARGETS.length; i++) if (TARGETS[i].id().equals(id)) return i;
        return -1;
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
