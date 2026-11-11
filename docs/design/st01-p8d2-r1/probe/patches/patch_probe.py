import sys
p=sys.argv[1]; s=open(p).read()
# (1) deferred reload check
s=s.replace('case "fixture" -> fixture(server, res);','case "fixture" -> { if (fixture(server, res, finish)) return; }')
s=s.replace('private static void fixture(MinecraftServer server, Map<String, Object> res) {','private static boolean fixture(MinecraftServer server, Map<String, Object> res, Runnable finish) {')
old=s[s.index('            String uuid = System.getProperty("mm.st01.d2.uuid");'):s.index('    // ---------------------------------------------------------------- bench')]
new='''            String uuid = System.getProperty("mm.st01.d2.uuid");
            String[] xz = System.getProperty("mm.st01.d2.at").split(",");
            level.getChunk(Math.floorDiv(Integer.parseInt(xz[0]), 16), Math.floorDiv(Integer.parseInt(xz[1]), 16), ChunkStatus.FULL);
            final int[] ticks = {0};
            ServerTickEvents.END_SERVER_TICK.register(srv -> {
                if (++ticks[0] < 80) return; // entities of a just-loaded chunk are attached by the entity manager over the next ticks
                if (ticks[0] > 80) return;
                Map<String, Object> pr = new LinkedHashMap<>();
                var e = level.getEntity(UUID.fromString(uuid));
                pr.put("entity_found_after_restart", e != null);
                if (e instanceof Villager v) {
                    MerchantOffers mo = v.getOffers();
                    pr.put("offer_count_after_getOffers_includes_mod_contract_stock", mo.size());
                    for (int i = 0; i < mo.size(); i++) if (mo.get(i).getResult().is(Items.FILLED_MAP)) {
                        pr.put("map_offer_index", i);
                        pr.put("map", describeMap(level, mo.get(i).getResult()));
                        pr.put("offer_json", MerchantOffers.CODEC.encodeStart(net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, reg), oneOffer(mo.get(i))).getOrThrow().toString());
                    }
                }
                res.put("persisted_villager_after_restart", pr);
                finish.run();
            });
            return true;
        }
        return false;
    }

'''
s=s.replace(old,new)
# create-phase: decoration-based target validation
s=s.replace('''                    ChunkPos mc = ChunkPos.containing(new BlockPos(cx, 0, cz));
                    StructureStart ss = level.getChunk(mc.x(), mc.z(), ChunkStatus.STRUCTURE_STARTS).getStartForStructure(st01(level).value());
                    r.put("map_center_chunk", mc.x() + "," + mc.z());
                    r.put("map_center_chunk_has_valid_st01_start", ss != null && ss.isValid());''','''                    @SuppressWarnings("unchecked") List<String> decs = (List<String>) d.get("decorations");
                    String[] dp = decs.get(0).substring(decs.get(0).indexOf('@') + 1).split(",");
                    ChunkPos mc = ChunkPos.containing(new BlockPos((int) Double.parseDouble(dp[0]), 0, (int) Double.parseDouble(dp[1])));
                    StructureStart ss = level.getChunk(mc.x(), mc.z(), ChunkStatus.STRUCTURE_STARTS).getStartForStructure(st01(level).value());
                    r.put("target_decoration_chunk", mc.x() + "," + mc.z());
                    r.put("target_decoration_chunk_has_valid_st01_start", ss != null && ss.isValid());
                    r.put("target_decoration_chunk_is_in_enumerated_real_starts", starts.contains(mc));
                    r.put("map_center_is_grid_snapped_not_the_target", true);
                    ChunkPos mcc = ChunkPos.containing(new BlockPos(cx, 0, cz));
                    r.put("map_center_chunk", mcc.x() + "," + mcc.z());''')
s=s.replace('r.put("map_center_inside_start_box_xz", cx >= b.minX() && cx <= b.maxX() && cz >= b.minZ() && cz <= b.maxZ());','r.put("target_decoration_inside_start_box_xz", Double.parseDouble(dp[0]) >= b.minX() && Double.parseDouble(dp[0]) <= b.maxX() && Double.parseDouble(dp[1]) >= b.minZ() && Double.parseDouble(dp[1]) <= b.maxZ());\n                        r.put("target_within_map_area", Math.abs(Double.parseDouble(dp[0]) - cx) <= 256 && Math.abs(Double.parseDouble(dp[1]) - cz) <= 256);')
s=s.replace('r.put("distance_map_center_to_anchor_blocks", Math.hypot(cx - (mc.getMinBlockX() + 8), cz - (mc.getMinBlockZ() + 8)));','r.put("distance_target_to_anchor_blocks", Math.hypot(Double.parseDouble(dp[0]) - (mc.getMinBlockX() + 8), Double.parseDouble(dp[1]) - (mc.getMinBlockZ() + 8)));')
s=s.replace('    // ---------------------------------------------------------------- bench','    static MerchantOffers oneOffer(MerchantOffer o) { MerchantOffers m = new MerchantOffers(); m.add(o); return m; }\n\n    // ---------------------------------------------------------------- bench',1)
# (2) incr dispatch worker
s=s.replace('StructureCheckResult r = sm.checkStructurePresence(pc, st, placement, false);\n                if (r == StructureCheckResult.START_NOT_PRESENT) { stats[1]++; continue; }','StructureCheckResult r = pre ? sm.checkStructurePresence(pc, st, placement, false) : StructureCheckResult.CHUNK_LOAD_NEEDED;\n                if (r == StructureCheckResult.START_NOT_PRESENT) { stats[1]++; continue; }')
s=s.replace('        final int[] ring = {0};','        final boolean pre = !System.getProperty("mm.st01.d2.precheck", "true").equals("false");\n        res.put("precheck_on_server_thread", pre);\n        final int[] ring = {0};')
s=s.replace('var f = cache.getChunkFuture(pc.x(), pc.z(), ChunkStatus.STRUCTURE_STARTS, true);','''java.util.concurrent.CompletableFuture<?> f;
                if (worker != null) f = java.util.concurrent.CompletableFuture.supplyAsync(() -> cache.getChunkFuture(pc.x(), pc.z(), ChunkStatus.STRUCTURE_STARTS, true), worker).thenCompose(x -> x);
                else f = cache.getChunkFuture(pc.x(), pc.z(), ChunkStatus.STRUCTURE_STARTS, true);''')
s=s.replace('        double budgetMs = Double.parseDouble(System.getProperty("mm.st01.d2.budgetMs", "5"));','        double budgetMs = Double.parseDouble(System.getProperty("mm.st01.d2.budgetMs", "5"));\n        final java.util.concurrent.ExecutorService worker = System.getProperty("mm.st01.d2.dispatch", "main").equals("worker") ? java.util.concurrent.Executors.newSingleThreadExecutor() : null;\n        res.put("dispatch", worker == null ? "main-thread getChunkFuture (blocks per candidate: managedBlock)" : "worker-thread getChunkFuture (non-blocking for the server thread)");')
open(p,'w').write(s)
