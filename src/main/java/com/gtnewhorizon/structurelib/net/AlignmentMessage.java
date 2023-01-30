package com.gtnewhorizon.structurelib.net;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import com.gtnewhorizon.structurelib.StructureLib;
import com.gtnewhorizon.structurelib.alignment.IAlignment;
import com.gtnewhorizon.structurelib.alignment.IAlignmentProvider;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public abstract class AlignmentMessage implements IMessage {

    int mPosX;
    int mPosY;
    int mPosZ;
    int mPosD;
    int mAlign;

    public AlignmentMessage() {}

    @Override
    public void fromBytes(ByteBuf pBuffer) {
        NBTTagCompound tTag = ByteBufUtils.readTag(pBuffer);
        mPosX = tTag.getInteger("posx");
        mPosY = tTag.getInteger("posy");
        mPosZ = tTag.getInteger("posz");
        mPosD = tTag.getInteger("posd");
        mAlign = tTag.getInteger("rotf");
    }

    @Override
    public void toBytes(ByteBuf pBuffer) {
        NBTTagCompound tFXTag = new NBTTagCompound();
        tFXTag.setInteger("posx", mPosX);
        tFXTag.setInteger("posy", mPosY);
        tFXTag.setInteger("posz", mPosZ);
        tFXTag.setInteger("posd", mPosD);
        tFXTag.setInteger("rotf", mAlign);

        ByteBufUtils.writeTag(pBuffer, tFXTag);
    }

    private AlignmentMessage(IAlignmentProvider provider) {
        if (!(provider instanceof TileEntity)) throw new IllegalArgumentException("Provider must be a TileEntity");
        IAlignment alignment = provider.getAlignment();
        if (alignment == null) throw new IllegalArgumentException("Passed in provider does not provide an alignment!");
        TileEntity base = (TileEntity) provider;
        mPosX = base.xCoord;
        mPosY = base.yCoord;
        mPosZ = base.zCoord;
        mPosD = base.getWorldObj().provider.dimensionId;
        mAlign = alignment.getExtendedFacing().getIndex();
    }

    private AlignmentMessage(World world, int x, int y, int z, IAlignment front) {
        mPosX = x;
        mPosY = y;
        mPosZ = z;
        mPosD = world.provider.dimensionId;
        mAlign = front.getExtendedFacing().getIndex();
    }

    public static class AlignmentQuery extends AlignmentMessage {

        public AlignmentQuery() {}

        public AlignmentQuery(IAlignmentProvider provider) {
            super(provider);
        }

        public AlignmentQuery(World world, int x, int y, int z, IAlignment front) {
            super(world, x, y, z, front);
        }
    }

    public static class AlignmentData extends AlignmentMessage {

        public AlignmentData() {}

        private AlignmentData(AlignmentQuery query) {
            mPosX = query.mPosX;
            mPosY = query.mPosY;
            mPosZ = query.mPosZ;
            mPosD = query.mPosD;
            mAlign = query.mAlign;
        }

        public AlignmentData(IAlignmentProvider provider) {
            super(provider);
        }

        public AlignmentData(World world, int x, int y, int z, IAlignment front) {
            super(world, x, y, z, front);
        }
    }

    public static class ClientHandler implements IMessageHandler<AlignmentData, IMessage> {

        @Override
        public IMessage onMessage(AlignmentData pMessage, MessageContext pCtx) {
            if (StructureLib.getCurrentPlayer().worldObj.provider.dimensionId == pMessage.mPosD) {
                TileEntity te = StructureLib.getCurrentPlayer().worldObj
                        .getTileEntity(pMessage.mPosX, pMessage.mPosY, pMessage.mPosZ);
                if (te instanceof IAlignmentProvider) {
                    IAlignment alignment = ((IAlignmentProvider) te).getAlignment();
                    if (alignment != null) {
                        alignment.setExtendedFacing(ExtendedFacing.byIndex(pMessage.mAlign));
                    }
                }
            }
            return null;
        }
    }

    public static class ServerHandler implements IMessageHandler<AlignmentQuery, AlignmentData> {

        @Override
        public AlignmentData onMessage(AlignmentQuery pMessage, MessageContext pCtx) {
            World world = DimensionManager.getWorld(pMessage.mPosD);
            if (world != null) {
                TileEntity te = world.getTileEntity(pMessage.mPosX, pMessage.mPosY, pMessage.mPosZ);
                if (te instanceof IAlignmentProvider) {
                    IAlignment alignment = ((IAlignmentProvider) te).getAlignment();
                    if (alignment == null) return null;
                    pMessage.mAlign = alignment.getExtendedFacing().getIndex();
                    return new AlignmentData(pMessage);
                }
            }
            return null;
        }
    }
}
