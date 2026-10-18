package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

final class XpQuestCatalogDataTest {
    private static final XpQuestCatalogData.Catalog CATALOG = XpQuestCatalogData.catalog();

    @Test void approvedLedgerAndClassificationMatchDesignCatalogA() {
        assertTrue(XpQuestCatalogData.validate(CATALOG).isEmpty(), () -> XpQuestCatalogData.validate(CATALOG).toString());
        assertEquals(200, CATALOG.quests().size());
        assertEquals(200, new HashSet<>(CATALOG.quests().stream().map(XpQuestCatalogData.Spec::id).toList()).size());
        assertEquals(156_050, CATALOG.total("NOW"));
        assertEquals(23_950, CATALOG.total("VERIFY"));
        assertEquals(29_100, CATALOG.total("FUTURE"));
        assertEquals(180_000, CATALOG.total("NOW") + CATALOG.total("VERIFY"));
        assertEquals(209_100, CATALOG.total("NOW") + CATALOG.total("VERIFY") + CATALOG.total("FUTURE"));
        assertEquals(160, count("NOW"));
        assertEquals(20, count("VERIFY"));
        assertEquals(20, count("FUTURE"));
    }

    /** Q1-B registers exactly the quests whose live detection has its own GameTest. */
    static final java.util.Set<String> Q1B_REGISTERED = java.util.Set.of(
            "xpq.bld.place_1000", "xpq.bld.place_10000", "xpq.bld.place_50000", "xpq.bld.place_200000",
            "xpq.bld.variety_50", "xpq.bld.variety_150",
            "xpq.min.ore_1000", "xpq.min.ore_5000", "xpq.min.ore_20000", "xpq.min.diamond_ore_64",
            "xpq.min.debris_16", "xpq.min.adamantium_10", "xpq.min.first_custom_ore", "xpq.min.custom_ore_survey",
            "xpq.min.deepslate_custom", "xpq.min.logs_1000", "xpq.min.pickaxe_5000",
            "xpq.frm.tame_5", "xpq.frm.till_1000", "xpq.frm.barter_200",
            "xpq.dim.nether_travel_50k", "xpq.fsh.first_sale", "xpq.fsh.sale_64", "xpq.fsh.emerald_500",
            "xpq.cft.anvil_50", "xpq.cft.redstone_100",
            "xpq.exp.archaeology_30", "xpq.cmb.shield_500", "xpq.cmb.trident_50", "xpq.alc.potions_250");
    static final java.util.Set<String> Q1B_SOURCE_BLOCKED = java.util.Set.of(
            "xpq.frm.harvest_1000", "xpq.frm.harvest_5000", "xpq.fsh.catch_500",
            "xpq.frm.composite_foods", "xpq.cft.sleeping_bag", "xpq.cft.parachute",
            "xpq.cft.all_daggers", "xpq.cft.all_greatswords", "xpq.cft.mm_blocks");
    static final java.util.Set<String> Q1C_LOOT_REGISTERED = java.util.Set.of(
            "xpq.exp.first_structure_loot", "xpq.exp.mineshaft_loot", "xpq.exp.dungeon_loot",
            "xpq.exp.temples", "xpq.exp.ocean_relics", "xpq.dim.fortress_loot",
            "xpq.dim.ancient_city_loot", "xpq.dim.bastion_all", "xpq.dim.end_city_loot_10");
    static final java.util.Set<String> Q1C_STRUCTURE_REGISTERED = java.util.Set.of(
            "xpq.dim.end_cities_3", "xpq.exp.villages_5", "xpq.exp.structures_15", "xpq.exp.grand_expedition");
    static final java.util.Set<String> Q1C_COMBAT_REGISTERED = java.util.Set.of(
            "xpq.cmb.hostile_100", "xpq.cmb.hostile_500", "xpq.cmb.hostile_2000", "xpq.cmb.hostile_5000",
            "xpq.cmb.vanilla_species_20", "xpq.cmb.bestiary_5", "xpq.cmb.bestiary_10", "xpq.cmb.breeze_10",
            "xpq.cmb.guardians_30", "xpq.cmb.lesser_wither_10", "xpq.cmb.ravagers_5", "xpq.cmb.brutes_3",
            "xpq.boss.elder_guardian", "xpq.boss.warden", "xpq.boss.wither", "xpq.boss.warden_3",
            "xpq.boss.twin_bosses", "xpq.cmb.mm_arms", "xpq.cmb.dagger_100", "xpq.cmb.greatsword_100",
            "xpq.cmb.netherite_blades", "xpq.cmb.bandits_20", "xpq.cmb.long_shot_25", "xpq.cmb.mace_smash_10");
    static final java.util.Set<String> Q1C_VAULT_REGISTERED = java.util.Set.of(
            "xpq.dim.trial_vaults_5", "xpq.dim.ominous_vaults_5");

