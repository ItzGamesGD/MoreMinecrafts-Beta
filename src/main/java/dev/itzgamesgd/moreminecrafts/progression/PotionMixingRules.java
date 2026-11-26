package dev.itzgamesgd.moreminecrafts.progression;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stage 22 §6 Potion Mixing (`v3.research.68`) and Potion Concentration (`v3.research.70`) pure rules:
 * - Both inputs must share delivery type (drinkable / splash / lingering).
 * - Union of distinct effects capped at 4.
 * - Identical effect set & matching amplifiers -> CONCENTRATION (+1 amplifier once within caps, non-instant only, min(d1,d2)*0.70).
 * - Otherwise -> MIX (preserve stronger amplifier, overlapping max(d1,d2)*0.70, non-overlapping d*0.70).
 */
public final class PotionMixingRules {
    public static final int MAX_COMBINED_EFFECTS = 4;
    public static final double DURATION_MULTIPLIER = 0.70D;
    public static final int BAD_OMEN_MAX_AMPLIFIER = 4; // Level V (0-indexed amplifier 4)

    public enum OperationKind { MIX, CONCENTRATE, REJECTED }

    public record EffectSpec(String effectId, int durationTicks, int amplifier,
                             boolean instantaneous, int maxAllowedAmplifier) {}

    public record Result(OperationKind kind, List<EffectSpec> effects, String rejectionReason) {
        public static Result rejected(String reason) {
            return new Result(OperationKind.REJECTED, List.of(), reason);
        }
    }

    private PotionMixingRules() {}

    public static Result resolve(String deliveryA, List<EffectSpec> effectsA, boolean alreadyConcentratedA,
                                 String deliveryB, List<EffectSpec> effectsB, boolean alreadyConcentratedB) {
        if (deliveryA == null || !deliveryA.equals(deliveryB)) {
            return Result.rejected("delivery_type_mismatch");
        }
        if (effectsA == null || effectsA.isEmpty() || effectsB == null || effectsB.isEmpty()) {
            return Result.rejected("empty_potion_effects");
        }

        Map<String, EffectSpec> mapA = toMap(effectsA);
        Map<String, EffectSpec> mapB = toMap(effectsB);

        boolean sameSetAndAmp = mapA.keySet().equals(mapB.keySet()) && mapA.entrySet().stream()
                .allMatch(e -> e.getValue().amplifier() == mapB.get(e.getKey()).amplifier());

        if (sameSetAndAmp) {
            if (alreadyConcentratedA || alreadyConcentratedB) {
                return Result.rejected("already_concentrated");
            }
            if (mapA.size() > MAX_COMBINED_EFFECTS) {
                return Result.rejected("too_many_effects");
            }
            List<EffectSpec> out = new ArrayList<>();
            boolean anyIncreased = false;
            for (var entry : mapA.entrySet()) {
                EffectSpec a = entry.getValue();
                EffectSpec b = mapB.get(entry.getKey());
                int cap = a.effectId().equals("minecraft:bad_omen")
                        ? Math.min(BAD_OMEN_MAX_AMPLIFIER, Math.max(a.maxAllowedAmplifier(), BAD_OMEN_MAX_AMPLIFIER))
                        : a.maxAllowedAmplifier();
                int nextAmp = a.amplifier();
                if (!a.instantaneous() && a.amplifier() < cap) {
                    nextAmp = a.amplifier() + 1;
                    anyIncreased = true;
                }
                int nextDur = a.instantaneous() ? a.durationTicks()
                        : Math.max(1, (int) Math.round(Math.min(a.durationTicks(), b.durationTicks()) * DURATION_MULTIPLIER));
                out.add(new EffectSpec(a.effectId(), nextDur, nextAmp, a.instantaneous(), cap));
            }
            if (!anyIncreased) return Result.rejected("no_effect_can_be_concentrated");
            return new Result(OperationKind.CONCENTRATE, List.copyOf(out), "");
        }

        // MIX path
        Map<String, EffectSpec> combined = new LinkedHashMap<>();
        for (var entry : mapA.entrySet()) {
            String id = entry.getKey();
            EffectSpec a = entry.getValue();
            EffectSpec b = mapB.get(id);
            if (b != null) {
                int strongerAmp = Math.max(a.amplifier(), b.amplifier());
                int dur = a.instantaneous() ? a.durationTicks()
                        : Math.max(1, (int) Math.round(Math.max(a.durationTicks(), b.durationTicks()) * DURATION_MULTIPLIER));
                combined.put(id, new EffectSpec(id, dur, strongerAmp, a.instantaneous(),
                        Math.max(a.maxAllowedAmplifier(), b.maxAllowedAmplifier())));
            } else {
                int dur = a.instantaneous() ? a.durationTicks()
                        : Math.max(1, (int) Math.round(a.durationTicks() * DURATION_MULTIPLIER));
                combined.put(id, new EffectSpec(id, dur, a.amplifier(), a.instantaneous(), a.maxAllowedAmplifier()));
            }
        }
        for (var entry : mapB.entrySet()) {
            if (!combined.containsKey(entry.getKey())) {
                EffectSpec b = entry.getValue();
                int dur = b.instantaneous() ? b.durationTicks()
                        : Math.max(1, (int) Math.round(b.durationTicks() * DURATION_MULTIPLIER));
                combined.put(entry.getKey(), new EffectSpec(b.effectId(), dur, b.amplifier(),
                        b.instantaneous(), b.maxAllowedAmplifier()));
            }
        }
        if (combined.size() > MAX_COMBINED_EFFECTS) {
            return Result.rejected("exceeds_four_effects");
        }
        return new Result(OperationKind.MIX, List.copyOf(combined.values()), "");
    }

    private static Map<String, EffectSpec> toMap(List<EffectSpec> list) {
        Map<String, EffectSpec> map = new LinkedHashMap<>();
        for (EffectSpec e : list) map.putIfAbsent(e.effectId(), e);
        return map;
    }
}
