package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ProgressionSectorGeometryTest {
    @Test
    void referenceLayoutDefinesSevenOfficialDisciplinesInProjectOrder() {
        var sectors = ProgressionSectorGeometry.sectors();

        assertEquals(7, sectors.size());
        assertEquals("resources", sectors.get(0).discipline());
        assertEquals("production", sectors.get(1).discipline());
        assertEquals("living", sectors.get(2).discipline());
        assertEquals("combat", sectors.get(3).discipline());
        assertEquals("ecology", sectors.get(4).discipline());
        assertEquals("world", sectors.get(5).discipline());
        assertEquals("alchemy", sectors.get(6).discipline());
    }

    @Test
    void eachSectorCentroidMapsToItsOwnDisciplineAfterLayoutScaling() {
        var sectors = ProgressionSectorGeometry.sectors();
        double originX = 42.0;
        double originY = 17.0;
        double scale = 0.75;

        for (int index = 0; index < sectors.size(); index++) {
            var center = sectors.get(index).centroid();
            int hit = ProgressionSectorGeometry.hitTest(originX + center.x() * scale,
                    originY + center.y() * scale, originX, originY, scale);
            assertEquals(index, hit, "sector centroid should resolve to its matching discipline");
        }
    }

    @Test
    void clicksOutsideTheSevenSectorPolygonsDoNotSelectADiscipline() {
        assertEquals(-1, ProgressionSectorGeometry.hitTest(12, 12, 0, 0, 1));
        assertEquals(-1, ProgressionSectorGeometry.hitTest(620, 340, 0, 0, 1));
    }

    @Test
    void hoverUsesTheSameSectorHitAreasAsClick() {
        var sector = ProgressionSectorGeometry.sectors().get(5);
        var center = sector.centroid();
        var hovered = ProgressionSectorGeometry.sectorAt(center.x(), center.y(), 0, 0, 1);

        assertNotNull(hovered);
        assertEquals("world", hovered.discipline());
        assertTrue(hovered.contains(center.x(), center.y()));
    }
}
