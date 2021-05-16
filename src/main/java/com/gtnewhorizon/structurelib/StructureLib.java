package com.gtnewhorizon.structurelib;

import com.gtnewhorizon.structurelib.proxy.CommonProxy;
import com.gtnewhorizon.structurelib.util.XSTR;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = StructureLib.MOD_ID, name = "StructureLib", version = "${version}", acceptableRemoteVersions = "*", canBeDeactivated = false)
public class StructureLib {
	public static boolean DEBUG_MODE = true;
	public static Logger LOGGER = LogManager.getLogger("StructureLib");
	public static final String MOD_ID = "structurelib";
	@SidedProxy(serverSide = "com.gtnewhorizon.structurelib.proxy.CommonProxy", clientSide = "com.gtnewhorizon.structurelib.proxy.ClientProxy")
	public static CommonProxy proxy;
	public static final XSTR RANDOM = new XSTR();
}
