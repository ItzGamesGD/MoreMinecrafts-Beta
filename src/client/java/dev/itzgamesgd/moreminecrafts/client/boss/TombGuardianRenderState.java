package dev.itzgamesgd.moreminecrafts.client.boss;

import net.minecraft.client.renderer.entity.state.WardenRenderState;

/** Warden-anatomy render state plus the guardian's synced enrage flag (brighter red eyes). */
public class TombGuardianRenderState extends WardenRenderState {
    public boolean enraged;
}
