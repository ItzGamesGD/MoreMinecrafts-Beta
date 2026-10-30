package dev.itzgamesgd.moreminecrafts.colosseum;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;

/** Round-trips controller state through the same arena codec the saved data uses. */
final class TestArenas {
    private TestArenas() {}

    static final BlockPos CENTER = new BlockPos(0, 64, 0);

    static ColosseumArena arena(ColosseumTrialState state) {
        return new ColosseumArena(ColosseumArena.testInstanceId("unit", CENTER), ColosseumArenaGeometry.testFixture(CENTER), state);
    }

    static JsonElement encode(ColosseumTrialState state) {
        return ColosseumArena.CODEC.encodeStart(JsonOps.INSTANCE, arena(state)).getOrThrow();
    }

    static ColosseumTrialState decode(JsonElement json) {
        return ColosseumArena.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow().state();
    }

    static ColosseumTrialState roundTrip(ColosseumTrialState state) {
        return decode(encode(state));
    }
}
