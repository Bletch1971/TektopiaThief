package bletch.tektopiathief.core;

import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.common.config.Config.Type;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Config(modid=ModDetails.MOD_ID, category="")
@ParametersAreNonnullByDefault
public class ModConfig {
	
	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void onConfigChanged(ConfigChangedEvent event) {
		
		if (event.getModID().equals(ModDetails.MOD_ID)) {
			ConfigManager.sync(ModDetails.MOD_ID, Type.INSTANCE);
		}
		
	}
	
	@Config.LangKey("config.debug")
	public static final Debug debug = new Debug();
	
	@Config.LangKey("config.thief")
	public static final Thief thief = new Thief();
	
	public static class Debug {
		
		@Config.Comment("If true, debug information will be output to the console.")
		@Config.LangKey("config.debug.enableDebug")
		public boolean enableDebug = false;	
		
	}
	
	public static class Thief {
		
		@Config.Comment("If enabled, when trying to spawn a thief it will check the size of the village. The more villagers the more often the thief will spawn. Default: True")
		@Config.LangKey("config.thief.checksvillagesize")
		public Boolean checksVillageSize = true;
		
		@Config.Comment("If enabled, the Thief will spawn even if the difficulty is set to Peaceful. Default: True")
		@Config.LangKey("config.thief.thiefspawnswhenpeaceful")
		public Boolean thiefSpawnsWhenPeaceful = true;
		
	}
	
}
