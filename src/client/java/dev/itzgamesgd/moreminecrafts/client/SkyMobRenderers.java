package dev.itzgamesgd.moreminecrafts.client;

/**
 * Renderers for the five Sky Realm normal mobs: Cloud Wraith (P2-B), Skyguard Archer (P2-C), Crystal Bug (P2-D),
 * Cloud Slime (P2-E) and Fallen Sentinel (P2-F1) all have custom models.
 */
final class SkyMobRenderers {
    private SkyMobRenderers() {}

    static void register() {
        CloudWraithRenderer.register();
        FallenSentinelRenderer.register();
        CrystalBugRenderer.register();
        CloudSlimeRenderer.register();
        SkyguardArcherRenderer.register();
    }
}
