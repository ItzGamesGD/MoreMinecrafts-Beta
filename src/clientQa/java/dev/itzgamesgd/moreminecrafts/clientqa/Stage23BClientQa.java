package dev.itzgamesgd.moreminecrafts.clientqa;

import dev.itzgamesgd.moreminecrafts.client.ProgressionDisciplineSelectionScreen;
import dev.itzgamesgd.moreminecrafts.client.ProgressionSelectionScreen;
import dev.itzgamesgd.moreminecrafts.client.ProgressionBrowseScreen;
import dev.itzgamesgd.moreminecrafts.equipment.BackpackItem;
import dev.itzgamesgd.moreminecrafts.equipment.PortableFurnaceContents;
import dev.itzgamesgd.moreminecrafts.equipment.PortableFurnaceMenu;
import dev.itzgamesgd.moreminecrafts.progression.ViewportScale;
import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Isolated client-only QA harness. All input paths and visual confidence are recorded explicitly. */
public final class Stage23BClientQa implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("MoreMinecrafts-ClientQA");
    private static final String SERVER_ENV = "MOREMINECRAFTS_CLIENT_QA_SERVER_ADDRESS";
    private static final List<String> SCENARIOS = List.of(
            "four_card_selector",
            "large_backpack_icon_and_movement",
            "flame_sword_front_back_model_texture",
            "greatsword_hit_miss_sweep_particles",
            "enchanting_gui_multiple_scales",
            "portable_furnace_inventory_cooking_persistence",
            "miner_lantern_lambdynamiclights"
    );
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssSSS'Z'").withZone(ZoneOffset.UTC);
    private static final List<Result> RESULTS = new ArrayList<>();
    private static final Map<String, List<String>> SCREENSHOTS = new HashMap<>();
    private static int startupTicks;
    private static int worldTicks;
    private static int renderFrames;
    private static boolean connectRequested;
    private static boolean suiteStarted;
    private static boolean suiteFinished;
    private static Capture pendingCapture;
    private static int backpackStage;
    private static int furnaceStage;
    private static int waitTicks;
    private static int backpackHotbarSlot = -1;
    private static int furnaceHotbarSlot = -1;
    private static PortableFurnaceContents furnaceSavedAfterClose;

    @Override
    public void onInitializeClient() {
        if (!"1".equals(System.getenv("MOREMINECRAFTS_CLIENT_QA"))) return;
        LOGGER.info("STAGE23B_QA_MOD_LOADED mod=moreminecrafts_client_qa");
        HudElementRegistry.attachElementAfter(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("moreminecrafts", "stage23b_client_qa_capture"),
                (graphics, delta) -> onRenderedFrame(Minecraft.getInstance()));
        ClientTickEvents.END_CLIENT_TICK.register(Stage23BClientQa::tick);
    }

    private static void tick(Minecraft client) {
        if (suiteFinished) return;
        if (client.level == null || client.player == null || client.getConnection() == null) {
            if (!connectRequested && ++startupTicks >= 60) connect(client);
            if (startupTicks > 600 && !connectRequested) finishBlocked("deterministic server address is missing");
            if (startupTicks > 1200 && connectRequested) finishBlocked("timed out joining deterministic server; no ClientLevel/LocalPlayer/connection");
            return;
        }

        if (!suiteStarted) {
            suiteStarted = true;
            LOGGER.info("STAGE23B_QA_WORLD_JOINED dimension={} player={} pos=({}, {}, {}) server_connection={} registries=items:{},components:{}",
                    client.level.dimension(), client.player.getGameProfile().getName(),
                    client.player.getX(), client.player.getY(), client.player.getZ(),
                    client.getConnection().getConnection().isConnected(), BuiltInRegistries.ITEM.size(),
                    BuiltInRegistries.DATA_COMPONENT_TYPE.size());
            LOGGER.info("STAGE23B_QA_WORLD_READY client_level=true local_player=true server_connection_observed=true test_world=visual-validation-world");
            queueCapture("in_world", "before", "four_card_selector", "client level and player present; untouched deterministic scene");
            return;
        }
        if (pendingCapture != null) return;
        if (++worldTicks < 3) return;
        worldTicks = 0;

        if (RESULTS.isEmpty()) {
            runSelector(client);
            return;
        }
        if (RESULTS.size() == 1) {
            runBackpackFixture(client);
            return;
        }
        if (RESULTS.size() == 2) {
            if (furnaceStage >= 6) {
                finishRemainingProbes(client);
                return;
            }
            runPortableFurnaceFixture(client);
            return;
        }
        finishRemainingProbes(client);
    }

    private static void connect(Minecraft client) {
        String address = System.getenv(SERVER_ENV);
        if (address == null || address.isBlank()) {
            LOGGER.error("STAGE23B_QA_CONNECT_BLOCKED reason=missing_env variable={}", SERVER_ENV);
            return;
        }
        ServerData server = new ServerData("MoreMinecrafts Stage 23-B QA", address, ServerData.Type.OTHER);
        ConnectScreen.startConnecting(client.gui.screen(), client, ServerAddress.parseString(address), server, false, null);
        connectRequested = true;
        LOGGER.info("STAGE23B_QA_CONNECTING server={}", address);
    }

    private static void onRenderedFrame(Minecraft client) {
        if (pendingCapture == null || client.level == null || client.player == null) return;
        if (++renderFrames < 3) return;
        Capture capture = pendingCapture;
        pendingCapture = null;
        renderFrames = 0;
        Path screenshotDir = client.gameDirectory.toPath().resolve("screenshots");
        String filename = "stage23b_" + capture.caseId() + "_" + capture.phase() + "_" + STAMP.format(Instant.now()) + ".png";
        Path output = screenshotDir.resolve(filename);
        try {
            Files.createDirectories(screenshotDir);
            Screenshot.grab(client.gameDirectory, filename, client.gameRenderer.mainRenderTarget(), 1, message -> {
                boolean exists = Files.isRegularFile(output) && fileSize(output) > 0;
                LOGGER.info("STAGE23B_QA_SCREENSHOT case={} phase={} saved={} path={} bytes={} meaning=LIVE_PENDING source=Minecraft.Screenshot.grab(mainRenderTarget)",
                        capture.caseId(), capture.phase(), exists, output.toAbsolutePath(), fileSize(output));
                if (!exists) LOGGER.error("STAGE23B_QA_SCREENSHOT_FAILED case={} phase={} callback={}", capture.caseId(), capture.phase(), message.getString());
                if (exists) SCREENSHOTS.computeIfAbsent(capture.caseId(), ignored -> new ArrayList<>()).add(filename);
                capture.complete(exists, output.toString());
            });
        } catch (Exception e) {
            LOGGER.error("STAGE23B_QA_SCREENSHOT_FAILED case={} phase={} error={}", capture.caseId(), capture.phase(), e.toString());
            capture.complete(false, "");
        }
    }

    private static long fileSize(Path path) {
        try { return Files.size(path); } catch (Exception ignored) { return 0; }
    }

    private static void queueCapture(String caseId, String phase, String scenario, String note) {
        pendingCapture = new Capture(caseId, phase, scenario, note, (ok, path) -> {});
        LOGGER.info("STAGE23B_QA_CAPTURE_QUEUED case={} phase={} note={}", caseId, phase, note);
    }

    private static void runSelector(Minecraft client) {
        ProgressionSelectionScreen selector = new ProgressionSelectionScreen();
        client.gui.setScreen(selector);
        queueCapture("four_card_selector", "before", "four_card_selector", "selector screen rendered before callback");
        waitForCapture(client, () -> {
            boolean callbackConsumed = selector.validationClick("knowledge");
            boolean correctDestination = client.gui.screen() instanceof ProgressionDisciplineSelectionScreen screen
                    && screen.mode() == ProgressionBrowseScreen.Mode.KNOWLEDGE;
            boolean passed = callbackConsumed && correctDestination;
            if (correctDestination) queueCapture("four_card_selector", "after", "four_card_selector", "actual knowledge destination screen after synthetic Screen callback");
            Object displayedScreen = client.gui.screen();
            ResultsAdd("four_card_selector", passed ? "PASS" : "FAIL", "synthetic_screen_callback",
                    "callback_consumed=" + callbackConsumed + " destination=knowledge screen="
                            + (displayedScreen == null ? "none" : displayedScreen.getClass().getSimpleName())
                            + "; OS_XTEST_XDOTOOL=NOT_RUN; human_manual_play=NOT_RUN",
                    List.of());
        });
    }

    private static void waitForCapture(Minecraft client, Runnable afterCapture) {
        // Screenshot.grab is asynchronous. Queue a sentinel frame and continue after its callback.
        Capture prior = pendingCapture;
        if (prior == null) { afterCapture.run(); return; }
        pendingCapture = new Capture(prior.caseId(), prior.phase(), prior.scenario(), prior.note(), (ok, path) -> {
            prior.complete(ok, path);
            afterCapture.run();
        });
    }

    private static void runBackpackFixture(Minecraft client) {
        try {
            if (!(ModItems.LARGE_BACKPACK instanceof BackpackItem item) || item.capacity() != 54) {
                ResultsAdd("large_backpack_icon_and_movement", "FAIL", "direct_state_fixture", "Large Backpack registration/capacity assertion failed", List.of());
                return;
            }
            if (backpackStage == 0) {
                backpackHotbarSlot = findInventoryItem(client, ModItems.LARGE_BACKPACK);
                if (backpackHotbarSlot < 0) throw new IllegalStateException("RCON fixture backpack was not synchronized to the client inventory");
                client.player.getInventory().selected = backpackHotbarSlot;
                client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                backpackStage = 1;
                LOGGER.info("STAGE23B_QA_BACKPACK_ACTION input_path=synthetic_client_game_packet action=use_item inventory_slot={}", backpackHotbarSlot);
                return;
            }
            if (!(client.gui.screen() instanceof AbstractContainerScreen<?>)) {
                if (++waitTicks > 100) throw new IllegalStateException("server did not open the Large Backpack menu after synthetic use packet");
                return;
            }
            AbstractContainerMenu menu = client.player.containerMenu;
            if (menu.slots.size() < 54) throw new IllegalStateException("Large Backpack menu exposed fewer than 54 storage slots");
            if (backpackStage == 1) {
                int stoneSource = findMenuStack(menu, Items.STONE, 54);
                if (stoneSource < 0) throw new IllegalStateException("RCON fixture stone stack was not synchronized into the player inventory menu");
                client.gameMode.handleInventoryMouseClick(menu.containerId, stoneSource, 0, ContainerInput.PICKUP, client.player);
                client.gameMode.handleInventoryMouseClick(menu.containerId, 0, 0, ContainerInput.PICKUP, client.player);
                backpackStage = 2;
                waitTicks = 0;
                return;
            }
            if (backpackStage == 2) {
                ItemStack stored = menu.getSlot(0).getItem();
                if (stored.getItem() != Items.STONE || stored.getCount() != 32) {
                    if (++waitTicks > 100) throw new IllegalStateException("server-backed backpack storage did not receive all 32 stone items");
                    return;
                }
                queueCapture("large_backpack_icon_and_movement", "before", "large_backpack_icon_and_movement", "real six-row Backpack chest menu; server-synchronized stone stack in slot 0");
                backpackStage = 3;
                waitForCapture(client, () -> {
                    AbstractContainerMenu current = client.player.containerMenu;
                    int destination = findEmptyPlayerMenuSlot(current, 54);
                    client.gameMode.handleInventoryMouseClick(current.containerId, 0, 0, ContainerInput.PICKUP, client.player);
                    client.gameMode.handleInventoryMouseClick(current.containerId, destination, 0, ContainerInput.PICKUP, client.player);
                    backpackStage = 4;
                    waitTicks = 0;
                });
                return;
            }
            if (backpackStage == 4) {
                ItemStack moved = menu.getSlot(0).getItem();
                int stoneTotal = countMenuItem(menu, Items.STONE);
                boolean movedOut = moved.isEmpty() && stoneTotal == 32;
                if (!movedOut && ++waitTicks <= 100) return;
                if (client.player.containerMenu != client.player.inventoryMenu) client.player.closeContainer();
                ResultsAdd("large_backpack_icon_and_movement", movedOut ? "PASS" : "FAIL", "synthetic_client_game_packet",
                        "joined_world=true real_server_backed_menu=true visible_stack_before=stone:32 movement_out_of_storage=" + movedOut
                                + "; item icon semantic correctness requires human review; OS_XTEST_XDOTool=NOT_RUN", List.of());
                queueCapture("large_backpack_icon_and_movement", "after", "large_backpack_icon_and_movement", "after server-backed slot transfer out of Backpack");
                backpackStage = 5;
                return;
            }
        } catch (Throwable t) {
            ResultsAdd("large_backpack_icon_and_movement", "FAIL", "synthetic_client_game_packet", "in-world Backpack gameplay exception=" + t, List.of());
        }
    }

    private static void runPortableFurnaceFixture(Minecraft client) {
        try {
            if (furnaceStage == 0) {
                furnaceHotbarSlot = findInventoryItem(client, ModItems.PORTABLE_FURNACE);
                if (furnaceHotbarSlot < 0) throw new IllegalStateException("RCON fixture Portable Furnace was not synchronized to the client inventory");
                client.player.getInventory().selected = furnaceHotbarSlot;
                client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                furnaceStage = 1;
                LOGGER.info("STAGE23B_QA_FURNACE_ACTION input_path=synthetic_client_game_packet action=use_same_item inventory_slot={}", furnaceHotbarSlot);
                return;
            }
            if (furnaceStage == 1) {
                if (!(client.player.containerMenu instanceof PortableFurnaceMenu menu)) {
                    if (++waitTicks > 100) throw new IllegalStateException("server did not open PortableFurnaceMenu after use packet");
                    return;
                }
                int rawIron = findMenuStack(menu, Items.RAW_IRON, 3);
                int coal = findMenuStack(menu, Items.COAL, 3);
                if (rawIron < 0 || coal < 0) throw new IllegalStateException("RCON fixture raw iron or coal was not present in the server menu");
                client.gameMode.handleInventoryMouseClick(menu.containerId, rawIron, 0, ContainerInput.PICKUP, client.player);
                client.gameMode.handleInventoryMouseClick(menu.containerId, 0, 0, ContainerInput.PICKUP, client.player);
                client.gameMode.handleInventoryMouseClick(menu.containerId, coal, 0, ContainerInput.PICKUP, client.player);
                client.gameMode.handleInventoryMouseClick(menu.containerId, 1, 0, ContainerInput.PICKUP, client.player);
                furnaceStage = 2;
                waitTicks = 0;
                return;
            }
            if (furnaceStage == 2 && client.player.containerMenu instanceof PortableFurnaceMenu menu) {
                if (++waitTicks < 90) return;
                queueCapture("portable_furnace_inventory_cooking_persistence", "before", "portable_furnace_inventory_cooking_persistence", "real server-backed Portable Furnace menu with input and fuel loaded");
                furnaceStage = 3;
                waitForCapture(client, () -> {
                    client.player.closeContainer();
                    furnaceStage = 4;
                    waitTicks = 0;
                });
                return;
            }
            if (furnaceStage == 4) {
                if (++waitTicks < 10) return;
                ItemStack sameItem = client.player.getInventory().getItem(furnaceHotbarSlot);
                furnaceSavedAfterClose = sameItem.get(ModDataComponents.PORTABLE_FURNACE_CONTENTS);
                if (furnaceSavedAfterClose == null && waitTicks < 100) return;
                if (client.player.containerMenu != client.player.inventoryMenu && waitTicks < 100) return;
                if (furnaceSavedAfterClose == null) throw new IllegalStateException("Portable Furnace item lacks persisted contents after close");
                if (furnaceSavedAfterClose.burnTime() <= 0 || furnaceSavedAfterClose.cookTime() <= 0)
                    throw new IllegalStateException("Portable Furnace did not persist positive burn and cooking progress after in-world ticks: burn=" + furnaceSavedAfterClose.burnTime() + " cook=" + furnaceSavedAfterClose.cookTime());
                client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                furnaceStage = 5;
                waitTicks = 0;
                return;
            }
            if (furnaceStage == 5) {
                if (!(client.player.containerMenu instanceof PortableFurnaceMenu menu)) {
                    if (++waitTicks > 100) throw new IllegalStateException("same Portable Furnace item did not reopen its menu");
                    return;
                }
                boolean inputRestored = ItemStack.matches(menu.getSlot(0).getItem(), furnaceSavedAfterClose.items().get(0));
                boolean fuelRestored = ItemStack.matches(menu.getSlot(1).getItem(), furnaceSavedAfterClose.items().get(1));
                boolean cookingPersisted = inputRestored && fuelRestored && furnaceSavedAfterClose.burnTime() > 0 && furnaceSavedAfterClose.cookTime() > 0;
                queueCapture("portable_furnace_inventory_cooking_persistence", "after", "portable_furnace_inventory_cooking_persistence", "same item reopened; server menu inventory restored; saved burn/cook values retained");
                ResultsAdd("portable_furnace_inventory_cooking_persistence", cookingPersisted ? "PASS" : "FAIL", "synthetic_client_game_packet",
                        "same_item_server_close_reopen=true input_restored=" + inputRestored + " fuel_restored=" + fuelRestored
                                + " burn_time=" + furnaceSavedAfterClose.burnTime() + " cook_time=" + furnaceSavedAfterClose.cookTime()
                                + "; client state came from server menu and synced item component", List.of());
                furnaceStage = 6;
                waitForCapture(client, () -> client.player.closeContainer());
                return;
            }
        } catch (Throwable t) {
            ResultsAdd("portable_furnace_inventory_cooking_persistence", "FAIL", "synthetic_client_game_packet", "in-world Portable Furnace gameplay exception=" + t, List.of());
        }
    }

    private static int findInventoryItem(Minecraft client, net.minecraft.world.item.Item target) {
        for (int slot = 0; slot < client.player.getInventory().getContainerSize(); slot++)
            if (client.player.getInventory().getItem(slot).is(target)) return slot;
        return -1;
    }

    private static int findMenuStack(AbstractContainerMenu menu, net.minecraft.world.item.Item target, int start) {
        for (int slot = start; slot < menu.slots.size(); slot++)
            if (menu.getSlot(slot).getItem().is(target)) return slot;
        return -1;
    }

    private static int findEmptyPlayerMenuSlot(AbstractContainerMenu menu, int start) {
        for (int slot = start; slot < menu.slots.size(); slot++)
            if (menu.getSlot(slot).getItem().isEmpty()) return slot;
        throw new IllegalStateException("No free player inventory slot for the Backpack movement assertion");
    }

    private static int countMenuItem(AbstractContainerMenu menu, net.minecraft.world.item.Item item) {
        int count = 0;
        for (int slot = 0; slot < menu.slots.size(); slot++) {
            ItemStack stack = menu.getSlot(slot).getItem();
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }

    private static void finishRemainingProbes(Minecraft client) {
        try {
            boolean sword = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("moreminecrafts", "flame_sword")) == ModItems.FLAME_SWORD
                    && client.getResourceManager().getResource(Identifier.fromNamespaceAndPath("moreminecrafts", "models/item/flame_sword.json")).isPresent()
                    && client.getResourceManager().getResource(Identifier.fromNamespaceAndPath("moreminecrafts", "textures/item/flame_sword.png")).isPresent();
            ResultsAdd("flame_sword_front_back_model_texture", sword ? "LIVE PENDING" : "FAIL", "none",
                    "joined_world=true item_model_texture_resources_present=" + sword + "; rendered front/back orientation requires human review", List.of());
        } catch (Throwable t) { ResultsAdd("flame_sword_front_back_model_texture", "FAIL", "none", "resource check exception=" + t, List.of()); }
        ResultsAdd("greatsword_hit_miss_sweep_particles", "LIVE PENDING", "none", "joined_world=true; hit/miss combat and rendered VFX not exercised", List.of());
        int[][] sizes = {{1280, 720}, {854, 480}, {640, 360}};
        boolean fit = true;
        for (int[] size : sizes) {
            ViewportScale scale = ViewportScale.fit(size[0], size[1], 330, 360, 8);
            fit &= scale.scale() > 0 && scale.scale() <= 1 && scale.left() >= 0 && scale.top() >= 0;
        }
        ResultsAdd("enchanting_gui_multiple_scales", fit ? "LIVE PENDING" : "FAIL", "none",
                "joined_world=true layout_bounds_fit=" + fit + " sizes=1280x720,854x480,640x360; rendered GUI review remains pending", List.of());
        boolean lamb = FabricLoader.getInstance().isModLoaded("lambdynamiclights");
        ResultsAdd("miner_lantern_lambdynamiclights", lamb ? "LIVE PENDING" : "BLOCKED", "none",
                lamb ? "joined_world=true LambDynamicLights installed; lantern field needs rendered validation"
                        : "joined_world=true LambDynamicLights runtime not installed; only this optional scenario is blocked", List.of());
        writeLedger(client);
        suiteFinished = true;
    }

    private static void ResultsAdd(String id, String status, String inputPath, String evidence, List<String> screenshots) {
        RESULTS.add(new Result(id, status, inputPath, evidence, screenshots));
        LOGGER.info("STAGE23B_QA_SCENARIO id={} status={} input_path={} evidence={}", id, status, inputPath, evidence);
    }

    private static void finishBlocked(String reason) {
        if (suiteFinished) return;
        for (String scenario : SCENARIOS) ResultsAdd(scenario, "BLOCKED", "none", reason, List.of());
        writeLedger(Minecraft.getInstance());
        suiteFinished = true;
    }

    private static void writeLedger(Minecraft client) {
        Path report = client.gameDirectory.toPath().resolve("client-qa-results").resolve("scenario-results.jsonl");
        try {
            Files.createDirectories(report.getParent());
            StringBuilder rows = new StringBuilder();
            for (Result result : RESULTS) {
                rows.append("{\"scenario\":\"").append(escape(result.id()))
                    .append("\",\"status\":\"").append(result.status())
                    .append("\",\"visual_status\":\"LIVE PENDING\",\"input_path\":\"").append(escape(result.inputPath()))
                    .append("\",\"os_xtest_xdotool\":\"NOT RUN\",\"synthetic_screen_callback\":\"")
                    .append(result.inputPath().equals("synthetic_screen_callback") ? "EXECUTED" : "NOT RUN")
                    .append("\",\"direct_state_fixture\":\"").append(result.inputPath().equals("direct_state_fixture") ? "EXECUTED" : "NOT RUN")
                    .append("\",\"human_manual_play\":\"NOT RUN\",\"evidence\":\"").append(escape(result.evidence()))
                    .append("\",\"screenshots\":[");
            List<String> images = SCREENSHOTS.getOrDefault(result.id(), List.of());
            for (int i = 0; i < images.size(); i++) {
                if (i > 0) rows.append(',');
                rows.append('"').append(escape(images.get(i))).append('"');
            }
                rows.append("]}").append('\n');
            }
            Files.writeString(report, rows.toString());
            LOGGER.info("STAGE23B_QA_RESULT_FILE path={} scenario_count={} client_world={} player={}", report.toAbsolutePath(), RESULTS.size(), client.level != null, client.player != null);
        } catch (Exception e) { LOGGER.error("STAGE23B_QA_RESULT_WRITE_FAILED path={}", report, e); }
    }

    private static String escape(String text) { return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r"); }

    private record Result(String id, String status, String inputPath, String evidence, List<String> screenshots) { }
    private record Capture(String caseId, String phase, String scenario, String note, CaptureCallback callback) {
        void complete(boolean success, String path) { callback.done(success, path); }
    }
    @FunctionalInterface private interface CaptureCallback { void done(boolean success, String path); }
}
