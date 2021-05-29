package com.gtnewhorizon.structurelib;

import com.gtnewhorizon.structurelib.alignment.AlignmentMessage;
import com.gtnewhorizon.structurelib.block.BlockHint;
import com.gtnewhorizon.structurelib.item.ItemBlockHint;
import com.gtnewhorizon.structurelib.proxy.CommonProxy;
import com.gtnewhorizon.structurelib.util.XSTR;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class does not contain a stable API. Refrain from using this class.
 */
@Mod(modid = StructureLib.MOD_ID, name = "StructureLib", version = "${version}", acceptableRemoteVersions = "*")
public class StructureLib {
	public static boolean DEBUG_MODE = true;
	public static Logger LOGGER = LogManager.getLogger("StructureLib");
	public static final String MOD_ID = "structurelib";

	@SidedProxy(serverSide = "com.gtnewhorizon.structurelib.proxy.CommonProxy", clientSide = "com.gtnewhorizon.structurelib.proxy.ClientProxy")
	static CommonProxy proxy;
	static SimpleNetworkWrapper net = NetworkRegistry.INSTANCE.newSimpleChannel(MOD_ID);
	static {
		net.registerMessage(AlignmentMessage.ServerHandler.class, AlignmentMessage.AlignmentQuery.class, 0, Side.SERVER);
		net.registerMessage(AlignmentMessage.ClientHandler.class, AlignmentMessage.AlignmentData.class, 1, Side.CLIENT);
	}

	public static final XSTR RANDOM = new XSTR();

	static Block blockHint;
	static Item itemBlockHint;
	public static final CreativeTabs creativeTab = new CreativeTabs("structurelib") {
		@Override
		@SideOnly(Side.CLIENT)
		public Item getTabIconItem() {
			return StructureLibAPI.getItemBlockHint();
		}
	};

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent e) {
		GameRegistry.registerBlock(blockHint = new BlockHint(), ItemBlockHint.class, "blockhint");
		itemBlockHint = ItemBlock.getItemFromBlock(StructureLibAPI.getBlockHint());
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
}
