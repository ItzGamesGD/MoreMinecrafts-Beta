package dev.itzgamesgd.moreminecrafts.bounty;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Immutable, item-bound contract snapshot. Delivery progress is always read from inventory. */
public record ContractData(String id, Tier tier, Status status, List<Objective> objectives, Reward reward) {
    public enum Tier { BEGINNER, INTERMEDIATE, ADVANCED, SPECIAL }
    public enum Status { OFFERED, ACCEPTED }
    public enum Category { COMBAT, DELIVERY, EXPLORATION, SPECIAL }

    public static final Codec<Tier> TIER_CODEC = Codec.STRING.xmap(Tier::valueOf, Tier::name);
    private static final Codec<Status> STATUS_CODEC = Codec.STRING.xmap(Status::valueOf, Status::name);
    private static final Codec<Category> CATEGORY_CODEC = Codec.STRING.xmap(Category::valueOf, Category::name);

    public record Objective(Category category, String target, int required, int progress, List<String> visited) {
        public static final Codec<Objective> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                CATEGORY_CODEC.fieldOf("category").forGetter(Objective::category),
                Codec.STRING.fieldOf("target").forGetter(Objective::target),
                Codec.intRange(1, 100000).fieldOf("required").forGetter(Objective::required),
                Codec.intRange(0, 100000).fieldOf("progress").forGetter(Objective::progress),
                Codec.STRING.listOf().optionalFieldOf("visited", List.of()).forGetter(Objective::visited)
        ).apply(instance, Objective::new));

        public Objective(Category category, String target, int required, int progress) {
            this(category, target, required, progress, List.of());
        }

        public Objective {
            if (category == null || target == null || target.isBlank() || required < 1 || progress < 0 || progress > required || visited == null)
                throw new IllegalArgumentException("Invalid contract objective");
            visited = List.copyOf(visited);
        }

        Objective advance() { return new Objective(category, target, required, Math.min(required, progress + 1), visited); }

        Objective enter(String instance) {
            if (progress >= required || visited.contains(instance)) return this;
            List<String> next = new ArrayList<>(visited);
            next.add(instance);
            return new Objective(category, target, required, progress + 1, next);
        }
    }

    public record Reward(int emeralds, String item, int count) {
        public static final Codec<Reward> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(0, 100000).fieldOf("emeralds").forGetter(Reward::emeralds),
                Codec.STRING.fieldOf("item").forGetter(Reward::item),
                Codec.intRange(1, 100000).fieldOf("count").forGetter(Reward::count)
        ).apply(instance, Reward::new));
        public Reward {
            if (emeralds < 0 || item == null || item.isBlank() || count < 1) throw new IllegalArgumentException("Invalid contract reward");
        }
    }

    public static final Codec<ContractData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(ContractData::id),
            TIER_CODEC.fieldOf("tier").forGetter(ContractData::tier),
            STATUS_CODEC.fieldOf("status").forGetter(ContractData::status),
            Objective.CODEC.listOf().fieldOf("objectives").forGetter(ContractData::objectives),
            Reward.CODEC.fieldOf("reward").forGetter(ContractData::reward)
    ).apply(instance, ContractData::new));

    public ContractData {
        UUID.fromString(id);
        if (tier == null || status == null || objectives == null || objectives.isEmpty() || reward == null)
            throw new IllegalArgumentException("Incomplete contract");
        objectives = List.copyOf(objectives);
    }

    public ContractData accept() {
        return status == Status.OFFERED ? new ContractData(id, tier, Status.ACCEPTED, objectives, reward) : this;
    }

    public ContractData advance(Category category, String target) {
        if (status != Status.ACCEPTED || (category != Category.COMBAT && category != Category.SPECIAL)) return this;
        List<Objective> next = new ArrayList<>(objectives.size());
        for (Objective objective : objectives)
            next.add(objective.category() == category && objective.target().equals(target) ? objective.advance() : objective);
        return new ContractData(id, tier, status, next, reward);
    }

    public ContractData enterStructure(String target, String instance) {
        if (status != Status.ACCEPTED || target == null || instance == null) return this;
        List<Objective> next = new ArrayList<>(objectives.size());
        for (Objective objective : objectives)
            next.add(objective.category() == Category.EXPLORATION && objective.target().equals(target)
                    ? objective.enter(instance) : objective);
        return new ContractData(id, tier, status, next, reward);
    }

    public Map<String, Integer> deliveryNeeds() {
        Map<String, Integer> needs = new HashMap<>();
        for (Objective objective : objectives)
            if (objective.category() == Category.DELIVERY) needs.merge(objective.target(), objective.required(), Integer::sum);
        return Map.copyOf(needs);
    }

    public boolean ready(Map<String, Integer> inventory) {
        if (status != Status.ACCEPTED) return false;
        for (Objective objective : objectives)
            if (objective.category() != Category.DELIVERY && objective.progress() < objective.required()) return false;
        for (var need : deliveryNeeds().entrySet())
            if (inventory.getOrDefault(need.getKey(), 0) < need.getValue()) return false;
        return true;
    }
}
