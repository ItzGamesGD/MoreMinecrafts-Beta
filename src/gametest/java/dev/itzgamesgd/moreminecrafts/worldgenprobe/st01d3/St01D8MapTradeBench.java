package dev.itzgamesgd.moreminecrafts.worldgenprobe.st01d3;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import dev.itzgamesgd.moreminecrafts.mixin.St01MerchantOffersAccessor;
import dev.itzgamesgd.moreminecrafts.pyramid.maptrade.St01MapPlaceholders;
import dev.itzgamesgd.moreminecrafts.pyramid.maptrade.St01MapTargetReadiness;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01DecisionMemo;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01SearchPreparation;
import java.lang.management.ManagementFactory;
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
import net.minecraft.server.level.ServerPlayer;
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
 * ST-01 P8-D8 benchmark of the production cartographer map trade (inert unless {@code -Dmm.st01.d8.out} is set). Dedicated
 * server, production data only. Cartographers at level 3 are spawned (the C6 triggers fire), a real ServerPlayer opens each
 * trading screen, the vanilla level-up to 4 runs ({@code increaseMerchantCareer}, map chance forced to 1 by the run script),
 * and the player tries to buy the map through the select-trade packet handler and the merchant menu every tick until a real
 * map is bought. Scenarios ({@code -Dmm.st01.d8.scenario}): {@code single}, {@code multi} (four cartographers, two in the
 * same placement region, level-up in the same tick), {@code save} (placeholder kept, world saved), {@code reload} (the saved
 * world reopened in a new process). {@code -Dmm.st01.maptrade.blocking=true} is the vanilla synchronous baseline.
 * {@code -Dmm.st01.d8.settle} = ticks between the screen opening and the level-up.
 */
