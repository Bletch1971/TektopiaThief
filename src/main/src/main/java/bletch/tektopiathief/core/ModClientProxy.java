package bletch.tektopiathief.core;

import java.io.File;

import javax.annotation.ParametersAreNonnullByDefault;

import com.leviathanstudio.craftstudio.client.registry.CSRegistryHelper;
import com.leviathanstudio.craftstudio.client.util.EnumRenderType;
import com.leviathanstudio.craftstudio.client.util.EnumResourceType;

import bletch.tektopiathief.utils.LoggerUtils;
import bletch.tektopiathief.entities.EntityThief;
import net.minecraft.client.Minecraft;

@ParametersAreNonnullByDefault
public class ModClientProxy extends ModCommonProxy {

	protected CSRegistryHelper registry = new CSRegistryHelper(ModDetails.MOD_ID);

	@Override
	public boolean isRemote() {
		return true;
	}

	@Override
	public File getMinecraftDirectory() {
		return Minecraft.getMinecraft().mcDataDir;
	}
	   
	@Override
	public void registerCraftStudioAnimations() {
		super.registerCraftStudioAnimations();
		
		LoggerUtils.info("Starting registerCraftStudioAnimations...");

		this.registry.register(EnumResourceType.ANIM, EnumRenderType.ENTITY, ModEntities.ANIMATION_VILLAGER_WALK);
		this.registry.register(EnumResourceType.ANIM, EnumRenderType.ENTITY, ModEntities.ANIMATION_VILLAGER_RUN);
		this.registry.register(EnumResourceType.ANIM, EnumRenderType.ENTITY, ModEntities.ANIMATION_VILLAGER_CREEP);
		
		LoggerUtils.info("Finished registerCraftStudioAnimations...");
	}	
	
	@Override
	public void registerCraftStudioModels() {
		super.registerCraftStudioModels();
		
		LoggerUtils.info("Starting registerCraftStudioModels...");
		
		registry.register(EnumResourceType.MODEL, EnumRenderType.ENTITY, EntityThief.ANIMATION_MODEL_NAME);
		
		LoggerUtils.info("Finished registerCraftStudioModels...");
	}
		
}
