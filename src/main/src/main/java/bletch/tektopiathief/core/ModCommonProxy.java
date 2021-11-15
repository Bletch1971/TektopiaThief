package bletch.tektopiathief.core;

import bletch.tektopiathief.utils.LoggerUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;

@ParametersAreNonnullByDefault
public class ModCommonProxy {

    public boolean isRemote() {
        return false;
    }

    public File getMinecraftDirectory() {
        return FMLCommonHandler.instance().getMinecraftServerInstance().getFile("");
    }

    public void preInitialize(FMLPreInitializationEvent e) {
        MinecraftForge.EVENT_BUS.register(new ModConfig());
    }

    public void initialize(FMLInitializationEvent e) {
    }

    public void postInitialize(FMLPostInitializationEvent e) {
    }

    public void registerCraftStudioAnimations() {
    }

    public void registerCraftStudioModels() {
    }

    public void resetDebug() {
		LoggerUtils.Initialise(ModDetails.MOD_NAME, getMinecraftDirectory() + ModDetails.FILE_DEBUGLOG);
		
        if (ModConfig.debug.enableDebug) {
            LoggerUtils.resetDebug();
        }
    }

}
