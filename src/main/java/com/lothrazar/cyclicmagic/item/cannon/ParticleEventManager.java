package com.lothrazar.cyclicmagic.item.cannon;

import com.lothrazar.cyclicmagic.ModCyclic;
import com.lothrazar.cyclicmagic.core.util.Const;
import com.lothrazar.cyclicmagic.proxy.ClientProxy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ParticleEventManager {

  public static long ticks = 0;

  @SideOnly(Side.CLIENT)
  @SubscribeEvent
  public void onTextureStitch(TextureStitchEvent event) {
    ParticleGolemLaser.sprite = event.getMap().registerSprite(new ResourceLocation(Const.MODID, "entity/particle_mote"));
    System.out.println("onTextureStitch");
  }

  @SideOnly(Side.CLIENT)
  @SubscribeEvent(priority = EventPriority.NORMAL)
  public void onTick(TickEvent.ClientTickEvent event) {
    if (event.side == Side.CLIENT && event.phase == TickEvent.Phase.START) {
      ClientProxy.particleRenderer.updateParticles();
      ticks++;
    }
  }

  static float tickCounter = 0;
  static EntityPlayer clientPlayer = null;

  @SubscribeEvent(priority = EventPriority.LOW)
  @SideOnly(Side.CLIENT)
  public void onRenderAfterWorld(RenderWorldLastEvent event) {
    tickCounter++;
    GlStateManager.pushMatrix();
    if (ModCyclic.proxy instanceof ClientProxy && Minecraft.getMinecraft().player != null) {
      ClientProxy.particleRenderer.renderParticles(Minecraft.getMinecraft().player, event.getPartialTicks());
    }
    GlStateManager.popMatrix();
  }
}
