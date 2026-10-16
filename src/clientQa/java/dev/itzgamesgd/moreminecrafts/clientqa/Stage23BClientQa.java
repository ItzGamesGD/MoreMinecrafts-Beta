package dev.itzgamesgd.moreminecrafts.clientqa;

import dev.itzgamesgd.moreminecrafts.client.ProgressionSelectionScreen;
import dev.itzgamesgd.moreminecrafts.equipment.BackpackItem;
import dev.itzgamesgd.moreminecrafts.equipment.BackpackStorage;
import dev.itzgamesgd.moreminecrafts.equipment.PortableFurnaceContents;
import dev.itzgamesgd.moreminecrafts.progression.ViewportScale;
import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Test-only client QA entrypoint. All unverified gameplay outcomes remain LIVE PENDING. */
public final class Stage23BClientQa implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("MoreMinecrafts-ClientQA");
    private static final List<String> SCENARIOS = List.of(
            "four_card_selector",
            "large_backpack_icon_and_movement",
            "flame_sword_front_back_model_texture",
            "greatsword_hit_miss_sweep_particles",
            "enchanting_gui_multiple_scales",
            "portable_furnace_inventory_cooking_persistence",
            "miner_lantern_lambdynamiclights"
    );
    private static int ticks;
    private static boolean suiteStarted;
    private static boolean suiteFinished;

    @Override
    public void onInitializeClient() {
        if (!"1".equals(System.getenv("MOREMINECRAFTS_CLIENT_QA"))) return;
        LOGGER.info("STAGE23B_QA_MOD_LOADED mod=moreminecrafts_client_qa");
        ClientTickEvents.END_CLIENT_TICK.register(Stage23BClientQa::tick);
    }

    private static void tick(Minecraft client) {
        if (suiteFinished || ++ticks < 30) return;
        suiteFinished = true;
        if (suiteStarted) return;
        suiteStarted = true;

        String selectorEvidence = "synthetic Screen.mouseClicked callback not executed: no client interaction adapter available";
        String selectorStatus = "LIVE PENDING";
        ProgressionSelectionScreen selector = new ProgressionSelectionScreen();
        client.gui.setScreen(selector);
        boolean disabledQuestConsumed = selector.validationClick("xp_quest");
        boolean questStayedOnSelector = client.gui.screen() == selector;
        boolean knowledgeConsumed = selector.validationClick("knowledge");
        boolean knowledgeTransitioned = client.gui.screen() != selector;
        if (disabledQuestConsumed && questStayedOnSelector && knowledgeConsumed && knowledgeTransitioned) {
            selectorEvidence = "synthetic_screen_callback disabled_quest_consumed=true stayed_on_selector=true knowledge_consumed=true transitioned=true";
        } else {
            selectorEvidence = "synthetic_screen_callback disabled_quest_consumed=" + disabledQuestConsumed
                    + " stayed_on_selector=" + questStayedOnSelector
                    + " knowledge_consumed=" + knowledgeConsumed
                    + " transitioned=" + knowledgeTransitioned;
            selectorStatus = "FAIL";
            LOGGER.error("STAGE23B_QA_SELECTOR_CALLBACK_FAILED evidence={}", selectorEvidence);
        }

        Path report = client.gameDirectory.toPath().resolve("client-qa-results").resolve("scenario-results.jsonl");
        try {
            Files.createDirectories(report.getParent());
            StringBuilder rows = new StringBuilder();
            for (String scenario : SCENARIOS) {
                ScenarioProbe probe;
                try {
                    probe = switch (scenario) {
                        case "four_card_selector" -> new ScenarioProbe(selectorStatus.equals("FAIL") ? "FAIL" : "PASS",
                                "synthetic_screen_callback", selectorEvidence);
                        case "large_backpack_icon_and_movement" -> probeBackpack();
                        case "flame_sword_front_back_model_texture" -> probeFlameSword(client);
                        case "greatsword_hit_miss_sweep_particles" -> probeGreatsword();
                        case "enchanting_gui_multiple_scales" -> probeEnchantingScales();
                        case "portable_furnace_inventory_cooking_persistence" -> probePortableFurnace(client);
                        case "miner_lantern_lambdynamiclights" -> probeLantern();
                        default -> new ScenarioProbe("LIVE PENDING", "none", "unknown scenario");
                    };
                } catch (Throwable error) {
                    probe = new ScenarioProbe("FAIL", "none", "scenario harness exception=" + error);
                    LOGGER.error("STAGE23B_QA_SCENARIO_EXCEPTION id={}", scenario, error);
                }
                String status = probe.fixtureStatus().equals("FAIL") ? "FAIL"
                        : probe.fixtureStatus().equals("BLOCKED") ? "BLOCKED" : "LIVE PENDING";
                String defectWatch = switch (scenario) {
                    case "four_card_selector" -> "known click-inability defect remains unconfirmed by OS-level input";
                    case "large_backpack_icon_and_movement" -> "known transparent-icon defect remains unconfirmed by semantic rendering";
                    default -> "none recorded";
                };
                LOGGER.info("STAGE23B_QA_SCENARIO id={} status={} fixture_status={} input_path={} evidence={}",
                        scenario, status, probe.fixtureStatus(), probe.inputPath(), probe.evidence());
                rows.append("{\"scenario\":\"").append(scenario)
                        .append("\",\"status\":\"").append(status)
                        .append("\",\"visual_status\":\"LIVE PENDING\"")
                        .append(",\"fixture_status\":\"").append(probe.fixtureStatus()).append("\"")
                        .append(",\"input_path\":\"").append(probe.inputPath()).append("\"")
                        .append(",\"os_xtest_xdotool\":\"NOT RUN\"")
                        .append(",\"direct_state_fixture\":\"")
                        .append(probe.inputPath().equals("direct_state_fixture") ? probe.fixtureStatus() : "NOT RUN")
                        .append("\"")
                        .append(",\"human_manual_play\":\"NOT RUN\"")
                        .append(",\"known_defect_watch\":\"").append(jsonEscape(defectWatch)).append("\"")
                        .append(",\"evidence\":\"").append(jsonEscape(probe.evidence())).append("\"}").append('\n');
            }
            Files.writeString(report, rows.toString());
            LOGGER.info("STAGE23B_QA_RESULT_FILE path={} scenario_count={}", report.toAbsolutePath(), SCENARIOS.size());
        } catch (Exception error) {
            LOGGER.error("STAGE23B_QA_RESULT_WRITE_FAILED path={}", report.toAbsolutePath(), error);
        }
    }

    private static String jsonEscape(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }

    private static ScenarioProbe probeBackpack() {
        try {
            if (!(ModItems.LARGE_BACKPACK instanceof BackpackItem item) || item.capacity() != 54) {
                return new ScenarioProbe("FAIL", "direct_state_fixture", "Large Backpack registration/capacity assertion failed");
            }
            BackpackStorage fixture = BackpackStorage.empty();
            boolean capacityMatches = fixture.items().getContainerSize() == 54;
            return new ScenarioProbe(capacityMatches ? "PASS" : "FAIL", "direct_state_fixture",
                    "direct_state_fixture capacity=54 storage_slots=" + fixture.items().getContainerSize()
                            + "; item movement and icon transparency remain unverified");
        } catch (RuntimeException | AssertionError error) {
            return new ScenarioProbe("FAIL", "direct_state_fixture", "backpack fixture error=" + error);
        }
    }

    private static ScenarioProbe probeFlameSword(Minecraft client) {
        try {
            boolean item = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("moreminecrafts", "flame_sword"))
                    == ModItems.FLAME_SWORD;
            boolean model = client.getResourceManager().getResource(
                    Identifier.fromNamespaceAndPath("moreminecrafts", "models/item/flame_sword.json")).isPresent();
            boolean texture = client.getResourceManager().getResource(
                    Identifier.fromNamespaceAndPath("moreminecrafts", "textures/item/flame_sword.png")).isPresent();
            boolean valid = item && model && texture;
            return new ScenarioProbe(valid ? "PASS" : "FAIL", "none",
                    "client_resource_preflight item_registered=" + item + " model_present=" + model
                            + " texture_present=" + texture
                            + "; first/third-person front/back visual orientation remains LIVE PENDING");
        } catch (RuntimeException | AssertionError error) {
            return new ScenarioProbe("FAIL", "none", "Flame Sword resource preflight error=" + error);
        }
    }

    private static ScenarioProbe probeGreatsword() {
        boolean registered = !ModItems.GREATSWORDS.isEmpty();
        return new ScenarioProbe(registered ? "PASS" : "FAIL", "none",
                "client_registry_preflight greatsword_tier_count=" + ModItems.GREATSWORDS.size()
                        + "; hit/miss health, durability and rendered sweep particles remain LIVE PENDING");
    }

    private static ScenarioProbe probeEnchantingScales() {
        int[][] sizes = {{1280, 720}, {854, 480}, {640, 360}};
        for (int[] size : sizes) {
            ViewportScale fit = ViewportScale.fit(size[0], size[1], 330, 360, 8);
            if (!(fit.scale() > 0 && fit.scale() <= 1 && fit.left() >= 0 && fit.top() >= 0)) {
                return new ScenarioProbe("FAIL", "none", "viewport_bounds_failed size=" + size[0] + "x" + size[1]);
            }
        }
        return new ScenarioProbe("PASS", "none",
                "layout_preflight sizes=1280x720,854x480,640x360 all_fit=true; actual screen clipping remains LIVE PENDING");
    }

    private static ScenarioProbe probePortableFurnace(Minecraft client) {
        if (client.level == null || client.player == null) {
            return new ScenarioProbe("BLOCKED", "direct_state_fixture",
                    "portable furnace ItemStack component fixture requires an in-world client; client is at title screen");
        }
        ItemStack opener = new ItemStack(ModItems.PORTABLE_FURNACE);
        PortableFurnaceContents fixture = new PortableFurnaceContents(
                List.of(new ItemStack(Items.RAW_IRON, 2), ItemStack.EMPTY, ItemStack.EMPTY), 99, 200, 17, 200);
        opener.set(ModDataComponents.PORTABLE_FURNACE_CONTENTS, fixture);
        PortableFurnaceContents observed = opener.get(ModDataComponents.PORTABLE_FURNACE_CONTENTS);
        boolean retained = observed != null && observed.items().get(0).getCount() == 2
                && observed.burnTime() == 99 && observed.cookTime() == 17;
        return new ScenarioProbe(retained ? "PASS" : "FAIL", "direct_state_fixture",
                "direct_state_fixture input=raw_ironx2 burn_time=99 cook_time=17 observed=" + retained
                        + "; close/reopen save restoration remains LIVE PENDING");
    }

    private static ScenarioProbe probeLantern() {
        if (!FabricLoader.getInstance().isModLoaded("lambdynamiclights")) {
            return new ScenarioProbe("BLOCKED", "none",
                    "LambDynamicLights runtime mod is not installed in this QA client; lighting checks were skipped independently");
        }
        boolean lanternRegistered = ModItems.MINER_LANTERN != null;
        return new ScenarioProbe(lanternRegistered ? "PASS" : "FAIL", "direct_state_fixture",
                "client_registry_preflight lantern_registered=" + lanternRegistered
                        + "; attached behavior/light-field visualization remains LIVE PENDING");
    }

    private record ScenarioProbe(String fixtureStatus, String inputPath, String evidence) { }
}
