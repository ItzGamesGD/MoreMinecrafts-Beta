package dev.itzgamesgd.moreminecrafts.skytower;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SkyTowerProgressTest {
    @Test void independentTowersAndDuplicateEvents() {
        var a = new SkyTowerProgress(0); var b = new SkyTowerProgress(0);
        for (int i = 0; i < 4; i++) assertTrue(a.completeVanillaTrial(i));
        assertTrue(a.floorCleared(0)); assertFalse(b.floorCleared(0));
        assertFalse(a.completeVanillaTrial(0)); assertFalse(a.floorCleared(1));
    }
    @Test void completionCannotComeFromMissingMobsOrPartialQuorum() {
        var p = new SkyTowerProgress(0);
        for (int i=0;i<3;i++) p.completeVanillaTrial(i);
        assertFalse(p.floorCleared(0)); assertFalse(p.allFloorsCleared());
    }
    @Test void allFloorsAndRestoreArePermanent() {
        var p = new SkyTowerProgress(0);
        for(int i=0;i<16;i++) p.completeVanillaTrial(i);
        var restored = new SkyTowerProgress(p.completedMask());
        assertTrue(restored.allFloorsCleared());
        for(int i=0;i<16;i++) assertFalse(restored.completeVanillaTrial(i));
    }
    @Test void invalidInputFailsInsteadOfUnlocking() {
        assertThrows(IllegalArgumentException.class,()->new SkyTowerProgress(-1));
        assertThrows(IllegalArgumentException.class,()->new SkyTowerProgress(65536));
        assertThrows(IllegalArgumentException.class,()->new SkyTowerProgress(0).completeVanillaTrial(16));
    }
    @Test void rotationsAndAnchorMembership() {
        for(int r=0;r<4;r++) {
            var site = new SkyTowerLayout.Site("minecraft:overworld",100,70,-200,r);
            for(int i=0;i<16;i++) assertEquals(i, site.spawnerIndex(site.spawner(i)));
            assertEquals(-1,site.spawnerIndex(new SkyTowerLayout.Point(0,0,0)));
            assertNotEquals(site.id(),new SkyTowerLayout.Site("minecraft:the_nether",100,70,-200,r).id());
        }
        var s=new SkyTowerLayout.Site("minecraft:overworld",0,0,0,1);
        assertEquals(new SkyTowerLayout.Point(-17,12,17),s.spawner(0));
    }
}
