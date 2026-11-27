package dev.itzgamesgd.moreminecrafts.spidercolony;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Default and player-placed sacs are inert. Worldgen is deliberately not connected. */
public final class EggSacBlock extends Block {
    public static final float HARDNESS_PROVISIONAL=0.3F;
    public static final BooleanProperty REACTIVE=BooleanProperty.create("reactive");
    public EggSacBlock(Properties p) {super(p);registerDefaultState(stateDefinition.any().setValue(REACTIVE,false));}
    public BlockState naturalState(net.minecraft.util.RandomSource random) { return defaultBlockState().setValue(REACTIVE,EggSacPolicy.naturalReactive(random.nextInt(100))); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {builder.add(REACTIVE);}
    @Override public BlockState playerWillDestroy(Level level,BlockPos pos,BlockState state,Player player) {
        if(level instanceof ServerLevel server && !player.isCreative() && state.getValue(REACTIVE)) {
            var silk=server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
            if(EnchantmentHelper.getItemEnchantmentLevel(silk,player.getMainHandItem())==0) {
                int count=1+server.getRandom().nextInt(2);
                for(int i=0;i<count;i++) for(int attempt=0;attempt<8;attempt++) {
                    var baby=SpiderColonyEntities.JUVENILE.create(server,EntitySpawnReason.TRIGGERED);
                    if(baby==null)break;
                    baby.setPos(pos.getX()+0.5+(server.getRandom().nextDouble()-0.5)*2,pos.getY()+1.01,pos.getZ()+0.5+(server.getRandom().nextDouble()-0.5)*2);
                    if(!server.noCollision(baby))continue;
                    if(server.addFreshEntity(baby))break;
                }
            }
        }
        return super.playerWillDestroy(level,pos,state,player);
    }
}
