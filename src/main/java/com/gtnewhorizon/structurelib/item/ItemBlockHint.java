package com.gtnewhorizon.structurelib.item;

import static net.minecraft.util.StatCollector.translateToLocal;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

public class ItemBlockHint extends ItemBlock {

    public ItemBlockHint(Block p_i45328_1_) {
        super(p_i45328_1_);
        setMaxDamage(0);
        setHasSubtypes(true);
        setCreativeTab(CreativeTabs.tabBlock);
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean adv) {
        tooltip.add(translateToLocal("structurelib.blockhint.desc.0")); // Helps while building
        switch (stack.getItemDamage()) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
                // Placeholder for a certain group.
                tooltip.add(
                        EnumChatFormatting.AQUA.toString() + EnumChatFormatting.BOLD
                                + translateToLocal("structurelib.blockhint.desc.1"));
                break;
            case 12:
                // General placeholder.
                tooltip.add(
                        EnumChatFormatting.AQUA.toString() + EnumChatFormatting.BOLD
                                + translateToLocal("structurelib.blockhint.desc.2"));
                break;
            case 13:
                // Make sure it contains Air material.
                tooltip.add(
                        EnumChatFormatting.AQUA.toString() + EnumChatFormatting.BOLD
                                + translateToLocal("structurelib.blockhint.desc.3"));
                break;
            case 14:
                // Make sure it does not contain Air material.
                tooltip.add(
                        EnumChatFormatting.AQUA.toString() + EnumChatFormatting.BOLD
                                + translateToLocal("structurelib.blockhint.desc.4"));
                break;
            case 15:
                // ERROR, what did u expect?
                tooltip.add(EnumChatFormatting.BLUE + translateToLocal("structurelib.blockhint.desc.5"));
                break;
            default: // WTF?
                tooltip.add("Damn son where did you get that!?");
                tooltip.add(EnumChatFormatting.BLUE + "From outer space... I guess...");
        }
    }

    public int getMetadata(int aMeta) {
        return aMeta;
    }

    public String getUnlocalizedName(ItemStack aStack) {
        return this.field_150939_a.getUnlocalizedName() + "." + getDamage(aStack);
    }
}
