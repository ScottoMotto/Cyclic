package com.lothrazar.cyclicmagic.item.gear;
import com.lothrazar.cyclicmagic.IHasRecipe;
import com.lothrazar.cyclicmagic.module.EmeraldArmorModule;
import net.minecraft.init.Items;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class ItemEmeraldSpade extends ItemSpade implements IHasRecipe {
  public static final String name = "emerald_spade";
  public ItemEmeraldSpade() {
    super(EmeraldArmorModule.TOOL_MATERIAL);
  }
  @Override
  public void addRecipe() {
    GameRegistry.addShapedRecipe(new ItemStack(this), " e ", " s ", " s ", 'e', new ItemStack(EmeraldArmorModule.REPAIR), 's', new ItemStack(Items.STICK));
  }
}