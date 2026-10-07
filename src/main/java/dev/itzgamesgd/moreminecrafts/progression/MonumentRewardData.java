package dev.itzgamesgd.moreminecrafts.progression;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** World-persistent one-time completion claims, keyed by Ocean Monument start chunk. */
final class MonumentRewardData extends SavedData {
    static final SavedDataType<MonumentRewardData> TYPE = new SavedDataType<>(
            Identifier.parse("moreminecrafts:ocean_monument_rewards"), MonumentRewardData::new,
            Codec.LONG.listOf().xmap(MonumentRewardData::fromList, MonumentRewardData::toList), DataFixTypes.LEVEL);

    private final Set<Long> claimedMonuments = new HashSet<>();

    private static MonumentRewardData fromList(List<Long> values) {
        MonumentRewardData data = new MonumentRewardData();
        data.claimedMonuments.addAll(values);
        return data;
    }

    private List<Long> toList() { return new ArrayList<>(claimedMonuments); }

    boolean claim(long monumentKey) {
        if (!claimedMonuments.add(monumentKey)) return false;
        setDirty();
        return true;
    }
}
