package com.gtnewhorizon.structurelib;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import cpw.mods.fml.common.event.FMLInterModComms;

public class DevelopHelper {

    private static final boolean DEBUG = Boolean.getBoolean("structurelib.develop_helper");

    public static void onPreInit() {
        if (!DEBUG) return;
        StructureLib.LOGGER.info("Running develop helper");
        StructureLibAPI.registerChannelDescription("sandstone", StructureLibAPI.MOD_ID, "variants of Sandstone");
        StructureLibAPI.registerChannelItem("sandstone", StructureLibAPI.MOD_ID, 1, new ItemStack(Blocks.sandstone));
        StructureLibAPI
                .registerChannelItem("sandstone", StructureLibAPI.MOD_ID, 2, new ItemStack(Blocks.sandstone, 1, 1));
        StructureLibAPI
                .registerChannelItem("sandstone", StructureLibAPI.MOD_ID, 3, new ItemStack(Blocks.sandstone, 1, 2));
        StructureLibAPI.registerChannelDescription("sandbricks", StructureLibAPI.MOD_ID, "variants of Sandbricks");
        StructureLibAPI.registerChannelDescription("apple", StructureLibAPI.MOD_ID, "apple");
        StructureLibAPI.registerChannelDescription("bright", StructureLibAPI.MOD_ID, "bright");
        StructureLibAPI.registerChannelDescription("apple_blight", StructureLibAPI.MOD_ID, "apple_blight");
        StructureLibAPI.registerChannelDescription("app_eagle", StructureLibAPI.MOD_ID, "app_eagle");
    }

    public void x() {
        NBTTagCompound command = new NBTTagCompound();
        NBTTagList items = new NBTTagList();
        int[] metas = { 0, 1, 2 };
        for (int meta : metas) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setTag("Item", new ItemStack(Blocks.sandstone, 1, meta).writeToNBT(new NBTTagCompound()));
            NBTTagList channels = new NBTTagList();
            NBTTagCompound channel = new NBTTagCompound();
            channel.setString("Channel", "sandstone");
            channel.setInteger("Value", meta + 1);
            channels.appendTag(channel);
            tag.setTag("Channels", channels);
            items.appendTag(tag);
        }
        command.setTag("Items", items);
        FMLInterModComms.sendMessage("structurelib", "register_channel_item", command);
    }
}
