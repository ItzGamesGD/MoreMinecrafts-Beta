package dev.itzgamesgd.moreminecrafts.bounty;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Every initialization order must work in a fresh JVM, and the serialized form must match the saved format. */
class ContractDataInitOrderTest {
    private static String run(String order) throws Exception {
        String java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        Process process = new ProcessBuilder(List.of(java, "-cp", System.getProperty("java.class.path"),
                InitOrderProbe.class.getName(), order)).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, process.waitFor(), order + " exited non-zero:\n" + output);
        assertTrue(output.contains("OK {"), order + " did not round-trip:\n" + output);
        return output.lines().filter(l -> l.startsWith("OK ")).findFirst().orElseThrow();
    }

    @Test void everyInitializationOrderWorksInItsOwnJvm() throws Exception {
        String reference = run("contract-first");
        for (String order : List.of("objective-first", "category-first", "generator-first"))
            assertEquals(reference, run(order), order);
    }

    @Test void goldenSavedFormatStillDecodesAndReencodesIdentically() throws Exception {
        // run() already asserts encode(decode(golden)) == golden; the golden string is the pre-fix field layout.
        assertTrue(run("contract-first").contains("\"visited\":[\"a\"]"));
    }
}
