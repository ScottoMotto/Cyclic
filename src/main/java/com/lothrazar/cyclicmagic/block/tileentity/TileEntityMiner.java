package com.lothrazar.cyclicmagic.block.tileentity;
import java.lang.ref.WeakReference;
import java.util.UUID;
import com.google.common.base.Charsets;
import com.lothrazar.cyclicmagic.block.BlockMiner;
import com.lothrazar.cyclicmagic.util.UtilChat;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fml.common.FMLCommonHandler;

/**
 * 
 * @author Lothrazar (Sam Bassett)
 *
 *         This entire file basically started as a carbon copy from lumien
 *         https://github.com/lumien231/Random-Things/blob/master/src/main/java/lumien/randomthings/tileentity/TileEntityBlockBreaker.java
 *         Which was released open source under MIT license
 *         https://github.com/lumien231/Random-Things/blob/master/README.md copy
 *         of lumien license
 *
 *
 *         <quote>"
 *
 *         Source Code of the Random Things mod. Feel free to make pull requests
 *         for translation and other stuff.
 * 
 *         License
 * 
 *         The MIT License (MIT)
 * 
 *         Copyright (c) <2015> lumien231 (https://github.com/lumien231)
 * 
 *         Permission is hereby granted, free of charge, to any person obtaining
 *         a copy of this software and associated documentation files (the
 *         "Software"), to deal in the Software without restriction, including
 *         without limitation the rights to use, copy, modify, merge, publish,
 *         distribute, sublicense, and/or sell copies of the Software, and to
 *         permit persons to whom the Software is furnished to do so, subject to
 *         the following conditions:
 * 
 *         The above copyright notice and this permission notice shall be
 *         included in all copies or substantial portions of the Software.
 * 
 *         THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *         EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *         MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *         NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 *         BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 *         ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *         CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *         SOFTWARE. "</quote>
 *
 *         So, i am following the License by including that and the authors name
 *         and links
 *
 *         REASONS for using it: i knew how to break a block instantly, but i
 *         had no idea how to have the mining slow animation speed that a player
 *         yas people on forums said to use a 'fake player' so i researched that
 *         on github and found this
 *
 *         DIFFERENCES FROM ORIGINAL that I added:
 *
 *         - this can only face horizontally, not up and down (defined in block)
 *         - different recipe - different texture/model - can be disabled in the
 *         config system just like other parts of this mod - only works when
 *         redstone powers it on - Slower mining speed - I will be making
 *         multiple versions: for axe/pick/shovel (use axe tool instead of
 *         diamond pick) and ill pass those in as enum flags - possibly also
 *         making a 3x3 version
 * 
 */