public final class St01D8MapTradeBench implements ModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("st01-d8-bench");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

    @Override
    public void onInitialize() {
        String out = System.getProperty("mm.st01.d8.out");
        if (out == null) return;
        ServerLifecycleEvents.SERVER_STARTED.register(server -> new Run(server, out).start());
    }

    private static final class Buyer {
        final Villager villager;
        ServerPlayer player;
        int mapIndex = -1, attemptsWhilePlaceholder, successesWhilePlaceholder, attempts;
        long replacedTick = -1;
        String bought;

        Buyer(Villager villager) { this.villager = villager; }
    }

    private static final class Run {
        final MinecraftServer server;
        final String out;
        final ServerLevel level;
        final String scenario = System.getProperty("mm.st01.d8.scenario", "single");
        final int settle = Integer.getInteger("mm.st01.d8.settle", 5);
        final Map<String, Object> result = new LinkedHashMap<>();
        final Map<String, Double> phaseMax = new LinkedHashMap<>();
        final List<String> events = new ArrayList<>();
        final List<Buyer> buyers = new ArrayList<>();
        final Map<Integer, Double> pendingTicks = new LinkedHashMap<>();
        final java.util.Set<Integer> replacementTicks = new java.util.TreeSet<>();
        String phase = "WARMUP";
        int tick, phaseTick;
        long tickStart, phaseStart, heapBefore;
        boolean finished;

        Run(MinecraftServer server, String out) {
            this.server = server;
            this.out = out;
            this.level = server.overworld();
        }

        void start() {
            if (scenario.equals("save")) St01MapPlaceholders.readiness = (v, l, r, p, a) -> St01MapTargetReadiness.State.NOT_READY;
            result.put("scenario", scenario);
            result.put("seed", level.getSeed());
            result.put("blocking", Boolean.getBoolean("mm.st01.maptrade.blocking"));
            result.put("settle_ticks", settle);
            ServerTickEvents.START_SERVER_TICK.register(s -> tickStart = System.nanoTime());
            ServerTickEvents.END_SERVER_TICK.register(s -> {
                if (finished) return;
                try { onTick(); } catch (Throwable t) { LOG.error("d8 bench failed", t); result.put("error", t.toString()); finish(false); }
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
                case "WARMUP" -> {
                    if (phaseTick == 30) { System.gc(); heapBefore = usedHeap(); label = "HEAP_GC"; }
                    if (phaseTick >= 40) next(scenario.equals("reload") ? "FIND" : "SPAWN");
                }
                case "SPAWN" -> {
                    int[][] at = scenario.equals("multi") ? new int[][] {{0, 0}, {8, 8}, {600, 0}, {0, 600}} : new int[][] {{0, 0}};
                    for (int[] p : at) buyers.add(new Buyer(spawn(p[0], p[1])));
                    next("VISIBLE");
                }
                case "VISIBLE" -> {
                    if (buyers.stream().allMatch(b -> level.getEntity(b.villager.getUUID()) == b.villager)) {
                        for (Buyer b : buyers) openScreen(b);
                        result.put("prep_requests_when_visible", St01SearchPreparation.REQUESTS.get());
                        next("SETTLE");
                    } else if (phaseTick > 400) throw new IllegalStateException("villagers never joined");
                }
                case "SETTLE" -> { if (phaseTick >= settle) next("LEVELUP"); }
                case "LEVELUP" -> {
                    List<Map<String, Object>> ups = new ArrayList<>();
                    for (Buyer b : buyers) ups.add(levelUp(b));
                    result.put("level_ups", ups);
                    result.put("level_up_tick_ms", (System.nanoTime() - tickStart) / 1e6);
                    next("PENDING");
                }
                case "FIND" -> {
                    List<? extends Villager> vs = level.getEntities(EntityTypes.VILLAGER, e -> true);
                    if (vs.isEmpty()) { if (phaseTick > 400) throw new IllegalStateException("no villager after reload"); break; }
                    Buyer b = new Buyer(vs.getFirst());
                    buyers.add(b);
                    b.mapIndex = mapIndex(b.villager);
                    result.put("reloaded_offers", describe(b.villager));
                    openScreen(b);
                    next("PENDING");
                }
                case "PENDING" -> {
                    boolean all = true;
                    for (Buyer b : buyers) {
                        if (b.bought != null) continue;
                        all = false;
                        MerchantOffers o = offers(b.villager);
                        boolean placeholder = b.mapIndex >= 0 && b.mapIndex < o.size() && St01MapPlaceholders.isPlaceholder(o.get(b.mapIndex));
                        if (placeholder) {
                            b.attemptsWhilePlaceholder++;
                            if (tryBuy(b) != null) b.successesWhilePlaceholder++;
                            continue;
                        }
                        if (b.replacedTick < 0) { b.replacedTick = tick; replacementTicks.add(tick); }
                        b.attempts++;
                        String got = tryBuy(b);
                        if (got != null) b.bought = got;
                        else if (b.attempts > 5) b.bought = "NOT BOUGHT";
                    }
                    if (scenario.equals("save") && phaseTick >= 100) {
                        result.put("offers_at_save", describe(buyers.getFirst().villager));
                        finish(true);
                        return;
                    }
                    if (all) {
                        result.put("pending_wall_ms", (System.nanoTime() - phaseStart) / 1e6);
                        next("AFTER");
                    } else if (phaseTick > 12000) throw new IllegalStateException("placeholders never resolved");
                }
                case "AFTER" -> {
                    if (phaseTick == 1) {
                        System.gc();
                        label = "HEAP_GC";
                        result.put("retained_heap_delta_mb", (usedHeap() - heapBefore) / 1048576.0);
                    }
                    if (phaseTick >= 260) { finish(false); return; }
                }
                default -> {}
            }
            double ms = (System.nanoTime() - tickStart) / 1e6;
            phaseMax.merge(label, ms, Math::max);
            if (label.equals("PENDING")) pendingTicks.put(tick, ms);
        }

        Villager spawn(int x, int z) {
            level.setChunkForced(x >> 4, z >> 4, true);
            BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z));
            Villager v = EntityTypes.VILLAGER.create(level, EntitySpawnReason.COMMAND);
            v.snapTo(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5);
            v.setNoAi(true);
            v.setPersistenceRequired();
            level.addFreshEntity(v);
            var prof = level.registryAccess().lookupOrThrow(Registries.VILLAGER_PROFESSION).getOrThrow(VillagerProfession.CARTOGRAPHER);
            v.setVillagerData(v.getVillagerData().withProfession(prof).withLevel(3));
            v.setVillagerXp(70);
            v.getOffers();
            return v;
        }

        void openScreen(Buyer b) {
            GameProfile profile = new GameProfile(UUID.randomUUID(), "st01-d8-" + buyers.indexOf(b));
            var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false);
            b.player = new ServerPlayer(server, level, profile, cookie.clientInformation());
            var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
            new io.netty.channel.embedded.EmbeddedChannel(connection);
            server.getPlayerList().placeNewPlayer(connection, b.player, cookie);
            b.player.snapTo(b.villager.getX() + 1, b.villager.getY(), b.villager.getZ());
            b.villager.mobInteract(b.player, InteractionHand.MAIN_HAND);
        }

        Map<String, Object> levelUp(Buyer b) throws Exception {
            Map<String, Object> m = new LinkedHashMap<>();
            b.villager.setVillagerXp(150);
            Method inc = Villager.class.getDeclaredMethod("increaseMerchantCareer", ServerLevel.class);
            inc.setAccessible(true);
            long misses = St01DecisionMemo.MISSES.get(), t0 = System.nanoTime();
            inc.invoke(b.villager, level);
            m.put("level_up_ms", (System.nanoTime() - t0) / 1e6);
            m.put("server_thread_memo_misses", St01DecisionMemo.MISSES.get() - misses);
            m.put("screen_open", b.player.containerMenu instanceof MerchantMenu && b.villager.getTradingPlayer() == b.player);
            b.mapIndex = mapIndex(b.villager);
            m.put("map_index", b.mapIndex);
            m.put("offers", describe(b.villager));
            return m;
        }

        /** The real select-trade packet handler, then a shift-click on the result; the target of a newly obtained map, or null. */
        String tryBuy(Buyer b) {
            if (!(b.player.containerMenu instanceof MerchantMenu menu) || b.mapIndex < 0) return null;
            ServerPlayer p = b.player;
            if (count(p, Items.EMERALD) < 32) p.getInventory().add(new ItemStack(Items.EMERALD, 64));
            if (count(p, Items.COMPASS) < 1) p.getInventory().add(new ItemStack(Items.COMPASS, 1));
            int maps = count(p, Items.FILLED_MAP);
            p.connection.handleSelectTrade(new ServerboundSelectTradePacket(b.mapIndex));
            menu.clicked(2, 0, ContainerInput.PICKUP, p);
            ItemStack carried = menu.getCarried();
            if (carried.is(Items.FILLED_MAP)) {
                String t = target(carried);
                p.getInventory().add(carried.copy());
                menu.setCarried(ItemStack.EMPTY);
                return t;
            }
            return count(p, Items.FILLED_MAP) > maps ? "map (target unread)" : null;
        }

        static int count(ServerPlayer p, net.minecraft.world.item.Item item) {
            int n = 0;
            for (ItemStack s : p.getInventory()) if (s.is(item)) n += s.getCount();
            return n;
        }

        static String target(ItemStack s) {
            var d = s.get(DataComponents.MAP_DECORATIONS);
            if (d == null || d.decorations().isEmpty()) return "map without target";
            var e = d.decorations().values().iterator().next();
            return "target chunk " + (((int) Math.floor(e.x())) >> 4) + "," + (((int) Math.floor(e.z())) >> 4);
        }

        static MerchantOffers offers(Villager v) { return ((St01MerchantOffersAccessor) v).moreminecrafts$offers(); }

        static int mapIndex(Villager v) {
            MerchantOffers o = offers(v);
            if (o == null) return -1;
            for (int i = 0; i < o.size(); i++) if (St01MapPlaceholders.isPlaceholder(o.get(i)) || o.get(i).getResult().is(Items.FILLED_MAP)) return i;
            return -1;
        }

        static List<String> describe(Villager v) {
            List<String> out = new ArrayList<>();
            MerchantOffers o = offers(v);
            if (o == null) return out;
            for (MerchantOffer offer : o) {
                String r = St01MapPlaceholders.isPlaceholder(offer) ? "PLACEHOLDER " + St01MapPlaceholders.markerOf(offer).get()
                        : offer.getResult().is(Items.FILLED_MAP) ? "MAP " + target(offer.getResult()) : offer.getResult().getItem().toString();
                out.add(r + " cost " + offer.getItemCostA().count() + "x" + offer.getItemCostA().item().getRegisteredName()
                        + offer.getItemCostB().map(c -> " + " + c.count() + "x" + c.item().getRegisteredName()).orElse("")
                        + " uses=" + offer.getUses() + "/" + offer.getMaxUses());
            }
            return out;
        }

        String tickets(ChunkPos pos) {
            try {
                Field f = level.getChunkSource().getClass().getDeclaredField("ticketStorage");
                f.setAccessible(true);
                return ((net.minecraft.world.level.TicketStorage) f.get(level.getChunkSource())).getTickets(pos.pack()).toString();
            } catch (ReflectiveOperationException e) {
                return "unavailable";
            }
        }

        static long usedHeap() {
            Runtime r = Runtime.getRuntime();
            return r.totalMemory() - r.freeMemory();
        }

        void finish(boolean save) {
            finished = true;
            List<Map<String, Object>> b = new ArrayList<>();
            for (Buyer x : buyers) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("villager_at", x.villager.blockPosition().toShortString());
                m.put("attempts_while_placeholder", x.attemptsWhilePlaceholder);
                m.put("successful_purchases_while_placeholder", x.successesWhilePlaceholder);
                m.put("replaced_at_tick", x.replacedTick);
                m.put("bought", x.bought);
                m.put("offers_final", describe(x.villager));
                b.add(m);
            }
            result.put("buyers", b);
            result.put("events", events);
            Map<String, Double> rep = new LinkedHashMap<>();
            for (int t : replacementTicks) rep.put("tick " + t, pendingTicks.get(t));
            result.put("replacement_tick_ms", rep);
            double other = 0;
            for (var e : pendingTicks.entrySet()) if (!replacementTicks.contains(e.getKey())) other = Math.max(other, e.getValue());
            result.put("max_pending_tick_without_replacement_ms", other);
            result.put("slowest_pending_ticks", pendingTicks.entrySet().stream().sorted((x1, x2) -> Double.compare(x2.getValue(), x1.getValue())).limit(4)
                    .map(e -> e.getKey() + ":" + Math.round(e.getValue())).toList());
            result.put("phase_max_tick_ms", phaseMax);
            result.put("placeholders_created", St01MapPlaceholders.CREATED.get());
            result.put("replaced", St01MapPlaceholders.REPLACED.get());
            result.put("immediate_real_offers", St01MapPlaceholders.IMMEDIATE.get());
            result.put("fallback_trades", St01MapPlaceholders.FALLBACK_TRADES.get());
            result.put("preloads", St01MapTargetReadiness.PRELOADS.get());
            result.put("readiness_failures", St01MapTargetReadiness.FAILURES.get());
            result.put("prep_completed", St01SearchPreparation.COMPLETED.get());
            result.put("prep_candidates", St01SearchPreparation.CANDIDATES.get());
            result.put("prep_worker_cpu_ms", St01SearchPreparation.WORKER_CPU_NANOS.get() / 1e6);
            result.put("max_warm_pass_ms", St01SearchPreparation.PUMP_MAX_NANOS.get() / 1e6);
            result.put("server_process_cpu_s", ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getProcessCpuTime() / 1e9);
            if (!buyers.isEmpty() && buyers.getFirst().bought != null && buyers.getFirst().bought.startsWith("target chunk")) {
                String[] xz = buyers.getFirst().bought.substring("target chunk ".length()).split(",");
                result.put("tickets_at_target_at_end", tickets(new ChunkPos(Integer.parseInt(xz[0]), Integer.parseInt(xz[1]))));
            }
            for (Buyer x : buyers) if (x.player != null && x.player.containerMenu instanceof MerchantMenu) x.player.closeContainer();
            if (save) server.saveEverything(true, true, true);
            String jfr = System.getProperty("mm.st01.d8.jfr");
            if (jfr != null) {
                try {
                    for (var r : jdk.jfr.FlightRecorder.getFlightRecorder().getRecordings()) r.dump(Path.of(jfr));
                } catch (Exception e) {
                    LOG.error("jfr dump failed", e);
                }
            }
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
