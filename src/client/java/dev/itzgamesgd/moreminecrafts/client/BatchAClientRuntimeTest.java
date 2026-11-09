package dev.itzgamesgd.moreminecrafts.client;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

/** Opt-in real-client Screen-callback test. Deliberately does not claim physical OS input or natural play. */
final class BatchAClientRuntimeTest {
    private static final String QUEST = "xpq.cmb.hostile_100";
    private static int ticks, phase, xpBefore, reward;
    private static boolean failed, requested;
    static void tick(Minecraft client) {
        if (failed || ++ticks % 20 != 0 || phase == 15) return;
        try {
            switch (phase) {
                case 0 -> client.gui.setScreen(new ProgressionSelectionScreen());
                case 1 -> { capture(client,"batch-a-selector.png"); selector(client).validationClick("knowledge"); }
                case 2 -> sectors(client).validationOpenSector("resources");
                case 3 -> {
                    var tree = tree(client);
                    check(tree.mode() == ProgressionBrowseScreen.Mode.KNOWLEDGE && tree.validationTreeReady(),"Knowledge screen/data");
                    check(ProgressionClientApi.knowledge("resources").size() == 10,"Knowledge resources catalog");
                    capture(client,"batch-a-knowledge.png");
                    tree.onClose(); check(sectors(client).mode() == ProgressionBrowseScreen.Mode.KNOWLEDGE,"Knowledge Back");
                    check(sectors(client).validationVisitAllSectors(),"all Knowledge sectors"); sectors(client).onClose();
                }
                case 4 -> selector(client).validationClick("research");
                case 5 -> sectors(client).validationOpenSector("production");
                case 6 -> {
                    var tree = tree(client);
                    check(tree.mode() == ProgressionBrowseScreen.Mode.RESEARCH && tree.validationTreeReady(),"Research screen/data");
                    capture(client,"batch-a-research.png"); tree.onClose();
                    check(sectors(client).validationVisitAllSectors(),"all Research sectors"); sectors(client).onClose();
                }
                case 7 -> selector(client).validationClick("xp_quest");
                case 8 -> { check(tree(client).validationCanvas().nodes().size() == 75,"75-node production tree"); capture(client,"batch-a-quest-tree.png"); }
                case 9 -> {
                    var row = row();
                    if (!row.complete()) {
                        if (!requested) MoreMinecrafts.LOGGER.info("BATCH_A_OBJECTIVE_FIXTURE_REQUIRED quest={} xp_before={}",QUEST,ProgressionClientApi.availableXp());
                        requested = true; return;
                    }
                    check(!row.claimed(),"fixture must start unclaimed");
                    xpBefore = ProgressionClientApi.availableXp(); reward = row.rewardRawXp();
                    tree(client).validationCanvas().select(QUEST,true);
                    tree(client).validationCanvas().setSelectionTooltip(true);
                }
                case 10 -> capture(client,"batch-a-completed-unclaimed.png");
                case 11 -> check(tree(client).validationClickNode(QUEST),"completed node click");
                case 12 -> {
                    if (!row().claimed()) return;
                    if (ProgressionClientApi.availableXp() != xpBefore + reward) return;
                    capture(client,"batch-a-claimed.png");
                    tree(client).validationClickNode(QUEST);
                    check(!ProgressionClientApi.claim(QUEST),"claimed UI cannot request another reward");
                    net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                            new dev.itzgamesgd.moreminecrafts.network.QuestClaimRequestPayload(QUEST));
                }
                case 13 -> {
                    check(row().claimed() && ProgressionClientApi.availableXp() == xpBefore + reward,"repeat claim cannot repay");
                    check(ProgressionClientApi.lastActionResult() == dev.itzgamesgd.moreminecrafts.progression.QuestProgressService.ClaimResult.ALREADY_CLAIMED.ordinal(),"server duplicate rejection");
                    tree(client).onClose(); selector(client);
                }
                case 14 -> MoreMinecrafts.LOGGER.info("BATCH_A_RUNTIME_PASS input=Screen-callbacks physical=false knowledge_sectors=7 research_sectors=7 quest_nodes=75 xp_before={} reward={} xp_after={} claimed=true repeat_rejected=true",xpBefore,reward,ProgressionClientApi.availableXp());
                default -> { }
            }
            phase++;
        } catch (RuntimeException failure) {
            failed = true; MoreMinecrafts.LOGGER.error("BATCH_A_RUNTIME_FAIL phase={}",phase,failure);
        }
    }
    private static dev.itzgamesgd.moreminecrafts.progression.ProgressionScreenData.QuestRow row() {
        return ProgressionClientApi.quests(null).stream().filter(q -> q.id().equals(QUEST)).findFirst().orElseThrow();
    }
    private static ProgressionSelectionScreen selector(Minecraft c) { return (ProgressionSelectionScreen)c.gui.screen(); }
    private static ProgressionDisciplineSelectionScreen sectors(Minecraft c) { return (ProgressionDisciplineSelectionScreen)c.gui.screen(); }
    private static ProgressionBrowseScreen tree(Minecraft c) { return (ProgressionBrowseScreen)c.gui.screen(); }
    private static void check(boolean ok,String message) { if (!ok) throw new IllegalStateException(message); }
    private static void capture(Minecraft c,String name) {
        Screenshot.grab(c.gameDirectory,name,c.gameRenderer.mainRenderTarget(),1,message -> MoreMinecrafts.LOGGER.info("BATCH_A_CAPTURE {}",name));
    }
}
