package com.gtnewhorizon.structurelib.command;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;

import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;
import com.gtnewhorizon.structurelib.item.ItemConstructableTrigger;

public class CommandConfigureChannels extends CommandBase {

    @Override
    public String getCommandName() {
        return "sl_channel";
    }

    @Override
    public String getCommandUsage(ICommandSender p_71518_1_) {
        return "/sl_channel <wipe/getall>\n" + "/sl_channel <get/unset> <channel name>\n"
                + "/sl_channel <set> <channel name> <value>\n"
                + "<channel name> can have space in it. <value> must be positive integer";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public List<?> addTabCompletionOptions(ICommandSender p_71516_1_, String[] p_71516_2_) {
        if (p_71516_2_.length == 1)
            return getListOfStringsMatchingLastWord(p_71516_2_, "get", "set", "unset", "wipe", "getall");
        return null;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) throw new WrongUsageException(getCommandUsage(sender));

        if (!(sender instanceof EntityPlayerMP)) throw new WrongUsageException("must be a player");

        EntityPlayerMP player = (EntityPlayerMP) sender;

        ItemStack heldItem = player.getHeldItem();
        if (heldItem == null || !(heldItem.getItem() instanceof ItemConstructableTrigger)) {
            throw new WrongUsageException("must hold a hologram");
        }

        String channel;

        switch (args[0]) {
            case "getall":
                if (!ChannelDataAccessor.hasSubChannel(heldItem)) {
                    sender.addChatMessage(new ChatComponentText("No subchannel"));
                } else {
                    sender.addChatMessage(
                            new ChatComponentTranslation(
                                    "item.structurelib.constructableTrigger.desc.lshift.0",
                                    ChannelDataAccessor.countChannelData(heldItem)));
                    ChannelDataAccessor.iterateChannelData(heldItem).map(e -> e.getKey() + ": " + e.getValue())
                            .map(ChatComponentText::new).forEach(player::addChatMessage);
                }
                return;
            case "wipe":
                ChannelDataAccessor.wipeChannelData(heldItem);
                return;
            case "get":
            case "unset":
                if (args.length < 2) throw new WrongUsageException(getCommandUsage(sender));
                channel = args.length == 2 ? args[1] : Arrays.stream(args).skip(1).collect(Collectors.joining());
                break;
            case "set":
                if (args.length < 3) throw new WrongUsageException(getCommandUsage(sender));
                channel = args.length == 3 ? args[1]
                        : Arrays.stream(args).skip(1).limit(args.length - 2).collect(Collectors.joining());
                break;
            default:
                throw new WrongUsageException(getCommandUsage(sender));
        }

        switch (args[0]) {
            case "get":
                if (ChannelDataAccessor.hasSubChannel(heldItem, channel)) sender.addChatMessage(
                        new ChatComponentText(
                                channel + " value: " + ChannelDataAccessor.getChannelData(heldItem, channel)));
                else sender.addChatMessage(new ChatComponentText(channel + " value: N/A"));
                break;
            case "clear":
                if (ChannelDataAccessor.hasSubChannel(heldItem, channel))
                    sender.addChatMessage(new ChatComponentText(channel + " no value"));
                else {
                    ChannelDataAccessor.unsetChannelData(heldItem, channel);
                    sender.addChatMessage(new ChatComponentText(channel + " cleared"));
                    player.inventoryContainer.detectAndSendChanges();
                }
                break;
            case "set":
                int value;
                try {
                    value = Integer.parseInt(args[args.length - 1]);
                } catch (NumberFormatException e) {
                    throw new WrongUsageException("%s not a valid value", args[args.length - 1]);
                }

                if (value <= 0) throw new WrongUsageException("%s not a valid value", args[args.length - 1]);

                ChannelDataAccessor.setChannelData(heldItem, channel, value);
                player.inventoryContainer.detectAndSendChanges();
                sender.addChatMessage(new ChatComponentText(channel + " value: " + value));
                break;
        }
    }
}
