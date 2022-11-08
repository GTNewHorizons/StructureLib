package com.gtnewhorizon.structurelib;

import com.gtnewhorizon.structurelib.block.BlockHint;
import com.gtnewhorizon.structurelib.commands.CommandStructureLib;
import com.gtnewhorizon.structurelib.item.ItemBlockHint;
import com.gtnewhorizon.structurelib.item.ItemConstructableTrigger;
import com.gtnewhorizon.structurelib.item.ItemDebugStructureWriter;
import com.gtnewhorizon.structurelib.item.ItemFrontRotationTool;
import com.gtnewhorizon.structurelib.net.AlignmentMessage;
import com.gtnewhorizon.structurelib.net.UpdateDebugWriterModePacket;
import com.gtnewhorizon.structurelib.proxy.CommonProxy;
import com.gtnewhorizon.structurelib.util.XSTR;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.command.CommandHandler;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class does not contain a stable API. Refrain from using this class.
 */
@Mod(modid = StructureLibAPI.MOD_ID, name = "StructureLib", version = "${version}", acceptableRemoteVersions = "*", guiFactory = "com.gtnewhorizon.structurelib.GuiFactory")
public class StructureLib {
	public static boolean DEBUG_MODE = Boolean.getBoolean("structurelib.debug");
	public static Logger LOGGER = LogManager.getLogger("StructureLib");

	@SidedProxy(serverSide = "com.gtnewhorizon.structurelib.proxy.CommonProxy", clientSide = "com.gtnewhorizon.structurelib.proxy.ClientProxy")
	public static CommonProxy proxy;
	public static SimpleNetworkWrapper net = NetworkRegistry.INSTANCE.newSimpleChannel(StructureLibAPI.MOD_ID);

	static {
		net.registerMessage(AlignmentMessage.ServerHandler.class, AlignmentMessage.AlignmentQuery.class, 0, Side.SERVER);
		net.registerMessage(AlignmentMessage.ClientHandler.class, AlignmentMessage.AlignmentData.class, 1, Side.CLIENT);

		net.registerMessage(UpdateDebugWriterModePacket.class, UpdateDebugWriterModePacket.class, 0, Side.CLIENT);
		net.registerMessage(UpdateDebugWriterModePacket.class, UpdateDebugWriterModePacket.class, 0, Side.SERVER);
	}

	public static final XSTR RANDOM = new XSTR();

	public static final CommandStructureLib commandStructureLib = new CommandStructureLib();

	static Block blockHint;
	static Item itemBlockHint;
	static Item itemFrontRotationTool;
	static Item itemConstructableTrigger;
	static Item itemDebugStructureWriter;
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
		GameRegistry.registerItem(itemFrontRotationTool = new ItemFrontRotationTool(), itemFrontRotationTool.getUnlocalizedName());
		GameRegistry.registerItem(itemConstructableTrigger = new ItemConstructableTrigger(), itemConstructableTrigger.getUnlocalizedName());
		GameRegistry.registerItem(itemDebugStructureWriter = new ItemDebugStructureWriter(), itemDebugStructureWriter.getUnlocalizedName());
		proxy.preInit(e);
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		proxy.init(event);
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

	@Mod.EventHandler
	public void serverStarting(FMLServerStartingEvent event) {
//		CommandHandler commandHandler = (CommandHandler) event.getServer().getCommandManager();
//		commandHandler.registerCommand(commandStructureLib);
	}
}
