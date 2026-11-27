package dev.itzgamesgd.moreminecrafts.client;

import dev.itzgamesgd.moreminecrafts.progression.*;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.text.NumberFormat;
import java.util.*;

/** Native-style workstation tabs with compact, topology-derived enchantment nodes. */
public final class EnchantingProgressionScreen extends AbstractContainerScreen<EnchantingProgressionMenu> {
    private static final int GUI_WIDTH = 330;
    private static final int GUI_HEIGHT = 368;
    private static final int TREE_PAGE_ROWS = 6;
    private static final int OPTION_PAGE_ROWS = 4;
    private static final int TREE_TOP = 76;
    private static final int TREE_ROW_STEP = 28;
    private static final int NODE_LEFT = 108;
    private static final int NODE_STEP = 23;

    private int treePage;
    private int optionPage;
    private List<List<EnchantmentTrees.Node>> treePages = List.of();
    private List<EnchantmentTrees.Node> visibleTree = List.of();
    private List<EnchantmentTrees.Node> allOptions = List.of();
    private List<EnchantmentTrees.Node> visibleOptions = List.of();
    private String selectedOption;
    private String selectedEquipment;
    private final EnchantSkinButton[][] nodes = new EnchantSkinButton[TREE_PAGE_ROWS][5];
    private final Button[] optionRows = new Button[OPTION_PAGE_ROWS];
    private Button enchantTab;
    private Button optionTab;
    private Button previousTree;
    private Button nextTree;
    private Button previousOptions;
    private Button nextOptions;
    private Button apply;
    private Button reset;
    private ViewportScale viewportScale = new ViewportScale(1.0D, 0.0D, 0.0D);

