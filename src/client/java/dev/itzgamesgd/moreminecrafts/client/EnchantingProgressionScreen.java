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
    private static final int GUI_HEIGHT = 360;
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
    private final Button[][] nodes = new Button[TREE_PAGE_ROWS][5];
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
        enchantTab = addRenderableWidget(Button.builder(Component.translatable("menu.moreminecrafts.enchantment"), b -> {
            treePage = 0;
            request(0);
        }).bounds(leftPos + 8, topPos + 8, 154, 20).build());
        optionTab = addRenderableWidget(Button.builder(Component.translatable("menu.moreminecrafts.book_option"), b -> {
            optionPage = 0;
            request(1);
        }).bounds(leftPos + 168, topPos + 8, 154, 20).build());

        for (int row = 0; row < TREE_PAGE_ROWS; row++) {
            for (int level = 1; level <= 5; level++) {
                final int rowIndex = row;
                final int targetLevel = level;
                nodes[row][level - 1] = addRenderableWidget(Button.builder(Component.empty(), b -> {
                    if (rowIndex >= visibleTree.size()) return;
                    EnchantmentTrees.Node node = visibleTree.get(rowIndex);
                    int current = T1Operations.enchantments(menu.getSlot(0).getItem()).getOrDefault(node.id(), 0);
                    if (targetLevel == current + 1) {
                        int index = menu.tree().indexOf(node);
                        if (index >= 0) request(100 + index);
                    }
                }).bounds(leftPos + NODE_LEFT, topPos + TREE_TOP, 18, 18).build());
            }
        }

        for (int row = 0; row < OPTION_PAGE_ROWS; row++) {
            final int rowIndex = row;
            optionRows[row] = addRenderableWidget(Button.builder(Component.empty(), b -> {
                if (rowIndex < visibleOptions.size()) selectedOption = visibleOptions.get(rowIndex).id();
                refresh();
            }).bounds(leftPos + 8, topPos + 86 + row * 21, 314, 20).build());
        }

        previousTree = addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            treePage = Math.max(0, treePage - 1);
            refresh();
        }).bounds(leftPos + 216, topPos + 246, 20, 18).build());
        nextTree = addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            treePage++;
            refresh();
        }).bounds(leftPos + 292, topPos + 246, 20, 18).build());
        previousOptions = addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            optionPage = Math.max(0, optionPage - 1);
            refresh();
        }).bounds(leftPos + 272, topPos + 171, 20, 18).build());
        nextOptions = addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            optionPage++;
            refresh();
        }).bounds(leftPos + 296, topPos + 171, 20, 18).build());
        reset = addRenderableWidget(Button.builder(Component.translatable("menu.moreminecrafts.reset"), b -> {
            EnchantmentTrees.Node node = selectedOptionNode();
            if (node != null) {
                int index = menu.tree().indexOf(node);
                if (index >= 0) request(200 + index);
            }
        }).bounds(leftPos + 8, topPos + 246, 90, 18).build());
        apply = addRenderableWidget(Button.builder(Component.empty(), b -> request(2))
                .bounds(leftPos + 102, topPos + 246, 220, 18).build());
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
                Button button = nodes[row][level - 1];
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
        int x = leftPos, y = topPos;
        g.fill(x, y, x + GUI_WIDTH, y + GUI_HEIGHT, 0xFFC6C6C6);
        g.fill(x + 2, y + 2, x + 328, y + 3, 0xFFFFFFFF);
        g.fill(x + 2, y + 2, x + 3, y + GUI_HEIGHT - 2, 0xFFFFFFFF);
        g.fill(x + GUI_WIDTH - 3, y + 3, x + GUI_WIDTH - 2, y + GUI_HEIGHT - 2, 0xFF555555);
        g.fill(x + 2, y + GUI_HEIGHT - 3, x + GUI_WIDTH - 2, y + GUI_HEIGHT - 2, 0xFF555555);
        for (var slot : menu.slots) {
            if (!slot.isActive()) continue;
            int sx = x + slot.x - 1, sy = y + slot.y - 1;
            g.fill(sx, sy, sx + 18, sy + 18, 0xFF373737);
            g.fill(sx + 1, sy + 1, sx + 18, sy + 18, 0xFFFFFFFF);
            g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF8B8B8B);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, Component.translatable("container.inventory"), inventoryLabelX, inventoryLabelY, 0xFF404040, false);
        ItemStack equipment = menu.getSlot(0).getItem();
        Component equipmentName = equipment.isEmpty() ? Component.translatable("menu.moreminecrafts.equipment") : equipment.getHoverName();
        if (menu.mode() == 1) {
            g.text(font, font.substrByWidth(equipmentName, 94).getString(), 36, 46, 0xFF404040, false);
            g.text(font, Component.translatable("menu.moreminecrafts.book_input"), 160, 46, 0xFF404040, false);
            T1Operations.book(menu.getSlot(1).getItem()).ifPresent(book -> {
                Component source = Component.translatable(enchantmentKey(book.id())).append(" ").append(BookOptionRules.roman(book.level()));
                g.text(font, font.substrByWidth(source, 162).getString(), 160, 58, 0xFF555555, false);
            });
            drawOptionRows(g);
            drawOptionDetail(g);
            return;
        }

        g.text(font, font.substrByWidth(equipmentName, 188).getString(), 36, 46, 0xFF404040, false);
        g.text(font, xpText(), 238, 47, 0xFF404040, false);
        if (visibleTree.isEmpty()) {
            g.text(font, Component.translatable("menu.moreminecrafts.insert_equipment"), 10, 77, 0xFF404040, false);
            return;
        }
        drawTreeConnectors(g);
        for (int row = 0; row < visibleTree.size(); row++) drawTreeRow(g, row, visibleTree.get(row));
        Component page = Component.translatable("menu.moreminecrafts.page", treePage + 1, treePages.size());
        g.text(font, page, 242, 251, 0xFF404040, false);
    }

    private void drawTreeConnectors(GuiGraphicsExtractor g) {
        Map<String, Integer> rowById = new HashMap<>();
        for (int row = 0; row < visibleTree.size(); row++) rowById.put(visibleTree.get(row).id(), row);
        for (int row = 0; row < visibleTree.size(); row++) {
            EnchantmentTrees.Node node = visibleTree.get(row);
            int offset = node.prerequisite().isEmpty() ? 0 : Math.max(0, node.prerequisiteLevel() - 1);
            int nodeY = TREE_TOP + row * TREE_ROW_STEP + 1;
            for (int level = 1; level < node.maximum(); level++) {
                int fromX = NODE_LEFT + (offset + level - 1) * NODE_STEP + 18;
                int toX = NODE_LEFT + (offset + level) * NODE_STEP;
                g.fill(fromX, nodeY + 9, toX, nodeY + 10, 0xFF555555);
            }
            Integer sourceRow = rowById.get(node.prerequisite());
            if (sourceRow != null && node.prerequisiteLevel() > 0) {
                int sourceOffset = visibleTree.get(sourceRow).prerequisite().isEmpty()
                        ? 0 : Math.max(0, visibleTree.get(sourceRow).prerequisiteLevel() - 1);
                int centerX = NODE_LEFT + (sourceOffset + node.prerequisiteLevel() - 1) * NODE_STEP + 9;
                int sourceY = TREE_TOP + sourceRow * TREE_ROW_STEP + 19;
                int targetY = nodeY;
                g.fill(centerX, Math.min(sourceY, targetY), centerX + 1, Math.max(sourceY, targetY), 0xFF555555);
            }
        }
    }

    private void drawTreeRow(GuiGraphicsExtractor g, int row, EnchantmentTrees.Node node) {
        int rowY = TREE_TOP + row * TREE_ROW_STEP;
        Component name = Component.translatable(enchantmentKey(node.id()));
        Component shortName = font.substrByWidth(name, 95).getString().equals(name.getString())
                ? name : Component.literal(font.plainSubstrByWidth(name.getString(), 94) + "…");
        g.text(font, shortName, 8, rowY + 7, 0xFF303030, false);
        int current = T1Operations.enchantments(menu.getSlot(0).getItem()).getOrDefault(node.id(), 0);
        EnchantmentTrees.Quote quote = EnchantmentTrees.quote(EquipmentTreeResolver.resolve(menu.getSlot(0).getItem()),
                T1Operations.enchantments(menu.getSlot(0).getItem()), node.id());
        int offset = node.prerequisite().isEmpty() ? 0 : Math.max(0, node.prerequisiteLevel() - 1);
        for (int level = 1; level <= node.maximum(); level++) {
            int nodeX = NODE_LEFT + (offset + level - 1) * NODE_STEP;
            int nodeY = rowY + 1;
            int color = nodeStateColor(level, current, quote, registered(node.id()));
            g.fill(nodeX, nodeY, nodeX + 18, nodeY + 1, color);
            g.fill(nodeX, nodeY + 17, nodeX + 18, nodeY + 18, color);
            g.fill(nodeX, nodeY, nodeX + 1, nodeY + 18, color);
            g.fill(nodeX + 17, nodeY, nodeX + 18, nodeY + 18, color);
            g.item(new ItemStack(Items.ENCHANTED_BOOK), nodeX + 3, nodeY + 2);
            String roman = BookOptionRules.roman(level);
            int labelColor = color;
            g.text(font, Component.literal(roman), nodeX + (18 - font.width(roman)) / 2,
                    rowY + 20, labelColor, false);
        }
    }

    private int nodeStateColor(int level, int current, EnchantmentTrees.Quote quote, boolean registered) {
        if (level <= current) return 0xFF3A8F3A;
        if (level > current + 1) return 0xFF686868;
        if (!registered || "conflict".equals(quote.reason())) return 0xFFB23A3A;
        if (!quote.allowed()) return 0xFF777777;
        if (!RawXp.canPay(minecraft.player, quote.cost())) return 0xFFD4A12A;
        return quote.premium() > 0 ? 0xFFD4A12A : 0xFFE9E9E9;
    }

    private void drawOptionRows(GuiGraphicsExtractor g) {
        g.text(font, Component.translatable("menu.moreminecrafts.option_column_enchantment"), 17, 75, 0xFF404040, false);
        g.text(font, Component.translatable("menu.moreminecrafts.option_column_current"), 150, 75, 0xFF404040, false);
        g.text(font, Component.translatable("menu.moreminecrafts.option_column_possible"), 217, 75, 0xFF404040, false);
        ItemStack equipment = menu.getSlot(0).getItem();
        Map<String, Integer> options = T1Operations.options(equipment);
        for (int row = 0; row < visibleOptions.size(); row++) {
            EnchantmentTrees.Node node = visibleOptions.get(row);
            int y = 86 + row * 21;
            boolean selected = node.id().equals(selectedOption);
            if (selected) g.fill(9, y + 1, 321, y + 19, 0xFF7296B8);
            int current = options.getOrDefault(node.id(), 0);
            Component name = Component.translatable(enchantmentKey(node.id()));
            Component clippedName = font.width(name) > 128
                    ? Component.literal(font.plainSubstrByWidth(name.getString(), 124) + "…") : name;
            g.text(font, clippedName, 17, y + 6, selected ? 0xFFFFFFFF : 0xFF303030, false);
            String currentText = current == 0 ? Component.translatable("menu.moreminecrafts.option_none").getString()
                    : "(+" + BookOptionRules.roman(current) + ")";
            g.text(font, Component.literal(currentText), 150, y + 6,
                    selected ? 0xFFFFFFFF : 0xFF303030, false);
            String possible = "(+I)–(+" + BookOptionRules.roman(BookOptionRules.MAXIMUMS.get(node.id())) + ")";
            g.text(font, Component.literal(possible), 217, y + 6,
                    selected ? 0xFFFFFFFF : 0xFF303030, false);
        }
        g.text(font, xpText(), 232, 64, 0xFF404040, false);
        if (allOptions.isEmpty()) g.text(font, Component.translatable("menu.moreminecrafts.no_options_for_equipment"), 12, 98, 0xFF404040, false);
        if (allOptions.size() > OPTION_PAGE_ROWS) {
            g.text(font, Component.translatable("menu.moreminecrafts.page", optionPage + 1,
                    (allOptions.size() + OPTION_PAGE_ROWS - 1) / OPTION_PAGE_ROWS), 223, 176, 0xFF404040, false);
        }
    }

    private void drawOptionDetail(GuiGraphicsExtractor g) {
        EnchantmentTrees.Node selected = selectedOptionNode();
        g.fill(8, 191, 322, 192, 0xFF888888);
        if (selected == null) return;
        int current = T1Operations.options(menu.getSlot(0).getItem()).getOrDefault(selected.id(), 0);
        int maximum = BookOptionRules.MAXIMUMS.get(selected.id());
        Component name = Component.translatable(enchantmentKey(selected.id()));
        Component currentText = current == 0 ? Component.translatable("menu.moreminecrafts.option_none")
                : Component.literal("(+" + BookOptionRules.roman(current) + ")");
        Component title = Component.translatable("menu.moreminecrafts.option_detail_title", name, currentText);
        g.text(font, font.substrByWidth(title, 314).getString(), 8, 196, 0xFF303030, false);
        g.text(font, Component.translatable("menu.moreminecrafts.option_possible_range", "+I", "+" + BookOptionRules.roman(maximum)),
                8, 208, 0xFF303030, false);
        String source = T1Operations.book(menu.getSlot(1).getItem()).filter(b -> b.id().equals(selected.id()))
                .map(b -> Component.translatable("menu.moreminecrafts.option_source_level", BookOptionRules.roman(b.level())).getString())
                .orElseGet(() -> Component.translatable("menu.moreminecrafts.option_source_random").getString());
        g.text(font, Component.literal(source), 8, 220, 0xFF555555, false);
        Component effect = Component.translatable(optionEffectKey(selected.id()));
        g.text(font, font.substrByWidth(effect, 314).getString(), 8, 232, 0xFF303030, false);
    }

    private static String optionEffectKey(String id) {
        return "menu.moreminecrafts.option_effect." + id.substring(id.indexOf(':') + 1);
    }

    private Component xpText() {
        return Component.translatable("menu.moreminecrafts.xp_amount", NumberFormat.getIntegerInstance(Locale.ROOT)
                .format(RawXp.available(minecraft.player)));
    }
}
