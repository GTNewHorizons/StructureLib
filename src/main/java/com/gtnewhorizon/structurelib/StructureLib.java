package com.gtnewhorizon.structurelib;

import static com.gtnewhorizon.structurelib.StructureLibAPI.CHANNEL_SHOW_ERROR;
import static com.gtnewhorizon.structurelib.StructureLibAPI.MOD_ID;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gtnewhorizon.structurelib.block.BlockHint;
import com.gtnewhorizon.structurelib.command.CommandConfigureChannels;
import com.gtnewhorizon.structurelib.command.CommandRegistryDebug;
import com.gtnewhorizon.structurelib.item.ItemBlockHint;
import com.gtnewhorizon.structurelib.item.ItemConstructableTrigger;
import com.gtnewhorizon.structurelib.item.ItemFrontRotationTool;
import com.gtnewhorizon.structurelib.net.AlignmentMessage;
import com.gtnewhorizon.structurelib.net.ErrorHintParticleMessage;
import com.gtnewhorizon.structurelib.net.RegistryOrderSyncMessage;
import com.gtnewhorizon.structurelib.net.SetChannelDataMessage;
import com.gtnewhorizon.structurelib.net.UpdateHintParticleMessage;
import com.gtnewhorizon.structurelib.util.InventoryUtility;
import com.gtnewhorizon.structurelib.util.XSTR;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInterModComms;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * This class does not contain a stable API. Refrain from using this class.
 */
@Mod(
        modid = MOD_ID,
        name = "StructureLib",
        version = Tags.VERSION,
        acceptableRemoteVersions = "*",
        guiFactory = "com.gtnewhorizon.structurelib.GuiFactory")
public class StructureLib {

    private static final String STRUCTURECOMPAT_MODID = "structurecompat";
    public static boolean DEBUG_MODE;
    public static boolean PANIC_MODE = Boolean.getBoolean("structurelib.panic");
    public static final Logger LOGGER = LogManager.getLogger("StructureLib");

    @SidedProxy(
            serverSide = "com.gtnewhorizon.structurelib.CommonProxy",
            clientSide = "com.gtnewhorizon.structurelib.ClientProxy")
    static CommonProxy proxy;

    static final SimpleNetworkWrapper net = NetworkRegistry.INSTANCE.newSimpleChannel(MOD_ID);

    static {
        net.registerMessage(
                AlignmentMessage.ServerHandler.class,
                AlignmentMessage.AlignmentQuery.class,
                0,
                Side.SERVER);
        net.registerMessage(AlignmentMessage.ClientHandler.class, AlignmentMessage.AlignmentData.class, 1, Side.CLIENT);
        net.registerMessage(UpdateHintParticleMessage.Handler.class, UpdateHintParticleMessage.class, 2, Side.CLIENT);
        net.registerMessage(SetChannelDataMessage.Handler.class, SetChannelDataMessage.class, 3, Side.SERVER);
        net.registerMessage(ErrorHintParticleMessage.Handler.class, ErrorHintParticleMessage.class, 4, Side.CLIENT);
        net.registerMessage(RegistryOrderSyncMessage.Handler.class, RegistryOrderSyncMessage.class, 5, Side.SERVER);

        try {
            DEBUG_MODE = Boolean.parseBoolean(System.getProperty("structurelib.debug"));
        } catch (IllegalArgumentException | NullPointerException e) {
            // turn on debug by default in dev mode
            // this will be overridden if above property is present and set to false
            DEBUG_MODE = (Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");
        }
    }

    public static final XSTR RANDOM = new XSTR();

    @Mod.Instance
    static StructureLib INSTANCE;
    @Mod.Instance(STRUCTURECOMPAT_MODID)
    public static Object COMPAT;

    static Block blockHint;
    static Item itemBlockHint;
    static Item itemFrontRotationTool;
    static Item itemConstructableTrigger;
    public static final CreativeTabs creativeTab = new CreativeTabs("structurelib") {

        @Override
        @SideOnly(Side.CLIENT)
        public Item getTabIconItem() {
            return StructureLibAPI.getItemBlockHint();
        }
    };

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        ConfigurationHandler.INSTANCE.init(e.getSuggestedConfigurationFile());
        GameRegistry.registerBlock(blockHint = new BlockHint(), ItemBlockHint.class, "blockhint");
        itemBlockHint = ItemBlock.getItemFromBlock(StructureLibAPI.getBlockHint());
        GameRegistry.registerItem(
                itemFrontRotationTool = new ItemFrontRotationTool(),
                itemFrontRotationTool.getUnlocalizedName());
        GameRegistry.registerItem(
                itemConstructableTrigger = new ItemConstructableTrigger(),
                itemConstructableTrigger.getUnlocalizedName());
        proxy.preInit(e);
        NetworkRegistry.INSTANCE.registerGuiHandler(instance(), new GuiHandler());

        InventoryUtility.init();

        ChannelDescription.set(CHANNEL_SHOW_ERROR, MOD_ID, "channels.structurelib.show_errors");

        DevelopHelper.onPreInit();
    }

    @Mod.EventHandler
    public void loadComplete(FMLLoadCompleteEvent e) {
        proxy.loadComplete(e);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent e) {
        e.registerServerCommand(new CommandConfigureChannels());
        e.registerServerCommand(new CommandRegistryDebug());
    }

    @Mod.EventHandler
    public void handleIMC(FMLInterModComms.IMCEvent event) {
        for (FMLInterModComms.IMCMessage message : event.getMessages()) {
            switch (message.key) {
                case "register_channel":
                    processRegisterChannel(message);
                    break;
                case "register_channel_item":
                    processRegisterChannelItem(message);
                    break;
            }
        }
    }

    private void processRegisterChannel(FMLInterModComms.IMCMessage message) {
        NBTTagList tags = message.getNBTValue().getTagList("Channels", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < tags.tagCount(); i++) {
            NBTTagCompound tag = tags.getCompoundTagAt(i);
            StructureLibAPI.registerChannelDescription(
                    tag.getString("Channel"),
                    message.getSender(),
                    tag.getString("Description"));
        }
    }

    private void processRegisterChannelItem(FMLInterModComms.IMCMessage message) {
        NBTTagList items = message.getNBTValue().getTagList("Items", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < items.tagCount(); i++) {
            NBTTagCompound itemTag = items.getCompoundTagAt(i);
            ItemStack item = ItemStack.loadItemStackFromNBT(itemTag.getCompoundTag("Item"));
            NBTTagList tags = itemTag.getTagList("Channels", Constants.NBT.TAG_COMPOUND);
            for (int j = 0; j < tags.tagCount(); j++) {
                NBTTagCompound tag = tags.getCompoundTagAt(j);
                StructureLibAPI.registerChannelItem(
                        tag.getString("Channel"),
                        message.getSender(),
                        tag.getInteger("Value"),
                        item);
            }
        }
    }

    public static void addClientSideChatMessages(String... messages) {
        proxy.addClientSideChatMessages(messages);
    }

    public static EntityPlayer getCurrentPlayer() {
        return proxy.getCurrentPlayer();
    }

    public static boolean isCurrentPlayer(EntityPlayer player) {
        return proxy.isCurrentPlayer(player);
    }

    public static long getOverworldTime() {
        return proxy.getOverworldTime();
    }

    public static StructureLib instance() {
        return INSTANCE;
    }

    public CommonProxy proxy() {
        return proxy;
    }
}
