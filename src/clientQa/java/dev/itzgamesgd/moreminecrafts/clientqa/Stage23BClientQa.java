package dev.itzgamesgd.moreminecrafts.clientqa;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Test-only entrypoint used to prove the isolated Loom QA source set loads. */
public final class Stage23BClientQa implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("MoreMinecrafts-ClientQA");

    @Override
    public void onInitializeClient() {
        if (!"1".equals(System.getenv("MOREMINECRAFTS_CLIENT_QA"))) {
            return;
        }
        LOGGER.info("STAGE23B_QA_MOD_LOADED mod=moreminecrafts_client_qa");
    }
}
