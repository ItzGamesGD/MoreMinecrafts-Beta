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

    @Test void noQuestIsRegisteredBeforeItsOwnTestGate() {
        assertTrue(CATALOG.quests().stream().noneMatch(XpQuestCatalogData.Spec::registered));
    }

    @Test void representativeRewardsMatchCatalogA() {
        assertEquals(3_150, spec("xpq.bld.place_200000").rewardRawXp());
        assertEquals(200_000, spec("xpq.bld.place_200000").target());
        assertEquals("VERIFY", spec("xpq.boss.monument_clear").status());
        assertEquals("NOW", spec("xpq.alc.shock_potion").status());
        assertEquals("pending", spec("xpq.min.ore_1000").tracker().kind(), "ore counts wait for Q1-B provenance");
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
