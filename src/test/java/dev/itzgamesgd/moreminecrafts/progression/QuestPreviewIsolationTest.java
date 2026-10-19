package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * The 200-node structure preview is test-only. These checks guard that it stays unreachable in normal play:
 * only the visual-validation harness may open it, it is double-gated by environment variables, and it never
 * touches registration, claims or the network.
 */
class QuestPreviewIsolationTest {
    private static final Path CLIENT = Path.of("src/client/java/dev/itzgamesgd/moreminecrafts/client");

    private static List<Path> sources(Path root) throws IOException {
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(p -> p.toString().endsWith(".java")).toList();
        }
    }

    private static Set<String> filesContaining(String needle) throws IOException {
        Set<String> hits = new TreeSet<>();
        for (Path root : List.of(Path.of("src/main/java"), Path.of("src/client/java"), Path.of("src/gametest"))) {
            if (!Files.isDirectory(root)) continue;
            for (Path file : sources(root)) {
                if (Files.readString(file).contains(needle)) hits.add(file.getFileName().toString());
            }
        }
        return hits;
    }

    @Test
    void onlyTheHarnessOpensThePreviewScreen() throws IOException {
        // The factory is defined in the screen and called from the harness; nothing else may call it.
        assertEquals(Set.of("ProgressionBrowseScreen.java", "VisualValidationHarness.java"), filesContaining("forStructurePreview("));
        assertEquals(Set.of("ProgressionBrowseScreen.java", "ProgressionTreeCanvas.java", "QuestStructurePreview.java",
                "VisualValidationHarness.java"), filesContaining("QuestStructurePreview"));
        String screen = Files.readString(CLIENT.resolve("ProgressionBrowseScreen.java"));
        assertTrue(screen.contains("static ProgressionBrowseScreen forStructurePreview"), "factory must be package-private");
        assertFalse(screen.contains("public static ProgressionBrowseScreen forStructurePreview"));
    }

    @Test
    void previewLoadingIsDoubleGatedByEnvironment() throws IOException {
        String preview = Files.readString(CLIENT.resolve("QuestStructurePreview.java"));
        assertTrue(preview.contains("MOREMINECRAFTS_VISUAL_VALIDATION"));
        assertTrue(preview.contains("MOREMINECRAFTS_STRUCTURE_PREVIEW_DIR"));
        assertEquals(Set.of("QuestStructurePreview.java"), filesContaining("MOREMINECRAFTS_STRUCTURE_PREVIEW_DIR"));
        // The proposal is not on the runtime classpath, so normal builds cannot load it.
        assertFalse(Files.exists(Path.of("src/main/resources/data/moreminecrafts/xp_quest/quest_structure_mapping.json")));
    }

    @Test
    void previewCodeNeverTouchesRegistrationClaimsOrTheNetwork() throws IOException {
        for (String name : List.of("QuestStructurePreview.java")) {
            String text = Files.readString(CLIENT.resolve(name));
            for (String forbidden : List.of("ClientPlayNetworking", "ServerPlayer", "XpQuestCatalogData", "QuestProgressService",
                    "claim(", "ProgressionClientApi", "Registry.register", "registered(true")) {
                assertFalse(text.contains(forbidden), name + " must not reference " + forbidden);
            }
        }
        // The browse screen has no claim call at all, so a preview click cannot grant XP.
        assertFalse(Files.readString(CLIENT.resolve("ProgressionBrowseScreen.java")).toLowerCase().contains("claim("));
    }

    @Test
    void previewDoesNotEnableTheGameplayCandidates() throws IOException {
        String loader = Files.readString(CLIENT.resolve("QuestStructurePreview.java"));
        assertTrue(loader.contains("\"presentation-only\".equals("), "only presentation-only edges may be loaded");
    }
}
