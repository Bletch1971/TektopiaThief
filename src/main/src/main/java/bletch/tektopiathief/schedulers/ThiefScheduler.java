package bletch.tektopiathief.schedulers;

import java.util.List;

import bletch.tektopiathief.core.ModConfig;
import bletch.tektopiathief.entities.EntityThief;
import bletch.tektopiathief.utils.TektopiaUtils;
import bletch.tektopiathief.utils.LoggerUtils;
import bletch.tektopiathief.utils.TextUtils;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.tangotek.tektopia.Village;
import net.tangotek.tektopia.structures.VillageStructure;
import net.tangotek.tektopia.structures.VillageStructureType;

public class ThiefScheduler implements IScheduler {

	protected Boolean checkedVillages = false;
	protected Boolean resetDay = false;

	@Override
	public void resetDay() {
		if (this.resetDay)
			return;

		// if it is day time, then clear the village checks
		this.checkedVillages = false;
		this.resetDay = true;
	}

	@Override
	public void resetNight() {
	}

	@Override
	public void update(World world) {
		// do not process any further if we have already performed the check, it is raining or it is day
		if (this.checkedVillages || world == null || world.isRaining() || !Village.isNightTime(world))
			return;
		
		this.resetDay = false;
		this.checkedVillages = true;

		if (world.getDifficulty() == EnumDifficulty.PEACEFUL && !ModConfig.thief.thiefSpawnsWhenPeaceful) {
			LoggerUtils.info(TextUtils.translate("message.thief.peaceful", new Object[0]), true);
			return;
		}
		
		// get a list of the villages from the VillageManager 
		List<Village> villages = TektopiaUtils.getVillages(world);
		if (villages == null || villages.isEmpty())
			return;

		// cycle through each village
		villages.forEach((v) -> {
			
			String villageName = v.getName();
			
			VillageStructure storage = v.getNearestStructure(VillageStructureType.STORAGE, v.getOrigin());
			if (storage != null) {
				EntityItemFrame frame = storage.getItemFrame();
				if (frame != null && frame.getRotation() != 0) {
					LoggerUtils.info(TextUtils.translate("message.thief.spawnblocked", new Object[] { villageName }), true);
					return;
				}
			}

			// get the village level (1-5) and test to spawn - bigger villages will increase the number of spawns of the Thief.
			int villageLevel = TektopiaUtils.getVillageLevel(v);
			int villageCheck = ModConfig.thief.checksVillageSize ? world.rand.nextInt(TektopiaUtils.MAX_VILLAGE_LEVEL - villageLevel + 1) : 0;
			
			if (villageLevel > 0 && villageCheck == 0) {
				
				LoggerUtils.info(TextUtils.translate("message.thief.villagechecksuccess", new Object[] { villageName, villageLevel, villageCheck }), true);
				
				// get a list of the Thieves in the village
				List<EntityThief> entityList = world.getEntitiesWithinAABB(EntityThief.class, v.getAABB().grow(Village.VILLAGE_SIZE));
				if (entityList.size() == 0) {
					
					BlockPos spawnPosition = TektopiaUtils.getVillageSpawnPoint(world, v);
					
					// attempt spawn
					if (TektopiaUtils.trySpawnEntity(world, spawnPosition, (World w) -> new EntityThief(w, villageLevel))) {
						LoggerUtils.info(TextUtils.translate("message.thief.spawned.village", new Object[] { villageName, TektopiaUtils.formatBlockPos(spawnPosition) }), true);
					} else {
						LoggerUtils.info(TextUtils.translate("message.thief.noposition.village", new Object[] { villageName }), true);
					}
					
				} else {
					LoggerUtils.info(TextUtils.translate("message.thief.exists", new Object[] { villageName }), true);
				}
				
			} else {
				LoggerUtils.info(TextUtils.translate("message.thief.villagecheckfailed", new Object[] { villageName, villageLevel, villageCheck }), true);
			}
		});
	}
}