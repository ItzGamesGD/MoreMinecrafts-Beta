import sys
p=sys.argv[1]; s=open(p).read()
old='                level.addFreshEntity(keep);\n'
new='''                ChunkPos kc = ChunkPos.containing(new BlockPos((int) vx, 70, (int) vz));
                level.getChunk(kc.x(), kc.z(), ChunkStatus.FULL);
                level.setChunkForced(kc.x(), kc.z(), true);
                boolean added = level.addFreshEntity(keep);
'''
assert old in s; s=s.replace(old,new)
s=s.replace('pr.put("uuid", keep.getUUID().toString());','pr.put("addFreshEntity_returned", added); pr.put("entity_visible_in_level_right_after", level.getEntity(keep.getUUID()) != null); pr.put("uuid", keep.getUUID().toString());')
open(p,'w').write(s)