    public EnchantingProgressionScreen(EnchantingProgressionMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GUI_WIDTH, GUI_HEIGHT);
        inventoryLabelX = 78;
        inventoryLabelY = 270;
    }

    private void request(int action) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action);
    }

    @Override
    protected void init() {
        super.init();
        viewportScale = ViewportScale.fit(width, height, GUI_WIDTH, GUI_HEIGHT, 8.0D);
        var tabLook = EnchantSkinButton.Look.sliced("anvil_tab_224", "selected", 224, 40, 16);
        enchantTab = addRenderableWidget(new EnchantSkinButton(leftPos + 8, topPos + 8, 154, 20,
                Component.translatable("menu.moreminecrafts.enchantment"), b -> {
            treePage = 0;
            request(0);
        }, tabLook));
        optionTab = addRenderableWidget(new EnchantSkinButton(leftPos + 168, topPos + 8, 154, 20,
                Component.translatable("menu.moreminecrafts.book_option"), b -> {
            optionPage = 0;
            request(1);
        }, tabLook));

        for (int row = 0; row < TREE_PAGE_ROWS; row++) {
            for (int level = 1; level <= 5; level++) {
                final int rowIndex = row;
                final int targetLevel = level;
                nodes[row][level - 1] = addRenderableWidget(new EnchantSkinButton(leftPos + NODE_LEFT, topPos + TREE_TOP, 18, 18,
                        Component.empty(), b -> {
                    if (rowIndex >= visibleTree.size()) return;
                    EnchantmentTrees.Node node = visibleTree.get(rowIndex);
                    int current = T1Operations.enchantments(menu.getSlot(0).getItem()).getOrDefault(node.id(), 0);
                    if (targetLevel == current + 1) {
                        int index = menu.tree().indexOf(node);
                        if (index >= 0) request(100 + index);
                    }
                }, EnchantSkinButton.Look.plain("node", "locked")));
            }
        }

        for (int row = 0; row < OPTION_PAGE_ROWS; row++) {
            final int rowIndex = row;
            optionRows[row] = addRenderableWidget(new EnchantSkinButton(leftPos + 8, topPos + 86 + row * 21, 314, 20,
                    Component.empty(), b -> {
                if (rowIndex < visibleOptions.size()) selectedOption = visibleOptions.get(rowIndex).id();
                refresh();
            }, EnchantSkinButton.Look.plain("option_row", "disabled"))
                    .selectedWhen(() -> rowIndex < visibleOptions.size()
                            && visibleOptions.get(rowIndex).id().equals(selectedOption)));
        }

        previousTree = addRenderableWidget(new EnchantSkinButton(leftPos + 216, topPos + 246, 20, 18,
                Component.literal("<"), b -> {
            treePage = Math.max(0, treePage - 1);
            refresh();
        }, EnchantSkinButton.Look.glyph("arrow_left")));
        nextTree = addRenderableWidget(new EnchantSkinButton(leftPos + 292, topPos + 246, 20, 18,
                Component.literal(">"), b -> {
            treePage++;
            refresh();
        }, EnchantSkinButton.Look.glyph("arrow_right")));
        previousOptions = addRenderableWidget(new EnchantSkinButton(leftPos + 272, topPos + 171, 20, 18,
                Component.literal("<"), b -> {
            optionPage = Math.max(0, optionPage - 1);
            refresh();
        }, EnchantSkinButton.Look.glyph("arrow_left")));
        nextOptions = addRenderableWidget(new EnchantSkinButton(leftPos + 296, topPos + 171, 20, 18,
                Component.literal(">"), b -> {
            optionPage++;
            refresh();
        }, EnchantSkinButton.Look.glyph("arrow_right")));
        reset = addRenderableWidget(new EnchantSkinButton(leftPos + 8, topPos + 246, 90, 18,
                Component.translatable("menu.moreminecrafts.reset"), b -> {
            EnchantmentTrees.Node node = selectedOptionNode();
            if (node != null) {
                int index = menu.tree().indexOf(node);
                if (index >= 0) request(200 + index);
            }
        }, EnchantSkinButton.Look.plain("reset_btn", "disabled")));
        apply = addRenderableWidget(new EnchantSkinButton(leftPos + 102, topPos + 246, 220, 18, Component.empty(),
                b -> request(2), EnchantSkinButton.Look.plain("apply_btn", "disabled")));
        refresh();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.pose().pushMatrix();
        graphics.pose().translate((float) (viewportScale.left() - leftPos * viewportScale.scale()),
                (float) (viewportScale.top() - topPos * viewportScale.scale()));
        graphics.pose().scale((float) viewportScale.scale(), (float) viewportScale.scale());
        super.extractRenderState(graphics,
                (int) Math.round(viewportScale.contentX(mouseX, leftPos)),
                (int) Math.round(viewportScale.contentY(mouseY, topPos)), delta);
        graphics.pose().popMatrix();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return super.mouseClicked(map(event), doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return super.mouseReleased(map(event));
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return super.mouseDragged(map(event), dragX / viewportScale.scale(), dragY / viewportScale.scale());
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(viewportScale.contentX(mouseX, leftPos), viewportScale.contentY(mouseY, topPos));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return super.mouseScrolled(viewportScale.contentX(mouseX, leftPos),
                viewportScale.contentY(mouseY, topPos), scrollX, scrollY);
    }

    private MouseButtonEvent map(MouseButtonEvent event) {
        return new MouseButtonEvent(viewportScale.contentX(event.x(), leftPos),
                viewportScale.contentY(event.y(), topPos), event.buttonInfo());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        refresh();
    }

    private void refresh() {
        List<EnchantmentTrees.Node> tree = menu.tree();
        String equipmentKey = menu.getSlot(0).getItem().isEmpty()
                ? "" : menu.getSlot(0).getItem().getItem().toString();
        if (!Objects.equals(equipmentKey, selectedEquipment)) {
            selectedEquipment = equipmentKey;
            selectedOption = null;
            optionPage = 0;
        }

        treePages = pages(tree);
        treePage = Math.clamp(treePage, 0, Math.max(0, treePages.size() - 1));
        visibleTree = treePages.isEmpty() ? List.of() : treePages.get(treePage);
        allOptions = tree.stream().filter(n -> BookOptionRules.retained(n.id())).toList();
        if (selectedOption == null || allOptions.stream().noneMatch(n -> n.id().equals(selectedOption))) {
            String source = T1Operations.book(menu.getSlot(1).getItem()).map(T1Operations.Book::id).orElse("");
            selectedOption = allOptions.stream().anyMatch(n -> n.id().equals(source)) ? source
                    : allOptions.stream().filter(n -> T1Operations.enchantments(menu.getSlot(0).getItem()).getOrDefault(n.id(), 0) > 0)
                    .map(EnchantmentTrees.Node::id).findFirst().orElseGet(() -> allOptions.stream()
                            .map(EnchantmentTrees.Node::id).findFirst().orElse(null));
        }
        int optionPages = Math.max(1, (allOptions.size() + OPTION_PAGE_ROWS - 1) / OPTION_PAGE_ROWS);
        optionPage = Math.clamp(optionPage, 0, optionPages - 1);
        int optionStart = Math.min(optionPage * OPTION_PAGE_ROWS, allOptions.size());
        visibleOptions = allOptions.subList(optionStart, Math.min(optionStart + OPTION_PAGE_ROWS, allOptions.size()));

        enchantTab.active = menu.mode() != 0;
        optionTab.active = menu.mode() != 1;
        for (int row = 0; row < TREE_PAGE_ROWS; row++) {
            EnchantmentTrees.Node node = row < visibleTree.size() ? visibleTree.get(row) : null;
            for (int level = 1; level <= 5; level++) {
                EnchantSkinButton button = nodes[row][level - 1];
                button.visible = menu.mode() == 0 && node != null && level <= node.maximum();
                if (!button.visible) continue;
                int offset = node.prerequisite().isEmpty() ? 0 : Math.max(0, node.prerequisiteLevel() - 1);
                button.setX(leftPos + NODE_LEFT + (offset + level - 1) * NODE_STEP);
                button.setY(topPos + TREE_TOP + row * TREE_ROW_STEP + 1);
                int current = T1Operations.enchantments(menu.getSlot(0).getItem()).getOrDefault(node.id(), 0);
                EnchantmentTrees.Quote quote = EnchantmentTrees.quote(EquipmentTreeResolver.resolve(menu.getSlot(0).getItem()),
                        T1Operations.enchantments(menu.getSlot(0).getItem()), node.id());
                boolean registered = registered(node.id());
                boolean next = level == current + 1;
                boolean purchasable = next && quote.allowed() && registered;
                button.active = purchasable && RawXp.canPay(minecraft.player, quote.cost());
                button.setMessage(Component.empty());
                button.forcedState(nodeTextureState(level, current, quote, registered));
                button.setTooltip(nodeTooltip(node, level, current, quote, registered, purchasable));
            }
        }
        for (int row = 0; row < OPTION_PAGE_ROWS; row++) {
            Button button = optionRows[row];
            button.visible = menu.mode() == 1 && row < visibleOptions.size();
            button.active = true;
            if (button.visible) {
                EnchantmentTrees.Node node = visibleOptions.get(row);
                button.setTooltip(Tooltip.create(Component.translatable("menu.moreminecrafts.select_option",
                        Component.translatable(enchantmentKey(node.id())))));
            }
        }

        previousTree.visible = nextTree.visible = menu.mode() == 0 && treePages.size() > 1;
        previousTree.active = treePage > 0;
        nextTree.active = treePage + 1 < treePages.size();
        previousOptions.visible = nextOptions.visible = menu.mode() == 1 && optionPages > 1;
        previousOptions.active = optionPage > 0;
        nextOptions.active = optionPage + 1 < optionPages;

        EnchantmentTrees.Node selected = selectedOptionNode();
        Map<String, Integer> options = T1Operations.options(menu.getSlot(0).getItem());
        boolean hasResetMaterials = List.of(net.minecraft.world.item.Items.DIAMOND,
                        dev.itzgamesgd.moreminecrafts.registry.ModItems.MYTHRIL,
                        net.minecraft.world.item.Items.NETHERITE_INGOT,
                        dev.itzgamesgd.moreminecrafts.registry.ModItems.ADAMANTIUM_INGOT)
                .stream().allMatch(item -> minecraft.player.getInventory().countItem(item) > 0);
        reset.visible = menu.mode() == 1;
        reset.active = selected != null && options.containsKey(selected.id()) && hasResetMaterials;
        reset.setTooltip(Tooltip.create(Component.translatable("menu.moreminecrafts.reset_materials")));
        apply.visible = menu.mode() == 1;
        apply.active = selected != null
                && selected.id().equals(T1Operations.book(menu.getSlot(1).getItem()).map(T1Operations.Book::id).orElse(""))
                && T1Operations.canApply(menu.getSlot(0).getItem(), menu.getSlot(1).getItem())
                && RawXp.canPay(minecraft.player, T1Balance.BOOK_OPTION_XP);
        apply.setMessage(Component.translatable("menu.moreminecrafts.apply_option")
                .append(" (" + T1Balance.BOOK_OPTION_XP + " XP)"));
    }

    private EnchantmentTrees.Node selectedOptionNode() {
        if (selectedOption == null) return null;
        return allOptions.stream().filter(n -> n.id().equals(selectedOption)).findFirst().orElse(null);
    }

    private boolean registered(String id) {
        try {
            return minecraft.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                    .get(ResourceKey.create(Registries.ENCHANTMENT, Identifier.parse(id))).isPresent();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private Tooltip nodeTooltip(EnchantmentTrees.Node node, int level, int current,
                                  EnchantmentTrees.Quote quote, boolean registered, boolean next) {
        Component result = Component.translatable(enchantmentKey(node.id())).append(" ").append(BookOptionRules.roman(level));
        result = result.copy().append("\n").append(Component.translatable(effectKey(node.id())));
        if (level <= current) {
            return Tooltip.create(result.copy().append("\n").append(Component.translatable("menu.moreminecrafts.node_unlocked")));
        }
        if (!next) {
            return Tooltip.create(result.copy().append("\n").append(Component.translatable("menu.moreminecrafts.node_locked_next")));
        }
        result = result.copy().append("\n").append(Component.translatable("menu.moreminecrafts.progression_current_target",
                BookOptionRules.roman(current), BookOptionRules.roman(level)));
        int base = node.cost(level);
        int premium = quote.allowed() ? quote.premium() : 0;
        int total = base + premium;
        result = result.copy().append("\n").append(premium > 0
                ? Component.translatable("menu.moreminecrafts.xp_cost_total", total, base, premium)
                : Component.translatable("menu.moreminecrafts.xp_cost_base", total));
        if (!registered) result = result.copy().append("\n").append(Component.translatable("menu.moreminecrafts.node_unavailable"));
        else if ("prerequisite".equals(quote.reason())) {
            result = result.copy().append("\n").append(Component.translatable("menu.moreminecrafts.missing_prerequisite",
                    Component.translatable(enchantmentKey(node.prerequisite())), BookOptionRules.roman(node.prerequisiteLevel())));
        } else if ("conflict".equals(quote.reason())) {
            result = result.copy().append("\n").append(Component.translatable("menu.moreminecrafts.structural_conflict"));
        }
        if (quote.allowed() && !RawXp.canPay(minecraft.player, total)) {
            result = result.copy().append("\n").append(Component.translatable("menu.moreminecrafts.node_unaffordable"));
        } else if (quote.allowed()) {
            result = result.copy().append("\n").append(Component.translatable("menu.moreminecrafts.node_purchase"));
        }
        return Tooltip.create(result);
    }

    private static String enchantmentKey(String id) {
        return "enchantment." + id.replace(':', '.');
    }

    private static String effectKey(String id) {
        return "menu.moreminecrafts.effect." + id.substring(id.indexOf(':') + 1);
    }

    /** Groups cross-enchantment prerequisite links so a page never splits one of those links. */
    private static List<List<EnchantmentTrees.Node>> pages(List<EnchantmentTrees.Node> tree) {
        if (tree.isEmpty()) return List.of();
        int[] parent = new int[tree.size()];
        for (int i = 0; i < parent.length; i++) parent[i] = i;
        Map<String, Integer> indexById = new HashMap<>();
        for (int i = 0; i < tree.size(); i++) indexById.put(tree.get(i).id(), i);
        for (int i = 0; i < tree.size(); i++) {
            String prerequisite = tree.get(i).prerequisite();
            Integer prerequisiteIndex = indexById.get(prerequisite);
            if (prerequisiteIndex != null) union(parent, i, prerequisiteIndex);
        }
        Map<Integer, List<EnchantmentTrees.Node>> groups = new LinkedHashMap<>();
        for (int i = 0; i < tree.size(); i++) groups.computeIfAbsent(find(parent, i), ignored -> new ArrayList<>()).add(tree.get(i));
        List<List<EnchantmentTrees.Node>> result = new ArrayList<>();
        List<EnchantmentTrees.Node> page = new ArrayList<>();
        for (List<EnchantmentTrees.Node> group : groups.values()) {
            if (!page.isEmpty() && page.size() + group.size() > TREE_PAGE_ROWS) {
                result.add(List.copyOf(page));
                page.clear();
            }
            page.addAll(group);
        }
        if (!page.isEmpty()) result.add(List.copyOf(page));
        return List.copyOf(result);
    }

    private static int find(int[] parent, int value) {
        if (parent[value] != value) parent[value] = find(parent, parent[value]);
        return parent[value];
    }

    private static void union(int[] parent, int first, int second) {
        int a = find(parent, first), b = find(parent, second);
        if (a != b) parent[b] = a;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractBackground(g, mouseX, mouseY, delta);
        // The background is extracted outside the scaled pose in extractRenderState (CI captures at GUI 3/4 showed the
        // widgets shrunk but the panel and slot frames at full size), so apply the same fit transform here unless the
        // current pose already carries it.
        boolean fitHere = viewportScale.scale() < 0.9999D && Math.abs(g.pose().m00() - viewportScale.scale()) > 1.0E-3D;
        if (fitHere) {
            g.pose().pushMatrix();
            g.pose().translate((float) (viewportScale.left() - leftPos * viewportScale.scale()),
                    (float) (viewportScale.top() - topPos * viewportScale.scale()));
            g.pose().scale((float) viewportScale.scale(), (float) viewportScale.scale());
        }
        int x = leftPos, y = topPos;
        EnchantAnvilSkin.blit(g, "enchant_panel_frame", x, y, GUI_WIDTH, GUI_HEIGHT);
        EnchantAnvilSkin.blit(g, "enchant_panel_overlay", x, y, GUI_WIDTH, GUI_HEIGHT);
        for (var slot : menu.slots) {
            if (!slot.isActive()) continue;
            String look = slot == menu.getSlot(0) ? "slot_equipment_normal" : slot == menu.getSlot(1) ? "slot_book_normal" : "slot_normal";
            EnchantAnvilSkin.blit(g, look, x + slot.x - 1, y + slot.y - 1, 18, 18);
        }
        if (fitHere) g.pose().popMatrix();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, Component.translatable("container.inventory"), inventoryLabelX, inventoryLabelY, EnchantAnvilSkin.TEXT, false);
        ItemStack equipment = menu.getSlot(0).getItem();
        Component equipmentName = equipment.isEmpty() ? Component.translatable("menu.moreminecrafts.equipment") : equipment.getHoverName();
        if (menu.mode() == 1) {
            g.text(font, font.substrByWidth(equipmentName, 94).getString(), 36, 46, EnchantAnvilSkin.TEXT, false);
            g.text(font, Component.translatable("menu.moreminecrafts.book_input"), 160, 46, EnchantAnvilSkin.TEXT, false);
            T1Operations.book(menu.getSlot(1).getItem()).ifPresent(book -> {
                Component source = Component.translatable(enchantmentKey(book.id())).append(" ").append(BookOptionRules.roman(book.level()));
                g.text(font, font.substrByWidth(source, 162).getString(), 160, 58, EnchantAnvilSkin.TEXT_DIM, false);
            });
            drawOptionRows(g);
            drawOptionDetail(g);
            return;
        }

        g.text(font, font.substrByWidth(equipmentName, 188).getString(), 36, 46, EnchantAnvilSkin.TEXT, false);
        g.text(font, xpText(), 238, 47, EnchantAnvilSkin.TEXT, false);
        if (visibleTree.isEmpty()) {
            g.text(font, Component.translatable("menu.moreminecrafts.insert_equipment"), 10, 77, EnchantAnvilSkin.TEXT, false);
            return;
        }
        drawTreeConnectors(g);
        for (int row = 0; row < visibleTree.size(); row++) drawTreeRow(g, row, visibleTree.get(row));
        Component page = Component.translatable("menu.moreminecrafts.page", treePage + 1, treePages.size());
        g.text(font, page, 242, 251, EnchantAnvilSkin.TEXT, false);
    }

    private void drawTreeConnectors(GuiGraphicsExtractor g) {
        Map<String, Integer> rowById = new HashMap<>();
        for (int row = 0; row < visibleTree.size(); row++) rowById.put(visibleTree.get(row).id(), row);
        for (int row = 0; row < visibleTree.size(); row++) {
            EnchantmentTrees.Node node = visibleTree.get(row);
            int offset = node.prerequisite().isEmpty() ? 0 : Math.max(0, node.prerequisiteLevel() - 1);
            int nodeY = TREE_TOP + row * TREE_ROW_STEP + 1;
            int owned = T1Operations.enchantments(menu.getSlot(0).getItem()).getOrDefault(node.id(), 0);
            for (int level = 1; level < node.maximum(); level++) {
                int fromX = NODE_LEFT + (offset + level - 1) * NODE_STEP + 18;
                int toX = NODE_LEFT + (offset + level) * NODE_STEP;
                EnchantAnvilSkin.horizontal(g, owned >= level + 1 ? "connector_h_active" : "connector_h_normal", fromX, nodeY + 9, toX - fromX);
            }
            Integer sourceRow = rowById.get(node.prerequisite());
            if (sourceRow != null && node.prerequisiteLevel() > 0) {
                int sourceOffset = visibleTree.get(sourceRow).prerequisite().isEmpty()
                        ? 0 : Math.max(0, visibleTree.get(sourceRow).prerequisiteLevel() - 1);
                int centerX = NODE_LEFT + (sourceOffset + node.prerequisiteLevel() - 1) * NODE_STEP + 9;
                int sourceY = TREE_TOP + sourceRow * TREE_ROW_STEP + 19;
                int targetY = nodeY;
                EnchantAnvilSkin.vertical(g, owned > 0 ? "connector_v_active" : "connector_v_normal", centerX,
                        Math.min(sourceY, targetY), Math.max(sourceY, targetY) - Math.min(sourceY, targetY));
            }
        }
    }

    private void drawTreeRow(GuiGraphicsExtractor g, int row, EnchantmentTrees.Node node) {
        int rowY = TREE_TOP + row * TREE_ROW_STEP;
        Component name = Component.translatable(enchantmentKey(node.id()));
        Component shortName = font.substrByWidth(name, 95).getString().equals(name.getString())
                ? name : Component.literal(font.plainSubstrByWidth(name.getString(), 94) + "…");
        g.text(font, shortName, 8, rowY + 7, EnchantAnvilSkin.TEXT, false);
        int current = T1Operations.enchantments(menu.getSlot(0).getItem()).getOrDefault(node.id(), 0);
        EnchantmentTrees.Quote quote = EnchantmentTrees.quote(EquipmentTreeResolver.resolve(menu.getSlot(0).getItem()),
                T1Operations.enchantments(menu.getSlot(0).getItem()), node.id());
        int offset = node.prerequisite().isEmpty() ? 0 : Math.max(0, node.prerequisiteLevel() - 1);
        for (int level = 1; level <= node.maximum(); level++) {
            int nodeX = NODE_LEFT + (offset + level - 1) * NODE_STEP;
            int nodeY = rowY + 1;
            int color = nodeStateColor(level, current, quote, registered(node.id()));
            g.item(new ItemStack(Items.ENCHANTED_BOOK), nodeX + 3, nodeY + 2);
            String roman = BookOptionRules.roman(level);
            int labelColor = color;
            g.text(font, Component.literal(roman), nodeX + (18 - font.width(roman)) / 2,
                    rowY + 20, labelColor, false);
        }
    }

    /** Node frame look: owned, locked (unreachable, conflicting or not allowed) or available. */
    private static String nodeTextureState(int level, int current, EnchantmentTrees.Quote quote, boolean registered) {
        if (level <= current) return "owned";
        if (level > current + 1) return "locked";
        if (!registered || "conflict".equals(quote.reason()) || !quote.allowed()) return "locked";
        return "available";
    }

    private int nodeStateColor(int level, int current, EnchantmentTrees.Quote quote, boolean registered) {
        if (level <= current) return 0xFFF6CA5C;
        if (level > current + 1) return 0xFF766C74;
        if (!registered || "conflict".equals(quote.reason())) return 0xFFFF7A7A;
        if (!quote.allowed()) return 0xFF766C74;
        if (!RawXp.canPay(minecraft.player, quote.cost())) return 0xFFDAA844;
        return quote.premium() > 0 ? 0xFFDAA844 : 0xFFE6DAF2;
    }

    private void drawOptionRows(GuiGraphicsExtractor g) {
        g.text(font, Component.translatable("menu.moreminecrafts.option_column_enchantment"), 17, 75, EnchantAnvilSkin.TEXT, false);
        g.text(font, Component.translatable("menu.moreminecrafts.option_column_current"), 150, 75, EnchantAnvilSkin.TEXT, false);
        g.text(font, Component.translatable("menu.moreminecrafts.option_column_possible"), 217, 75, EnchantAnvilSkin.TEXT, false);
        ItemStack equipment = menu.getSlot(0).getItem();
        Map<String, Integer> options = T1Operations.options(equipment);
        for (int row = 0; row < visibleOptions.size(); row++) {
            EnchantmentTrees.Node node = visibleOptions.get(row);
            int y = 86 + row * 21;
            boolean selected = node.id().equals(selectedOption);
            int current = options.getOrDefault(node.id(), 0);
            Component name = Component.translatable(enchantmentKey(node.id()));
            Component clippedName = font.width(name) > 128
                    ? Component.literal(font.plainSubstrByWidth(name.getString(), 124) + "…") : name;
            g.text(font, clippedName, 17, y + 6, selected ? 0xFFFFFFFF : EnchantAnvilSkin.TEXT, false);
            String currentText = current == 0 ? Component.translatable("menu.moreminecrafts.option_none").getString()
                    : "(+" + BookOptionRules.roman(current) + ")";
            g.text(font, Component.literal(currentText), 150, y + 6,
                    selected ? 0xFFFFFFFF : EnchantAnvilSkin.TEXT, false);
            String possible = "(+I)–(+" + BookOptionRules.roman(BookOptionRules.MAXIMUMS.get(node.id())) + ")";
            g.text(font, Component.literal(possible), 217, y + 6,
                    selected ? 0xFFFFFFFF : EnchantAnvilSkin.TEXT, false);
        }
        g.text(font, xpText(), 232, 64, EnchantAnvilSkin.TEXT, false);
        if (allOptions.isEmpty()) g.text(font, Component.translatable("menu.moreminecrafts.no_options_for_equipment"), 12, 98, EnchantAnvilSkin.TEXT, false);
        if (allOptions.size() > OPTION_PAGE_ROWS) {
            g.text(font, Component.translatable("menu.moreminecrafts.page", optionPage + 1,
                    (allOptions.size() + OPTION_PAGE_ROWS - 1) / OPTION_PAGE_ROWS), 223, 176, EnchantAnvilSkin.TEXT, false);
        }
    }

    private void drawOptionDetail(GuiGraphicsExtractor g) {
        EnchantmentTrees.Node selected = selectedOptionNode();
        EnchantAnvilSkin.blit(g, "divider", 8, 191, 314, 1);
        if (selected == null) return;
        int current = T1Operations.options(menu.getSlot(0).getItem()).getOrDefault(selected.id(), 0);
        int maximum = BookOptionRules.MAXIMUMS.get(selected.id());
        Component name = Component.translatable(enchantmentKey(selected.id()));
        Component currentText = current == 0 ? Component.translatable("menu.moreminecrafts.option_none")
                : Component.literal("(+" + BookOptionRules.roman(current) + ")");
        Component title = Component.translatable("menu.moreminecrafts.option_detail_title", name, currentText);
        g.text(font, font.substrByWidth(title, 314).getString(), 8, 196, EnchantAnvilSkin.TEXT, false);
        g.text(font, Component.translatable("menu.moreminecrafts.option_possible_range", "+I", "+" + BookOptionRules.roman(maximum)),
                8, 208, EnchantAnvilSkin.TEXT, false);
        String source = T1Operations.book(menu.getSlot(1).getItem()).filter(b -> b.id().equals(selected.id()))
                .map(b -> Component.translatable("menu.moreminecrafts.option_source_level", BookOptionRules.roman(b.level())).getString())
                .orElseGet(() -> Component.translatable("menu.moreminecrafts.option_source_random").getString());
        g.text(font, Component.literal(source), 8, 220, EnchantAnvilSkin.TEXT_DIM, false);
        Component effect = Component.translatable(optionEffectKey(selected.id()));
        g.text(font, font.substrByWidth(effect, 314).getString(), 8, 232, EnchantAnvilSkin.TEXT, false);
    }

    private static String optionEffectKey(String id) {
        return "menu.moreminecrafts.option_effect." + id.substring(id.indexOf(':') + 1);
    }

    private Component xpText() {
        return Component.translatable("menu.moreminecrafts.xp_amount", NumberFormat.getIntegerInstance(Locale.ROOT)
                .format(RawXp.available(minecraft.player)));
    }
}
