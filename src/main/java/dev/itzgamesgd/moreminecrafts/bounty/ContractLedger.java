package dev.itzgamesgd.moreminecrafts.bounty;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** World ledger for contract IDs that have already been redeemed. */
final class ContractLedger extends SavedData {
    static final Codec<ContractLedger> CODEC = Codec.STRING.listOf().xmap(ContractLedger::fromList,
            ledger -> new ArrayList<>(ledger.redeemed));
    static final SavedDataType<ContractLedger> TYPE = new SavedDataType<>(
            Identifier.parse("moreminecrafts:contract_redemptions"), ContractLedger::new, CODEC, DataFixTypes.LEVEL);

    private final Set<String> redeemed = new HashSet<>();

    private static ContractLedger fromList(java.util.List<String> ids) {
        ContractLedger ledger = new ContractLedger();
        ledger.redeemed.addAll(ids);
        return ledger;
    }

    boolean contains(String id) { return redeemed.contains(id); }

    boolean claim(String id) {
        if (!redeemed.add(id)) return false;
        setDirty();
        return true;
    }
}
