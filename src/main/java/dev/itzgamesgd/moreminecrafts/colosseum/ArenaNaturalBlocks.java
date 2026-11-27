package dev.itzgamesgd.moreminecrafts.colosseum;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.progression.K5NaturalTreeProvenance;
import java.util.*;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.block.Blocks;

/** Generation provenance plus permanent post-generation edit exclusions. Legacy/unmarked chunks fail closed. */
public final class ArenaNaturalBlocks {
    public static final class Data {
        private final boolean generated;
        private final Set<Long> edits;
        public Data(boolean generated,Set<Long> edits) {this.generated=generated;this.edits=new HashSet<>(edits);}
        public boolean generated() {return generated;}
        public Set<Long> edits() {return Collections.unmodifiableSet(edits);}
    }
    private static final Codec<Data> CODEC=RecordCodecBuilder.create(i->i.group(Codec.BOOL.fieldOf("generated").forGetter(Data::generated),Codec.LONG.listOf().fieldOf("edits").forGetter(d->List.copyOf(d.edits))).apply(i,(g,e)->new Data(g,new HashSet<>(e))));
    private static final AttachmentType<Data> TYPE=AttachmentRegistry.create(ModIds.id("st05_natural_provenance_v1"),b->b.persistent(CODEC));
    private ArenaNaturalBlocks() {}
    static void initialize() { }
    public static void generated(ChunkAccess chunk) {
        AttachmentTarget target=(AttachmentTarget)chunk;
        if(target.getAttached(TYPE)==null)target.setAttached(TYPE,new Data(true,Set.of()));
    }
    public static void changed(Level level,BlockPos pos,net.minecraft.world.level.block.state.BlockState before,net.minecraft.world.level.block.state.BlockState after) {
        if(!naturalType(before) && !naturalType(after))return;
        if(!(level instanceof ServerLevel l) || !l.hasChunkAt(pos))return;
        var target=(AttachmentTarget)l.getChunkAt(pos);Data d=target.getAttached(TYPE);
        if(d==null || !d.generated || d.edits.contains(pos.asLong()))return;
        // Server-thread mutation: do not copy/hash an entire chunk of edits per changed block.
        if(d.edits.add(pos.asLong()))l.getChunkAt(pos).markUnsaved();
    }
    static boolean eligible(ServerLevel l,BlockPos p,ColosseumData.Site site) {
        // Full protected structure footprint, including decorations/stands and floor, irrespective of block type.
        long dx=p.getX()-site.cx(),dz=p.getZ()-site.cz();
        if(dx*dx+dz*dz<=(long)ArenaGeometry.EXT_R*ArenaGeometry.EXT_R)return false;
        if(!l.hasChunkAt(p) || l.getBlockEntity(p)!=null || !l.structureManager().getAllStructuresAt(p).isEmpty())return false;
        var state=l.getBlockState(p);if(state.getDestroySpeed(l,p)<0)return false;
        Data d=((AttachmentTarget)l.getChunkAt(p)).getAttached(TYPE);
        if(d==null || !d.generated || d.edits.contains(p.asLong()))return false;
        return naturalStone(state) || K5NaturalTreeProvenance.hasMarker(l,p);
    }
    private static boolean naturalType(net.minecraft.world.level.block.state.BlockState s) {return naturalStone(s) || s.is(net.minecraft.tags.BlockTags.LOGS);}
    private static boolean naturalStone(net.minecraft.world.level.block.state.BlockState s) {return s.is(Blocks.STONE)||s.is(Blocks.DEEPSLATE)||s.is(Blocks.GRANITE)||s.is(Blocks.DIORITE)||s.is(Blocks.ANDESITE)||s.is(Blocks.TUFF);}

}
