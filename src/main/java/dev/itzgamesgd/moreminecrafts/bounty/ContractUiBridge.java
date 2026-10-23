package dev.itzgamesgd.moreminecrafts.bounty;

import java.util.function.Consumer;

/** Lets common code request the client contract screen without referencing client classes. */
public final class ContractUiBridge {
    private static volatile Consumer<String> opener = ignored -> { };

    private ContractUiBridge() {}

    public static void setOpener(Consumer<String> consumer) { opener = consumer; }

    static void open(String contractId) { opener.accept(contractId); }
}
