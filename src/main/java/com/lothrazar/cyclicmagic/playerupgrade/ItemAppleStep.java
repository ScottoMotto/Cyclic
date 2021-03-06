/*******************************************************************************
 * The MIT License (MIT)
 * 
 * Copyright (C) 2014-2018 Sam Bassett (aka Lothrazar)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package com.lothrazar.cyclicmagic.playerupgrade;

import java.util.List;
import com.lothrazar.cyclicmagic.ModCyclic;
import com.lothrazar.cyclicmagic.config.IHasConfig;
import com.lothrazar.cyclicmagic.core.IHasRecipe;
import com.lothrazar.cyclicmagic.core.util.Const;
import com.lothrazar.cyclicmagic.core.util.UtilChat;
import com.lothrazar.cyclicmagic.core.util.UtilParticle;
import com.lothrazar.cyclicmagic.core.util.UtilSound;
import com.lothrazar.cyclicmagic.registry.CapabilityRegistry;
import com.lothrazar.cyclicmagic.registry.CapabilityRegistry.IPlayerExtendedProperties;
import com.lothrazar.cyclicmagic.registry.RecipeRegistry;
import com.lothrazar.cyclicmagic.registry.SoundRegistry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemAppleStep extends ItemFood implements IHasRecipe, IHasConfig {

  public static boolean defaultPlayerStepUp = false;

  public ItemAppleStep() {
    super(4, false);
    this.setAlwaysEdible();
  }

  @Override
  protected void onFoodEaten(ItemStack stack, World world, EntityPlayer player) {
    final IPlayerExtendedProperties data = CapabilityRegistry.getPlayerProperties(player);
    boolean previousOn = data.isStepHeightOn();
    data.setStepHeightOn(!previousOn);
    if (previousOn) {
      UtilSound.playSound(player, player.getPosition(), SoundRegistry.step_height_down, SoundCategory.PLAYERS, 1.0F);
      data.setForceStepOff(true);
    }
    else {
      UtilSound.playSound(player, SoundRegistry.step_height_up);
    }
    UtilParticle.spawnParticle(world, EnumParticleTypes.CRIT_MAGIC, player.getPosition());
    UtilParticle.spawnParticle(world, EnumParticleTypes.CRIT_MAGIC, player.getPosition().up());
    if (player.getEntityWorld().isRemote) {
      UtilChat.addChatMessage(player, "unlocks.stepheight." + !previousOn);
    }
  }

  @Override
  public IRecipe addRecipe() {
    return RecipeRegistry.addShapelessRecipe(new ItemStack(this),
        "dyeCyan", "dyeOrange", Blocks.TALLGRASS,
        Items.APPLE);
  }

  @SubscribeEvent
  public void onEntityUpdate(LivingUpdateEvent event) {
    if (event.getEntityLiving() instanceof EntityPlayer) {//some of the items need an off switch
      EntityPlayer player = (EntityPlayer) event.getEntityLiving();
      final IPlayerExtendedProperties data = CapabilityRegistry.getPlayerProperties(player);
      if (data.isStepHeightOn()) {
        if (player.isSneaking()) {
          //make sure that, when sneaking, dont fall off!!
          player.stepHeight = 0.9F;
        }
        else {
          player.stepHeight = 1.0F + (1F / 16F);//PATH BLOCKS etc are 1/16th downif MY feature turns this on, then do it
        }
      }
      else if (data.doForceStepOff()) {
        data.setForceStepOff(false);
        //otherwise, dont automatically force it off. only force it off the once if player is toggling FROM on TO off with my feature
        // EntityLivingBase default constructor uses 0.6 as default, so slabs + path block for example
        player.stepHeight = 0.6F;
        ModCyclic.logger.log(player.stepHeight + "disabled step height" + player.world.isRemote);
      }
      //else leave it alone (allows other mods to turn it on without me disrupting)
    }
  }

  @SideOnly(Side.CLIENT)
  @Override
  public void addInformation(ItemStack stack, World player, List<String> tooltip, net.minecraft.client.util.ITooltipFlag advanced) {
    tooltip.add(UtilChat.lang(this.getUnlocalizedName() + ".tooltip"));
    super.addInformation(stack, player, tooltip, advanced);
  }

  @Override
  public void syncConfig(Configuration config) {
    defaultPlayerStepUp = config.getBoolean("StepHeightDefault", Const.ConfigCategory.player, false, "Set the players default step height value.  False is just like normal minecraft, true means step height is one full block.   Only applies to new players the first time they join the world.  Regardless of setting this can still be toggled with Apple of Lofty Stature.  ");
  }
}
