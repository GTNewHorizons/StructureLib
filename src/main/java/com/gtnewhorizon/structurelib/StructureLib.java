package com.gtnewhorizon.structurelib;

import com.gtnewhorizon.structurelib.block.BlockHint;
import com.gtnewhorizon.structurelib.command.CommandConfigureChannels;
import com.gtnewhorizon.structurelib.item.ItemBlockHint;
import com.gtnewhorizon.structurelib.item.ItemConstructableTrigger;
import com.gtnewhorizon.structurelib.item.ItemFrontRotationTool;
import com.gtnewhorizon.structurelib.net.AlignmentMessage;
import com.gtnewhorizon.structurelib.net.ErrorHintParticleMessage;
import com.gtnewhorizon.structurelib.net.SetChannelDataMessage;
import com.gtnewhorizon.structurelib.net.UpdateHintParticleMessage;
import com.gtnewhorizon.structurelib.util.XSTR;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.launchwrapper.Launch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class does not contain a stable API. Refrain from using this class.
 */
@Mod(
        modid = StructureLibAPI.MOD_ID,
        name = "StructureLib",
        version = "GRADLETOKEN_VERSION",
        acceptableRemoteVersions = "*",
        guiFactory = "com.gtnewhorizon.structurelib.GuiFactory")
public class StructureLib {
    private static final String STRUCTURECOMPAT_MODID = "structurecompat";
    public static boolean DEBUG_MODE;
    public static boolean PANIC_MODE = Boolean.getBoolean("structurelib.panic");
    public static Logger LOGGER = LogManager.getLogger("StructureLib");

    @SidedProxy(
            serverSide = "com.gtnewhorizon.structurelib.CommonProxy",
            clientSide = "com.gtnewhorizon.structurelib.ClientProxy")
    static CommonProxy proxy;

    static SimpleNetworkWrapper net = NetworkRegistry.INSTANCE.newSimpleChannel(StructureLibAPI.MOD_ID);

    static {
        net.registerMessage(
                AlignmentMessage.ServerHandler.class, AlignmentMessage.AlignmentQuery.class, 0, Side.SERVER);
        net.registerMessage(AlignmentMessage.ClientHandler.class, AlignmentMessage.AlignmentData.class, 1, Side.CLIENT);
        net.registerMessage(UpdateHintParticleMessage.Handler.class, UpdateHintParticleMessage.class, 2, Side.CLIENT);
        net.registerMessage(SetChannelDataMessage.Handler.class, SetChannelDataMessage.class, 3, Side.SERVER);
        net.registerMessage(ErrorHintParticleMessage.Handler.class, ErrorHintParticleMessage.class, 4, Side.CLIENT);

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

    static Object COMPAT;

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
                itemFrontRotationTool = new ItemFrontRotationTool(), itemFrontRotationTool.getUnlocalizedName());
        GameRegistry.registerItem(
                itemConstructableTrigger = new ItemConstructableTrigger(),
                itemConstructableTrigger.getUnlocalizedName());
        proxy.preInit(e);
        NetworkRegistry.INSTANCE.registerGuiHandler(instance(), new GuiHandler());

        if (Loader.isModLoaded(STRUCTURECOMPAT_MODID)) {
            COMPAT = Loader.instance()
                    .getIndexedModList()
                    .get(STRUCTURECOMPAT_MODID)
                    .getMod();
        }
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent e) {
        e.registerServerCommand(new CommandConfigureChannels());
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
