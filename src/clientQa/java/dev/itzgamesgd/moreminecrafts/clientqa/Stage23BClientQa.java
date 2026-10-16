package dev.itzgamesgd.moreminecrafts.clientqa;

import dev.itzgamesgd.moreminecrafts.client.ProgressionSelectionScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
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
                String status = scenario.equals("four_card_selector") ? selectorStatus : "LIVE PENDING";
                String inputPath = scenario.equals("four_card_selector") ? "synthetic_screen_callback" : "none";
                String evidence = scenario.equals("four_card_selector")
                        ? selectorEvidence
                        : "not exercised: no scenario-specific in-client fixture or semantic observer is available";
                if (scenario.equals("four_card_selector")) {
                    LOGGER.info("STAGE23B_QA_SCENARIO id={} status={} input_path={} evidence={}",
                            scenario, status, inputPath, selectorEvidence);
                } else {
                    LOGGER.info("STAGE23B_QA_SCENARIO id={} status={} input_path={} evidence={}",
                            scenario, status, inputPath, evidence);
                }
                rows.append("{\"scenario\":\"").append(scenario)
                        .append("\",\"status\":\"").append(status)
                        .append("\",\"input_path\":\"").append(inputPath)
                        .append("\",\"evidence\":\"").append(jsonEscape(evidence)).append("\"}\n");
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
}