public class TileEntityMiner extends TileEntity implements ITickable {
  //vazkii wanted simple block breaker and block placer. already have the BlockBuilder for placing :D
  //of course this isnt standalone and hes probably found some other mod by now but doing it anyway https://twitter.com/Vazkii/status/767569090483552256
  // fake player idea ??? https://gitlab.prok.pw/Mirrors/minecraftforge/commit/f6ca556a380440ededce567f719d7a3301676ed0
  public static final GameProfile breakerProfile = new GameProfile(UUID.nameUUIDFromBytes("CyclicFakePlayer".getBytes(Charsets.UTF_8)), "CyclicFakePlayer");
  UUID uuid;
  boolean isCurrentlyMining;
  WeakReference<FakePlayer> fakePlayer;
  float curBlockDamage;
  boolean firstTick = true;
  BlockPos targetPos = null;
  @Override
  public void update() {
    if (!worldObj.isRemote) {
      if (firstTick || fakePlayer == null) {
        firstTick = false;
        initFakePlayer();
      }
      BlockPos center = pos.offset(worldObj.getBlockState(pos).getValue(BlockMiner.PROPERTYFACING));
      if (targetPos == null) {
        targetPos = center; //not sure if this is needed
      }
      boolean hasPower = (worldObj.isBlockIndirectlyGettingPowered(this.getPos()) > 0);
      if (hasPower) {
        if (isCurrentlyMining == false) { //we can mine but are not currently
          int rollDice = worldObj.rand.nextInt(9); //TODO: dont have it switch while mining and get this working
          // 1 2 3
          // 4 - 5
          // 6 7 8
          switch (rollDice) {
          // case zero: stay on target
          case 1:
            targetPos = center.offset(EnumFacing.UP).offset(EnumFacing.WEST);
            break;
          case 2:
            targetPos = center.offset(EnumFacing.UP);
            break;
          case 3:
            targetPos = center.offset(EnumFacing.UP).offset(EnumFacing.EAST);
            break;
          case 4:
            targetPos = center.offset(EnumFacing.WEST);
            break;
          case 5:
            targetPos = center.offset(EnumFacing.EAST);
            break;
          case 6:
            targetPos = center.offset(EnumFacing.DOWN).offset(EnumFacing.WEST);
            break;
          case 7:
            targetPos = center.offset(EnumFacing.DOWN);
            break;
          case 8:
            targetPos = center.offset(EnumFacing.DOWN).offset(EnumFacing.EAST);
            break;
          }
          if (!worldObj.isAirBlock(targetPos)) { //we have a valid target
            isCurrentlyMining = true;
            curBlockDamage = 0;
          }
          else { // no valid target, back out
            isCurrentlyMining = false;
            resetProgress(targetPos);
          }
        }
        // else    we can mine ANd we are already mining, dont change anything eh
      }
      else { // we do not have power
        if (isCurrentlyMining) {
          isCurrentlyMining = false;
          resetProgress(targetPos);
        }
      }
      if (isCurrentlyMining) {
        IBlockState targetState = worldObj.getBlockState(targetPos);
        curBlockDamage += targetState.getBlock().getPlayerRelativeBlockHardness(targetState, fakePlayer.get(), worldObj, targetPos);
        if (curBlockDamage >= 1.0f) {
          isCurrentlyMining = false;
          resetProgress(targetPos);
          if (fakePlayer.get() != null) {
            boolean didBreak = fakePlayer.get().interactionManager.tryHarvestBlock(targetPos);
//            if (didBreak == false)
//              System.out.println("tried to break but failed " + UtilChat.blockPosToString(targetPos) + "_" + targetState.getBlock().getUnlocalizedName());
          }
        }
        else {
          worldObj.sendBlockBreakProgress(uuid.hashCode(), targetPos, (int) (curBlockDamage * 10.0F) - 1);
        }
      }
    }
  }
  private void initFakePlayer() {
    if (uuid == null) {
      uuid = UUID.randomUUID();
      IBlockState state = worldObj.getBlockState(this.pos);
      worldObj.notifyBlockUpdate(pos, state, state, 3);
    }
    fakePlayer = new WeakReference<FakePlayer>(FakePlayerFactory.get((WorldServer) worldObj, breakerProfile));
    ItemStack unbreakingIronPickaxe = new ItemStack(Items.DIAMOND_PICKAXE, 1);
    unbreakingIronPickaxe.setTagCompound(new NBTTagCompound());
    unbreakingIronPickaxe.getTagCompound().setBoolean("Unbreakable", true);
    fakePlayer.get().setHeldItem(EnumHand.MAIN_HAND, unbreakingIronPickaxe);
    fakePlayer.get().onGround = true;
    fakePlayer.get().connection = new NetHandlerPlayServer(FMLCommonHandler.instance().getMinecraftServerInstance(), new NetworkManager(EnumPacketDirection.SERVERBOUND), fakePlayer.get()) {
      @SuppressWarnings("rawtypes")
      @Override
      public void sendPacket(Packet packetIn) {
      }
    };
  }
  private static final String NBTMINING = "mining";
  private static final String NBTDAMAGE = "curBlockDamage";
  private static final String NBTPLAYERID = "uuid";
  private static final String NBTTARGET = "target";
  @Override
  public NBTTagCompound writeToNBT(NBTTagCompound compound) {
    if (uuid != null) {
      compound.setString(NBTPLAYERID, uuid.toString());
    }
    if (targetPos != null) {
      compound.setIntArray(NBTTARGET, new int[] { targetPos.getX(), targetPos.getY(), targetPos.getZ() });
    }
    compound.setBoolean(NBTMINING, isCurrentlyMining);
    compound.setFloat(NBTDAMAGE, curBlockDamage);
    return compound;
  }
  @Override
  public void readFromNBT(NBTTagCompound compound) {
    if (compound.hasKey(NBTPLAYERID)) {
      uuid = UUID.fromString(compound.getString(NBTPLAYERID));
    }
    if (compound.hasKey(NBTTARGET)) {
      int[] coords = compound.getIntArray(NBTTARGET);
      if (coords.length >= 3) {
        targetPos = new BlockPos(coords[0], coords[1], coords[2]);
      }
    }
    isCurrentlyMining = compound.getBoolean(NBTMINING);
    curBlockDamage = compound.getFloat(NBTDAMAGE);
  }
  public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
    if (isCurrentlyMining && uuid != null) {
      resetProgress(pos);
    }
  }
  private void resetProgress(BlockPos targetPos) {
    if (uuid != null) {
      //BlockPos targetPos = pos.offset(state.getValue(BlockMiner.PROPERTYFACING));
      worldObj.sendBlockBreakProgress(uuid.hashCode(), targetPos, -1);
      curBlockDamage = 0;
    }
  }
}
