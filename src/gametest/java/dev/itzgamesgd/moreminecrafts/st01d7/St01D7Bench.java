package dev.itzgamesgd.moreminecrafts.st01d7;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import dev.itzgamesgd.moreminecrafts.mixin.St01MerchantOffersAccessor;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01DecisionMemo;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01SearchPreparation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ST-01 P8-D7 experiment bench (inert unless {@code -Dmm.st01.d7.out} is set). Dedicated server, bench data pack (cartographer
 * level 2 offers only the fixture ST-01 map), real level-up ({@code increaseMerchantCareer}), real interaction
 * ({@code mobInteract}) and the real select-trade packet handler with a Fabric FakePlayer that keeps the screen open and
 * tries to buy every tick. Scenarios ({@code -Dmm.st01.d7.scenario}): {@code pending} (placeholder, then replacement while
 * the screen is open), {@code pending-save} (the placeholder is never resolved; the world is saved) and {@code reload}
 * (a saved world is reopened in a new process: memo empty, preparation restarts from the persisted placeholder).
 * {@code -Dmm.st01.d7.preload=true} preloads the prepared target to STRUCTURE_STARTS before replacing.
 */
public final class St01D7Bench implements ModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("st01-d7-bench");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

    @Override
    public void onInitialize() {
        String out = System.getProperty("mm.st01.d7.out");
        if (out == null) return;
        ServerLifecycleEvents.SERVER_STARTED.register(server -> new Run(server, out).start());
    }

    private static final class Run {
        final MinecraftServer server;
        final String out;
        final ServerLevel level;
        final String scenario = System.getProperty("mm.st01.d7.scenario", "pending");
        final Map<String, Object> result = new LinkedHashMap<>();
        final Map<String, Double> phaseMax = new LinkedHashMap<>();
        final List<String> events = new ArrayList<>();
        String phase = "WARMUP";
        int tick, phaseTick, mapIndex = -1, attemptsBeforeReplacement, successesBeforeReplacement, attemptsAfter;
        long tickStart, phaseStart;
        boolean finished, replaced;
        Villager villager;
        net.minecraft.server.level.ServerPlayer player;
        ChunkPos preloadTarget;

        Run(MinecraftServer server, String out) {
            this.server = server;
            this.out = out;
            this.level = server.overworld();
        }

        void start() {
            St01D7Placeholders.enabled = true;
            if (scenario.equals("pending-save")) St01D7Placeholders.readiness = (l, p) -> false;
            result.put("scenario", scenario);
            result.put("seed", level.getSeed());
            result.put("preload", St01D7Placeholders.preload);
            result.put("memo_size_at_start", St01DecisionMemo.size(level.getChunkSource().randomState()));
            ServerTickEvents.START_SERVER_TICK.register(s -> tickStart = System.nanoTime());
            ServerTickEvents.END_SERVER_TICK.register(s -> {
                if (finished) return;
                try { onTick(); } catch (Throwable t) { LOG.error("d7 bench failed", t); result.put("error", t.toString()); finish(false); }
            });
        }

        void next(String p) {
            events.add(tick + " " + p);
            phase = p;
            phaseTick = 0;
            phaseStart = System.nanoTime();
        }

        void onTick() throws Exception {
            tick++;
            phaseTick++;
            String label = phase;
            switch (phase) {
                case "WARMUP" -> { if (phaseTick >= 40) next(scenario.equals("reload") ? "FIND" : "SPAWN"); }
                case "SPAWN" -> { spawn(); next("VISIBLE"); }
                case "VISIBLE" -> {
                    if (level.getEntity(villager.getUUID()) == villager) {
                        result.put("prep_requests_when_visible", St01SearchPreparation.REQUESTS.get());
                        openScreen(); // the player opens the screen at level 1 and stays; the level-up happens while it is open
                        next("SETTLE");
                    } else if (phaseTick > 300) throw new IllegalStateException("villager never joined");
                }
                case "SETTLE" -> { if (phaseTick >= 60) next("LEVELUP"); }
                case "LEVELUP" -> { levelUp(); result.put("screen_open_after_level_up", player.containerMenu instanceof MerchantMenu); next("PENDING"); }
                case "FIND" -> {
                    List<? extends Villager> vs = level.getEntities(EntityTypes.VILLAGER, e -> true);
                    if (vs.isEmpty()) { if (phaseTick > 300) throw new IllegalStateException("no villager after reload"); break; }
                    villager = vs.getFirst();
                    result.put("reloaded_offers", describe(villager));
                    mapIndex = placeholderIndex();
                    result.put("reloaded_placeholder_index", mapIndex);
                    openScreen();
                    next("PENDING");
                }
                case "PENDING" -> {
                    boolean placeholderNow = mapIndex >= 0 && St01D7Placeholders.isPlaceholder(offers().get(mapIndex));
                    if (placeholderNow) {
                        attemptsBeforeReplacement++;
                        if (tryBuy()) successesBeforeReplacement++;
                        if (scenario.equals("pending-save") && phaseTick >= 200) { result.put("offers_at_save", describe(villager)); finish(true); return; }
                        if (phaseTick > 6000) throw new IllegalStateException("placeholder never replaced");
                    } else {
                        replaced = true;
                        result.put("pending_wall_ms", (System.nanoTime() - phaseStart) / 1e6);
                        result.put("attempts_while_placeholder", attemptsBeforeReplacement);
                        result.put("successful_purchases_while_placeholder", successesBeforeReplacement);
                        next("BUY");
                    }
                }
                case "BUY" -> {
                    attemptsAfter++;
                    boolean bought = tryBuy();
                    if (bought || attemptsAfter > 5) {
                        result.put("bought_after_replacement", bought);
                        result.put("purchase_attempts_after_replacement", attemptsAfter);
                        result.put("player_maps", playerMaps());
                        result.put("offers_after", describe(villager));
                        if (preloadTarget != null) result.put("tickets_at_target_after_purchase", tickets(preloadTarget));
                        next("TICKETS");
                    }
                }
                case "TICKETS" -> {
                    if (preloadTarget == null || phaseTick >= 260) {
                        if (preloadTarget != null) result.put("tickets_at_target_after_timeout", tickets(preloadTarget));
                        finish(false);
                        return;
                    }
                }
                default -> {}
            }
            double ms = (System.nanoTime() - tickStart) / 1e6;
            if (phase.equals("PENDING") || label.equals("PENDING")) {
                // the tick on which the replacement happened is attributed to REPLACE
                if (!label.equals(phase) && phase.equals("BUY")) label = "REPLACE_TICK";
            }
            phaseMax.merge(label, ms, Math::max);
            if (St01D7Placeholders.PRELOADS.get() > 0 && preloadTarget == null) preloadTarget = currentPreloadTarget();
        }

        void spawn() {
            level.setChunkForced(0, 0, true);
            BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(0, 0, 0));
            Villager v = EntityTypes.VILLAGER.create(level, EntitySpawnReason.COMMAND);
            v.snapTo(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5);
            v.setNoAi(true);
            v.setPersistenceRequired();
            level.addFreshEntity(v);
            v.setVillagerData(v.getVillagerData().withProfession(level.registryAccess().lookupOrThrow(Registries.VILLAGER_PROFESSION)
                    .getOrThrow(VillagerProfession.CARTOGRAPHER)));
            v.setVillagerXp(1);
            v.getOffers();
            villager = v;
        }

        void levelUp() throws Exception {
            int before = offers().size();
            villager.setVillagerXp(10);
            Method m = Villager.class.getDeclaredMethod("increaseMerchantCareer", ServerLevel.class);
            m.setAccessible(true);
            long misses = St01DecisionMemo.MISSES.get(), t0 = System.nanoTime();
            m.invoke(villager, level);
            result.put("level_up_ms", (System.nanoTime() - t0) / 1e6);
            result.put("level_up_server_thread_memo_misses", St01DecisionMemo.MISSES.get() - misses);
            result.put("offers_before_level_up", before);
            result.put("offers_after_level_up", describe(villager));
            mapIndex = placeholderIndex();
            result.put("placeholder_index", mapIndex);
        }

        void openScreen() {
            // a real ServerPlayer on an embedded connection, built as GameTestHelper.makeMockServerPlayerInLevel builds it
            GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes("st01-d7-buyer".getBytes(StandardCharsets.UTF_8)), "st01-d7-buyer");
            var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false);
            player = new net.minecraft.server.level.ServerPlayer(server, level, profile, cookie.clientInformation());
            var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
            new io.netty.channel.embedded.EmbeddedChannel(connection);
            server.getPlayerList().placeNewPlayer(connection, player, cookie);
            player.snapTo(villager.getX() + 1, villager.getY(), villager.getZ());
            villager.mobInteract(player, InteractionHand.MAIN_HAND);
            result.put("screen_open", player.containerMenu instanceof MerchantMenu && villager.getTradingPlayer() == player);
        }

        /** The real select-trade packet handler, then a shift-click on the result slot; true if a filled map was obtained. */
        boolean tryBuy() {
            if (!(player.containerMenu instanceof MerchantMenu menu) || mapIndex < 0) return false;
            if (count(Items.EMERALD) < 32) player.getInventory().add(new ItemStack(Items.EMERALD, 64));
            if (count(Items.COMPASS) < 1) player.getInventory().add(new ItemStack(Items.COMPASS, 1));
            int maps = count(Items.FILLED_MAP);
            player.connection.handleSelectTrade(new ServerboundSelectTradePacket(mapIndex));
            menu.clicked(2, 0, ContainerInput.QUICK_MOVE, player);
            return count(Items.FILLED_MAP) > maps;
        }

        int count(net.minecraft.world.item.Item item) {
            int n = 0;
            for (ItemStack s : player.getInventory()) if (s.is(item)) n += s.getCount();
            return n;
        }

        List<String> playerMaps() {
            List<String> out = new ArrayList<>();
            for (ItemStack s : player.getInventory()) if (s.is(Items.FILLED_MAP)) out.add(target(s));
            return out;
        }

        static String target(ItemStack s) {
            var d = s.get(DataComponents.MAP_DECORATIONS);
            if (d == null || d.decorations().isEmpty()) return "map without target";
            var e = d.decorations().values().iterator().next();
            return "target chunk " + (((int) Math.floor(e.x())) >> 4) + "," + (((int) Math.floor(e.z())) >> 4);
        }

        MerchantOffers offers() { return ((St01MerchantOffersAccessor) villager).moreminecrafts$offers(); }

        int placeholderIndex() {
            MerchantOffers o = offers();
            if (o == null) return -1;
            for (int i = 0; i < o.size(); i++) if (St01D7Placeholders.isPlaceholder(o.get(i)) || o.get(i).getResult().is(Items.FILLED_MAP)) return i;
            return -1;
        }

        static List<String> describe(Villager v) {
            List<String> out = new ArrayList<>();
            MerchantOffers o = ((St01MerchantOffersAccessor) v).moreminecrafts$offers();
            if (o == null) return out;
            for (MerchantOffer offer : o) {
                String r = St01D7Placeholders.isPlaceholder(offer) ? "PLACEHOLDER " + St01D7Placeholders.markerOf(offer).get()
                        : offer.getResult().is(Items.FILLED_MAP) ? "MAP " + target(offer.getResult()) : offer.getResult().getItem().toString();
                out.add(r + " uses=" + offer.getUses() + "/" + offer.getMaxUses() + " outOfStock=" + offer.isOutOfStock());
            }
            return out;
        }

        ChunkPos currentPreloadTarget() {
            var s = St01SearchPreparation.service();
            var t = St01SearchPreparation.targetFor(level);
            if (s == null || t.isEmpty() || villager == null) return null;
            return s.result(t.get(), villager.blockPosition(), 20).flatMap(St01SearchPreparation.Result::firstEligible).orElse(null);
        }

        String tickets(ChunkPos pos) {
            try {
                Field f = level.getChunkSource().getClass().getDeclaredField("ticketStorage");
                f.setAccessible(true);
                var storage = (net.minecraft.world.level.TicketStorage) f.get(level.getChunkSource());
                return storage.getTickets(pos.pack()).toString();
            } catch (ReflectiveOperationException e) {
                return "unavailable: " + e;
            }
        }

        void finish(boolean save) {
            finished = true;
            result.put("events", events);
            result.put("phase_max_tick_ms", phaseMax);
            result.put("placeholders_created", St01D7Placeholders.PLACEHOLDERS_CREATED.get());
            result.put("replaced", St01D7Placeholders.REPLACED.get());
            result.put("not_found", St01D7Placeholders.NOT_FOUND.get());
            result.put("preloads", St01D7Placeholders.PRELOADS.get());
            result.put("preload_target", preloadTarget == null ? null : preloadTarget.x() + "," + preloadTarget.z());
            result.put("prep_completed", St01SearchPreparation.COMPLETED.get());
            result.put("prep_worker_cpu_ms", St01SearchPreparation.WORKER_CPU_NANOS.get() / 1e6);
            if (player != null && player.containerMenu instanceof MerchantMenu) player.closeContainer();
            if (save) server.saveEverything(true, true, true);
            try {
                Path p = Path.of(out);
                if (p.getParent() != null) Files.createDirectories(p.getParent());
                Files.writeString(p, GSON.toJson(result), StandardCharsets.UTF_8);
            } catch (Throwable t) {
                LOG.error("could not write", t);
            }
            Runtime.getRuntime().halt(0);
        }
    }
}