    @Test void onlyQuestsWithTheirOwnTestGateAreRegistered() {
        java.util.Set<String> registered = new java.util.TreeSet<>();
        CATALOG.quests().stream().filter(XpQuestCatalogData.Spec::registered).forEach(q -> registered.add(q.id()));
        java.util.Set<String> approved = new java.util.TreeSet<>(Q1B_REGISTERED);
        approved.addAll(Q1C_LOOT_REGISTERED);
        approved.addAll(Q1C_VAULT_REGISTERED);
        approved.addAll(Q1C_STRUCTURE_REGISTERED);
        approved.addAll(Q1C_COMBAT_REGISTERED);
        assertEquals(approved, registered);
        assertTrue(Q1B_SOURCE_BLOCKED.stream().allMatch(id -> !spec(id).registered()));
        assertTrue(CATALOG.quests().stream().filter(XpQuestCatalogData.Spec::registered)
                .allMatch(q -> q.status().equals("NOW") && XpQuestCatalogData.EVALUABLE_KINDS.contains(q.tracker().kind())));
    }

    @Test void setQuestTargetsMatchTheirEntryLists() {
        assertEquals(4, spec("xpq.min.custom_ore_survey").target());
        assertEquals(4, spec("xpq.min.deepslate_custom").target());
        assertEquals(5, spec("xpq.frm.composite_foods").target());
        assertEquals(7, spec("xpq.cft.all_daggers").target());
        assertEquals(7, spec("xpq.cft.all_greatswords").target());
        assertEquals(5, spec("xpq.cft.mm_blocks").target());
        for (XpQuestCatalogData.Spec q : CATALOG.quests()) {
            if (!q.registered()) continue;
            String kind = q.tracker().kind();
            if (kind.equals("distinct_groups") || kind.equals("stat_crafted_distinct")
                    || (kind.equals("distinct_set") && !q.tracker().entries().isEmpty()))
                assertTrue(q.tracker().entries().size() >= q.target(), q.id() + " has enough entries for its target");
        }
    }

    @Test void representativeRewardsMatchCatalogA() {
        assertEquals(3_150, spec("xpq.bld.place_200000").rewardRawXp());
        assertEquals(200_000, spec("xpq.bld.place_200000").target());
        assertEquals("VERIFY", spec("xpq.boss.monument_clear").status());
        assertEquals("NOW", spec("xpq.alc.shock_potion").status());
        assertEquals("counter", spec("xpq.min.ore_1000").tracker().kind(), "verified-natural ore counter (Q1-B)");
        assertEquals("natural_ore_mined", spec("xpq.min.ore_1000").tracker().key());
        assertEquals("crops_harvested", spec("xpq.frm.harvest_1000").tracker().seedCondition());
    }

    @Test void validatorRejectsLedgerAndRegistrationViolations() {
        List<XpQuestCatalogData.Spec> quests = new ArrayList<>(CATALOG.quests());
        XpQuestCatalogData.Spec first = quests.get(0);
        quests.set(1, new XpQuestCatalogData.Spec(first.id(), first.tab(), first.tier(), first.status(),
                quests.get(1).rewardRawXp(), first.target(), first.tracker(), false));
        assertTrue(problems(quests).stream().anyMatch(p -> p.startsWith("duplicate id")));

        quests = new ArrayList<>(CATALOG.quests());
        XpQuestCatalogData.Spec q = quests.get(0);
        quests.set(0, new XpQuestCatalogData.Spec(q.id(), q.tab(), q.tier(), q.status(), q.rewardRawXp() + 25, q.target(), q.tracker(), false));
        List<String> broken = problems(quests);
        assertTrue(broken.stream().anyMatch(p -> p.contains("reward")) && broken.stream().anyMatch(p -> p.startsWith("active ledger")));

        quests = new ArrayList<>(CATALOG.quests());
        int verify = indexOf(quests, "VERIFY");
        q = quests.get(verify);
        quests.set(verify, new XpQuestCatalogData.Spec(q.id(), q.tab(), q.tier(), q.status(), q.rewardRawXp(), q.target(), q.tracker(), true));
        assertTrue(problems(quests).stream().anyMatch(p -> p.contains("registered while VERIFY")));

        quests = new ArrayList<>(CATALOG.quests());
        int pending = -1;
        for (int i = 0; i < quests.size(); i++)
            if (quests.get(i).status().equals("NOW") && !quests.get(i).tracker().evaluable()) { pending = i; break; }
        q = quests.get(pending);
        quests.set(pending, new XpQuestCatalogData.Spec(q.id(), q.tab(), q.tier(), q.status(), q.rewardRawXp(), q.target(), q.tracker(), true));
        assertTrue(problems(quests).stream().anyMatch(p -> p.contains("without an implemented tracker")));
        assertFalse(XpQuestCatalogData.validate(CATALOG).iterator().hasNext());
    }

    private static List<String> problems(List<XpQuestCatalogData.Spec> quests) {
        return XpQuestCatalogData.validate(new XpQuestCatalogData.Catalog(1, 180_000, 29_100, 209_100, quests));
    }
    private static int indexOf(List<XpQuestCatalogData.Spec> quests, String status) {
        for (int i = 0; i < quests.size(); i++) if (quests.get(i).status().equals(status)) return i;
        throw new AssertionError(status);
    }
    private static long count(String status) { return CATALOG.quests().stream().filter(q -> q.status().equals(status)).count(); }
    private static XpQuestCatalogData.Spec spec(String id) {
        return CATALOG.quests().stream().filter(q -> q.id().equals(id)).findFirst().orElseThrow();
    }
}
