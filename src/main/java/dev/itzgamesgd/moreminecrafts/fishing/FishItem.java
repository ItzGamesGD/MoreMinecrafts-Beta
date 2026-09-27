package dev.itzgamesgd.moreminecrafts.fishing;
import net.minecraft.world.item.Item;
public final class FishItem extends Item {
 private final FishSpecies species;
 public FishItem(FishSpecies species, Properties properties){super(properties);this.species=species;}
 public FishSpecies species(){return species;}
}
