package dev.itzgamesgd.moreminecrafts.spidercolony;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
class ColonyLedgerTest {
 @Test void unloadedChildrenKeepSlotsUntilConfirmedRemoved() {
  var ledger=new ColonyLedger(); var mother=UUID.randomUUID(); var a=UUID.randomUUID();
  ledger.addChild(mother,a); assertEquals(1,ledger.childCount(mother));
  assertEquals(1,ledger.childCount(mother));
  ledger.removeChild(a); assertEquals(0,ledger.childCount(mother));
 }
 @Test void onlyBoundMotherDeathPermanentlyClearsNest() {
  var ledger=new ColonyLedger(); var a=UUID.randomUUID(); var b=UUID.randomUUID();
  assertTrue(ledger.bind("nest",a)); assertFalse(ledger.bind("nest",b));
  ledger.defeat("nest",b); assertFalse(ledger.defeated("nest"));
  ledger.defeat("nest",a); assertTrue(ledger.defeated("nest"));
  assertFalse(ledger.bind("nest",b));
 }
 @Test void savedCodecRoundTripRetainsDefeatAndUnloadedChildren() {
  var ledger=new ColonyLedger(); var mother=UUID.randomUUID(); var child=UUID.randomUUID();
  ledger.bind("nest",mother); ledger.defeat("nest",mother); ledger.addChild(mother,child);
  var encoded=ColonyLedger.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,ledger).getOrThrow();
  var restored=ColonyLedger.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,encoded).getOrThrow();
  assertTrue(restored.defeated("nest")); assertEquals(1,restored.childCount(mother));
 }
}
