package dev.itzgamesgd.moreminecrafts.mob;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class CustomMobRosterDefinitionTest {
    @Test
    void allCustomMobRendererStatesHaveDedicatedValidVanillaSizedPngs() throws Exception {
        String catalog = Files.readString(Path.of("src/main/java/dev/itzgamesgd/moreminecrafts/mob/CustomMobCatalog.java"));
        Matcher matcher = Pattern.compile("type\\(\"([a-z0-9_]+)\"").matcher(catalog);
        Set<String> mobs = new HashSet<>();
        while (matcher.find()) mobs.add(matcher.group(1));

        Set<String> expected = new HashSet<>();
        for (String mob : mobs) {
            expected.add(mob + ".png");
            if (mob.endsWith("_zombie")) expected.add(mob + "_baby.png");
            if (Set.of("drowned_sailor", "deep_sea_drowned", "river_drowned").contains(mob)) {
                expected.add(mob + "_baby.png");
                expected.add(mob + "_outer_layer.png");
                expected.add(mob + "_outer_layer_baby.png");
            }
            if (mob.endsWith("_spider")) expected.add(mob + "_eyes.png");
            if (mob.equals("mountain_skeleton")) expected.add(mob + "_overlay.png");
            if (mob.equals("wild_piglin")) expected.add(mob + "_baby.png");
            if (mob.equals("lesser_phantom")) expected.add(mob + "_eyes.png");
            if (mob.equals("cave_creeper")) expected.add(mob + "_armor.png");
            if (mob.equals("giant_bee")) {
                expected.addAll(Set.of("giant_bee_angry.png", "giant_bee_nectar.png",
                        "giant_bee_angry_nectar.png", "giant_bee_baby.png", "giant_bee_angry_baby.png",
                        "giant_bee_nectar_baby.png", "giant_bee_angry_nectar_baby.png"));
            }
            if (mob.equals("feral_wolf")) {
                expected.addAll(Set.of("feral_wolf_angry.png", "feral_wolf_tame.png", "feral_wolf_baby.png",
                        "feral_wolf_angry_baby.png", "feral_wolf_tame_baby.png"));
            }
        }
        assertEquals(40, mobs.size());
        assertEquals(78, expected.size(), "All base and renderer-state texture routes must be accounted for");

        Path textureRoot = Path.of("src/main/resources/assets/moreminecrafts/textures/entity/mobs");
        Set<String> actual;
        try (var paths = Files.list(textureRoot)) {
            actual = paths.filter(Files::isRegularFile).map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".png")).collect(Collectors.toSet());
        }
        assertEquals(expected, actual, "No routed mob-state PNG may be missing or left unused");

        Set<String> size64x32 = new HashSet<>();
        for (String id : mobs) {
            if (id.contains("skeleton") || id.endsWith("_spider") || id.endsWith("_bug")
                    || Set.of("wild_slime", "mud_slime", "sand_slime", "cave_creeper", "feral_wolf").contains(id)) {
                size64x32.add(id + ".png");
            }
        }
        for (String id : Set.of("small_spider", "large_spider", "forest_spider", "desert_spider", "swamp_spider", "ice_spider")) {
            size64x32.add(id + "_eyes.png");
        }
        size64x32.addAll(Set.of("mountain_skeleton_overlay.png", "cave_creeper_armor.png",
                "feral_wolf_angry.png", "feral_wolf_tame.png"));
        Set<String> size32x32 = Set.of("cave_bat.png", "giant_bee_baby.png", "giant_bee_angry_baby.png",
                "giant_bee_nectar_baby.png", "giant_bee_angry_nectar_baby.png", "feral_wolf_baby.png",
                "feral_wolf_angry_baby.png", "feral_wolf_tame_baby.png");
        for (String name : expected) {
            var image = ImageIO.read(textureRoot.resolve(name).toFile());
            assertTrue(image != null, "PNG decoder rejected " + name);
            int width = size32x32.contains(name) ? 32 : 64;
            int height = size64x32.contains(name) ? 32 : width;
            assertEquals(width, image.getWidth(), "Unexpected vanilla UV width for " + name);
            assertEquals(height, image.getHeight(), "Unexpected vanilla UV height for " + name);
        }
    }

    @Test
    void allFortyTypesHaveUniqueLocalizedNamesAndTextureBacklogEntries() throws Exception {
        String catalog = Files.readString(Path.of("src/main/java/dev/itzgamesgd/moreminecrafts/mob/CustomMobCatalog.java"));
        String language = Files.readString(Path.of("src/main/resources/assets/moreminecrafts/lang/en_us.json"));
        String backlog = Files.readString(Path.of("docs/CUSTOM_MOB_RESOURCE_BACKLOG.md"));
        Matcher matcher = Pattern.compile("type\\(\"([a-z0-9_]+)\"").matcher(catalog);
        Set<String> ids = new HashSet<>();
        while (matcher.find()) {
            String id = matcher.group(1);
            assertTrue(ids.add(id), "Duplicate entity identifier: " + id);
            assertTrue(language.contains("\"entity.moreminecrafts." + id + "\""),
                    "Missing localized entity name: " + id);
            assertTrue(backlog.contains("textures/entity/mobs/" + id + ".png"),
                    "Missing per-mob texture backlog path: " + id);
            Path lootPath = Path.of("src/main/resources/data/moreminecrafts/loot_table/entities/" + id + ".json");
            assertTrue(Files.isRegularFile(lootPath), "Missing vanilla-family loot alias: " + id);
            assertTrue(Files.readString(lootPath).contains("minecraft:entities/"),
                    "Custom mob loot must preserve its vanilla base-family drops: " + id);
        }
        assertEquals(40, ids.size(), "The implementation must retain the approved forty-entry roster");
        String stats = Files.readString(Path.of("src/main/java/dev/itzgamesgd/moreminecrafts/mob/CustomMobStats.java"));
        assertTrue(stats.contains("type == CustomMobCatalog.SWAMP_ZOMBIE")
                        && stats.contains("type == CustomMobCatalog.CAVE_CREEPER"),
                "Custom stat profiles must be scoped to exact MoreMinecrafts EntityType identities");
        assertTrue(!stats.contains("getKey(mob.getType()).getPath()"),
                "Custom stat profiles must never dispatch on an un-namespaced registry path");
        assertTrue(stats.contains("!APPLIED.add(mob)"),
                "Custom stat application must explicitly guard against repeated finalization");
        assertTrue(!stats.contains("Attributes.SCALE"),
                "Entity size must have one source of truth; stat finalization must not rescale sized EntityTypes");
        assertTrue(!catalog.contains("Slime.checkSlimeSpawnRules")
                        && catalog.contains("type == WILD_SLIME || type == MUD_SLIME || type == SAND_SLIME")
                        && catalog.contains("return Monster.checkMonsterSpawnRules(type, level, reason, pos, random);")
                        && catalog.contains("MagmaCube.checkMagmaCubeSpawnRules")
                        && catalog.contains("Drowned.checkDrownedSpawnRules")
                        && catalog.contains("Piglin.checkPiglinSpawnRules")
                        && catalog.contains("Wolf.checkWolfSpawnRules")
                        && catalog.contains("Bat.checkBatSpawnRules")
                        && catalog.contains("Animal.checkAnimalSpawnRules"),
                "Natural spawn predicates must preserve family rules except where an approved explicit habitat replaces vanilla's unrelated spawn gate");
        assertTrue(catalog.contains("spec.type.getCategory()"),
                "Biome spawn registration must use each EntityType's family population category");
        for (String tag : Set.of("undead", "zombies", "skeletons", "raiders", "illager", "arthropod",
                "sensitive_to_smite", "sensitive_to_bane_of_arthropods", "beehive_inhabitors",
                "burn_in_daylight", "inverted_healing_and_harm", "wither_friends", "immune_to_oozing",
                "frog_food", "axolotl_always_hostiles", "immune_to_infested",
                "no_anger_from_wind_charge", "powder_snow_walkable_mobs", "fall_damage_immune",
                "freeze_hurts_extra_types", "freeze_immune_entity_types", "dismounts_underwater",
                "followable_friendly_mobs", "non_controlling_rider")) {
            assertTrue(Files.isRegularFile(Path.of("src/main/resources/data/minecraft/tags/entity_type/" + tag + ".json")),
                    "Missing vanilla semantic tag extension: " + tag);
        }
        assertTrue(Files.readString(Path.of("src/main/resources/data/moreminecrafts/loot_table/entities/bandit.json"))
                        .contains("\"minecraft:entities/pillager\""),
                "Bandit must preserve Pillager-family loot rather than Vindicator loot");
        assertTrue(Files.readString(Path.of("src/main/resources/data/moreminecrafts/loot_table/entities/small_spider.json"))
                        .contains("\"minecraft:entities/cave_spider\""),
                "Small Spider must preserve its Cave Spider-family loot alias");
        assertTrue(catalog.contains("new CatalogSlime") && catalog.contains("new CatalogMagmaCube"),
                "Slime-family variants must own size-lifecycle stat scaling instead of relying on one-time finalizeSpawn");
        assertTrue(!stats.contains("case \"wild_slime\"") && !stats.contains("case \"lesser_magma_cube\""),
                "Cube-mob stat multipliers must not be duplicated by the global finalizeSpawn stat pass");
        String feralWolf = Files.readString(Path.of("src/main/java/dev/itzgamesgd/moreminecrafts/mob/FeralWolf.java"));
        assertTrue(feralWolf.contains("NonTameRandomTargetGoal")
                        && feralWolf.contains("public void setTame(boolean isTame, boolean includeSideEffects)")
                        && feralWolf.contains("return !FeralWolf.this.isTame() && super.canContinueToUse()")
                        && feralWolf.contains("protected void applyTamingSideEffects()")
                        && feralWolf.contains("HEALTH_MULTIPLIER = 1.15")
                        && feralWolf.contains("setTarget(null)"),
                "Feral Wolf player hunting must stop on tame and vanilla tame-health rewrites must retain the approved health profile");
        assertTrue(catalog.contains("new SmallCaveSpider") && catalog.contains("new SwampCaveSpider"),
                "Small and Swamp Spider must use their explicit Cave Spider-family behavior classes");
        String smallSpider = Files.readString(Path.of("src/main/java/dev/itzgamesgd/moreminecrafts/mob/SmallCaveSpider.java"));
        String swampSpider = Files.readString(Path.of("src/main/java/dev/itzgamesgd/moreminecrafts/mob/SwampCaveSpider.java"));
        assertTrue(smallSpider.contains("MobEffectInstance.CODEC.encodeStart")
                        && smallSpider.contains("MobEffectInstance.CODEC.parse")
                        && swampSpider.contains("MobEffectInstance.CODEC.encodeStart")
                        && swampSpider.contains("MobEffectInstance.CODEC.parse"),
                "Cave-spider poison replacement must deep-copy the full pre-existing effect chain instead of dropping hidden effects");
        assertTrue(!smallSpider.contains("new MobEffectInstance(poison)")
                        && !swampSpider.contains("new MobEffectInstance(poison)"),
                "Poison restoration must not use the shallow MobEffectInstance copy constructor");
        assertTrue(!catalog.contains("WILD_PIGLIN = type(\"wild_piglin\", Piglin::new, 0.6f, 1.95f, true)"),
                "Wild Piglin must not gain non-vanilla fire immunity");
        for (String biomeTag : Set.of("snowy_habitat", "fungal_habitat", "desert_habitat", "swamp_habitat", "wild_slime_habitat")) {
            assertTrue(Files.isRegularFile(Path.of("src/main/resources/data/moreminecrafts/tags/worldgen/biome/" + biomeTag + ".json")),
                    "Missing approved habitat biome tag: " + biomeTag);
        }
        String banditArcher = Files.readString(Path.of("src/main/java/dev/itzgamesgd/moreminecrafts/mob/BanditArcher.java"));
        assertTrue(banditArcher.contains("arrow.setBaseDamage(vanillaBaseDamage * 0.92)")
                        && banditArcher.contains("velocity * 2.0F")
                        && banditArcher.contains("getRandom().triangle(serverLevel.getDifficulty().getId() * 0.11, 0.57425)")
                        && banditArcher.contains("getProjectile(bow)")
                        && banditArcher.contains("BOW_AND_ARROW")
                        && banditArcher.contains("public void applyRaidBuffs")
                        && !banditArcher.contains("super.applyRaidBuffs")
                        && !stats.contains("case \"bandit_archer\" -> { health = 0.82; damage = 0.92; }"),
                "Bandit Archer must preserve bow projectile semantics, aiming pose, raid-stable equipment, and ranged damage scaling");
        String banditEntity = Files.readString(Path.of("src/main/java/dev/itzgamesgd/moreminecrafts/mob/BanditEntity.java"));
        assertTrue(banditEntity.contains("public void applyRaidBuffs")
                        && banditEntity.contains("Items.IRON_SWORD")
                        && !banditEntity.contains("super.applyRaidBuffs"),
                "Melee Bandit must retain its approved sword through Pillager raid buff lifecycle");
        assertTrue(stats.contains("AttributeModifier.Operation.ADD_VALUE")
                        && stats.contains("ARMOR_PROFILE_MODIFIER")
                        && stats.contains("refreshArmorProfile(mob)")
                        && stats.contains("instance.getValue()"),
                "Armor profiles must scale current armor, including equipment added after spawn finalization");
        String lesserPhantom = Files.readString(Path.of("src/main/java/dev/itzgamesgd/moreminecrafts/mob/LesserPhantom.java"));
        assertTrue(lesserPhantom.contains("(6.0 + getPhantomSize()) * ATTACK_MULTIPLIER")
                        && !stats.contains("case \"lesser_phantom\" -> { health = 0.60; damage = 0.72; }"),
                "Lesser Phantom must own dynamic size-derived attack scaling instead of losing it after vanilla rewrites");
        assertTrue(catalog.contains("MobCategory.AMBIENT, false, 5, false")
                        && catalog.contains("MobCategory.CREATURE, false, 10, false")
                        && catalog.contains("MobCategory.MONSTER, false, 8, true"),
                "Custom EntityTypes with non-default vanilla-family metadata must declare it explicitly");
        assertTrue(catalog.contains("builder.notInPeaceful()")
                        && catalog.contains("builder.spawnDimensionsScale(4.0F)")
                        && catalog.contains("BlockTags.WITHER_SKELETON_IMMUNE_TO")
                        && catalog.contains("BlockTags.STRAY_IMMUNE_TO"),
                "EntityType metadata parity must include Peaceful exclusion, cube spawn sizing, and family block immunities");
        assertTrue(catalog.contains("structureAssociationAllowed(type, level, pos)")
                        && catalog.contains("BuiltinStructures.MINESHAFT")
                        && catalog.contains("BuiltinStructures.FORTRESS")
                        && catalog.contains("BuiltinStructures.DESERT_PYRAMID")
                        && catalog.contains("BuiltinStructures.SHIPWRECK")
                        && catalog.contains("getStructureWithPieceAt(pos.getX(), pos.getY(), pos.getZ(), structure)"),
                "Structure-associated roster entries must require generated structures, not only structure-capable biomes");
        assertTrue(catalog.contains("insideDesertPyramidUnderground(level, pos)")
                        && catalog.contains("DESERT_PYRAMID_BELOW_PIECE_DEPTH = 14")
                        && catalog.contains("startsForStructure(chunkX, chunkZ")
                        && catalog.contains("pos.getY() < box.minY()")
                        && catalog.contains("box.minY() - DESERT_PYRAMID_BELOW_PIECE_DEPTH"),
                "Desert Bug must recognize the pyramid's vanilla-generated underground volume below its recorded piece bounding box");
        assertTrue(!catalog.contains("minecraft:is_swamp"),
                "26.3 has no vanilla minecraft:is_swamp biome tag; swamp variants must use the mod-owned habitat tag");
        assertTrue(Files.readString(Path.of("src/main/resources/data/moreminecrafts/tags/worldgen/biome/wild_slime_habitat.json"))
                        .contains("#moreminecrafts:swamp_habitat"),
                "Wild Slime habitat must compose the mod-owned swamp tag instead of a missing vanilla tag");
        assertTrue(catalog.contains("add(FROZEN_ZOMBIE, Zombie.createAttributes(), SNOWY_HABITAT")
                        && catalog.contains("add(FUNGAL_ZOMBIE, Zombie.createAttributes(), FUNGAL_HABITAT")
                        && catalog.contains("add(DESERT_SPIDER, Spider.createAttributes(), DESERT_HABITAT")
                        && catalog.contains("add(ICE_SPIDER, Spider.createAttributes(), SNOWY_HABITAT")
                        && catalog.contains("add(WILD_SLIME, net.minecraft.world.entity.monster.Monster.createMonsterAttributes(), WILD_SLIME_HABITAT")
                        && catalog.contains("add(SAND_SLIME, net.minecraft.world.entity.monster.Monster.createMonsterAttributes(), DESERT_HABITAT"),
                "Approved biome identities must not regress to mountain/jungle/badlands shortcuts");
        assertTrue(!Files.exists(Path.of("src/main/resources/data/minecraft/tags/entity_type/aquatic.json"))
                        && !Files.exists(Path.of("src/main/resources/data/minecraft/tags/entity_type/axolotl_hunt_targets.json")),
                "Drowned variants must not be injected into vanilla aquatic or axolotl prey tags");
        String oozingTag = Files.readString(Path.of("src/main/resources/data/minecraft/tags/entity_type/immune_to_oozing.json"));
        assertTrue(oozingTag.contains("moreminecrafts:wild_slime")
                        && !oozingTag.contains("moreminecrafts:lesser_magma_cube"),
                "Oozing immunity must follow vanilla Slime rather than Magma Cube");
        assertTrue(Files.readString(Path.of("src/main/resources/data/minecraft/tags/entity_type/freeze_immune_entity_types.json"))
                        .contains("moreminecrafts:mountain_skeleton"),
                "Stray-based Mountain Skeleton must extend vanilla freeze immunity");
        String dismountTag = Files.readString(Path.of("src/main/resources/data/minecraft/tags/entity_type/dismounts_underwater.json"));
        assertTrue(dismountTag.contains("moreminecrafts:large_spider")
                        && dismountTag.contains("moreminecrafts:ice_spider")
                        && !dismountTag.contains("moreminecrafts:small_spider")
                        && !dismountTag.contains("moreminecrafts:swamp_spider"),
                "Only Spider-based variants, not Cave Spider variants, inherit underwater dismount semantics");
        String followableTag = Files.readString(Path.of("src/main/resources/data/minecraft/tags/entity_type/followable_friendly_mobs.json"));
        assertTrue(followableTag.contains("moreminecrafts:feral_wolf")
                        && followableTag.contains("moreminecrafts:giant_bee"),
                "Wolf- and Bee-family variants must extend the vanilla followable-friendly-mobs tag");
        String client = Files.readString(Path.of("src/client/java/dev/itzgamesgd/moreminecrafts/client/MoreMinecraftsClient.java"));
        Matcher rendererMatcher = Pattern.compile("EntityRendererRegistry\\.register\\(CustomMobCatalog\\.").matcher(client);
        int rendererCount = 0;
        while (rendererMatcher.find()) rendererCount++;
        assertEquals(40, rendererCount, "All forty custom EntityTypes must retain client renderer registrations");
        Matcher routes = Pattern.compile("CustomMobCatalog\\.([A-Z_]+),\\s*context -> CustomMobRenderers\\.(\\w+)\\(context, \"([a-z_]+)\"\\)").matcher(client);
        Set<String> routedIds = new HashSet<>();
        java.util.Map<String, String> families = new java.util.HashMap<>();
        while (routes.find()) {
            String id = routes.group(3);
            assertEquals(routes.group(1).toLowerCase(java.util.Locale.ROOT), id,
                    "A renderer must use its own entity's dedicated texture identifier");
            assertTrue(routedIds.add(id), "Duplicate texture route: " + id);
            families.put(id, routes.group(2));
        }
        assertEquals(ids, routedIds, "Every approved entity must route to its own texture");
        assertEquals("zombie", families.get("swamp_zombie"));
        assertEquals("caveSpider", families.get("small_spider"));
        assertEquals("caveSpider", families.get("swamp_spider"));
        assertEquals("witherSkeleton", families.get("lesser_wither_skeleton"));
        assertEquals("stray", families.get("mountain_skeleton"));
        assertEquals("pillager", families.get("bandit_archer"));
        assertTrue(Files.isRegularFile(Path.of("src/main/resources/assets/moreminecrafts/textures/entity/mobs/swamp_zombie.png")),
                "Swamp Zombie must provide its dedicated texture resource");
        String renderers = Files.readString(Path.of("src/client/java/dev/itzgamesgd/moreminecrafts/client/CustomMobRenderers.java"));
        for (String family : Set.of("Zombie", "Skeleton", "WitherSkeleton", "Stray", "CaveSpider",
                "Silverfish", "Endermite", "Slime", "MagmaCube", "Drowned", "Piglin", "Pillager",
                "Vindicator", "Wolf", "Bat", "Bee", "Phantom", "Creeper")) {
            assertTrue(renderers.contains("new " + family + "Renderer(context"),
                    "Texture adapters must retain the original vanilla renderer: " + family);
        }
        assertTrue(renderers.contains("new SpiderRenderer<Spider>(context)"));
        assertTrue(renderers.contains("ModelLayers.PIGLIN_ARMOR, ModelLayers.PIGLIN_BABY_ARMOR"));
        assertTrue(renderers.contains("layer instanceof DrownedOuterLayer")
                        && renderers.contains("ModelLayers.DROWNED_BABY_OUTER_LAYER")
                        && renderers.contains("state.isBaby ? babyOuter : outer")
                        && renderers.contains("ModelLayers.STRAY_OUTER_LAYER")
                        && renderers.contains("layer instanceof SlimeOuterLayer")
                        && renderers.contains("RenderTypes.entityTranslucent(base)")
                        && renderers.contains("RenderTypes.outline(base)")
                        && renderers.contains("layer instanceof SpiderEyesLayer")
                        && renderers.contains("layer instanceof PhantomEyesLayer")
                        && renderers.contains("layer instanceof CreeperPowerLayer")
                        && renderers.contains("new CreeperPowerLayer(this, context.getModelSet())")
                        && renderers.contains("Identifier armor = texture(id + \"_armor\")")
                        && renderers.contains("protected Identifier getTextureLocation() { return armor; }")
                        && renderers.contains("RenderTypes.eyes(texture(id + \"_eyes\"))"),
                "Dedicated textures must preserve the original outer, clothes, translucent and emissive layers");
        assertTrue(renderers.contains("state.isAngry ? 1 : 0")
                        && renderers.contains("state.hasNectar ? 2 : 0")
                        && renderers.contains("state.isBaby ? 4 : 0")
                        && renderers.contains("super.extractRenderState(wolf, state, partialTick)")
                        && renderers.contains("wolf.isTame() ? 1 : wolf.isAngry() ? 2 : 0")
                        && renderers.contains("state.isBaby ? 3 : 0"),
                "Bee and Wolf must retain all vanilla state and age texture branches, including tame priority");
        assertTrue(Files.readString(Path.of("docs/VANILLA_MODEL_CUSTOM_MOB_ROSTER.md"))
                .contains("VANILLA_MODEL_CUSTOM_MOB_ROSTER_IMPLEMENTATION.md"));
    }
}
