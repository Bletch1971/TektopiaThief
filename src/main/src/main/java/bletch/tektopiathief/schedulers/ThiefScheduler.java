package bletch.tektopiathief.schedulers;

import bletch.common.schedulers.IScheduler;
import bletch.common.utils.TektopiaUtils;
import bletch.common.utils.TextUtils;
import bletch.tektopiathief.core.ModConfig;
import bletch.tektopiathief.entities.EntityThief;
import bletch.tektopiathief.utils.LoggerUtils;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.tangotek.tektopia.Village;
import net.tangotek.tektopia.structures.VillageStructure;
import net.tangotek.tektopia.structures.VillageStructureType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ThiefScheduler implements IScheduler {

    protected static Map<Village, Integer> gracePeriods = new HashMap<>();

    protected Boolean checkedVillages = false;
    protected Boolean resetDay = false;

    @Override
    public void resetDay() {
        if (this.resetDay)
            return;

        LoggerUtils.instance.info("ThiefScheduler - resetDay called", true);

        // if it is day time, then clear the village checks
        this.checkedVillages = false;
        this.resetDay = true;

        reduceGracePeriods();
    }

    @Override
    public void resetNight() {
    }

    @Override
    public void update(World world) {
        // do not process any further if we have already performed the check, it is raining, or it is day
        if (this.checkedVillages || world == null || world.isRaining() || !EntityThief.isWorkTime(world, 0))
            return;

        LoggerUtils.instance.info("ThiefScheduler - update called", true);

        this.resetDay = false;
        this.checkedVillages = true;

        if (world.getDifficulty() == EnumDifficulty.PEACEFUL && !ModConfig.thief.thiefSpawnsWhenPeaceful) {
            LoggerUtils.instance.info(TextUtils.translate("message.thief.peaceful"), true);
            return;
        }

        // get a list of the villages from the VillageManager
        List<Village> villages = TektopiaUtils.getVillages(world);
        if (villages == null || villages.isEmpty())
            return;

        // cycle through each village
        villages.forEach((v) -> {

            String villageName = v.getName();

            int gracePeriod = getGracePeriod(v);
            if (gracePeriod > 0) {
                LoggerUtils.instance.info(TextUtils.translate("message.thief.graceperiod", villageName), true);
                return;
            }

            VillageStructure storage = v.getNearestStructure(VillageStructureType.STORAGE, v.getOrigin());
            if (storage != null) {
                EntityItemFrame frame = storage.getItemFrame();
                if (frame != null && frame.getRotation() != 0) {
                    LoggerUtils.instance.info(TextUtils.translate("message.thief.spawnblocked", villageName), true);
                    return;
                }
            }

            // get the village level (1-5) and test to spawn - bigger villages will increase the number of spawns of the Thief.
            int villageLevel = TektopiaUtils.getVillageLevel(v);
            int villageCheck = ModConfig.thief.checksVillageSize ? world.rand.nextInt(TektopiaUtils.MAX_VILLAGE_LEVEL - villageLevel + 1) : 0;

            if (villageLevel > 0 && villageCheck == 0) {

                LoggerUtils.instance.info(TextUtils.translate("message.thief.villagechecksuccess", villageName, villageLevel, villageCheck), true);

                // get a list of the Thieves in the village
                List<EntityThief> entityList = world.getEntitiesWithinAABB(EntityThief.class, v.getAABB().grow(Village.VILLAGE_SIZE));
                if (entityList.size() == 0) {

                    BlockPos spawnPosition = TektopiaUtils.getVillageSpawnPoint(world, v);

                    // attempt spawn
                    if (TektopiaUtils.trySpawnPersistenceEntity(world, spawnPosition, (World w) -> new EntityThief(w, villageLevel))) {
                        LoggerUtils.instance.info(TextUtils.translate("message.thief.spawned.village", villageLevel, villageName, TektopiaUtils.formatBlockPos(spawnPosition)), true);
                    } else {
                        LoggerUtils.instance.info(TextUtils.translate("message.thief.noposition.village", villageName), true);
                    }

                } else {
                    LoggerUtils.instance.info(TextUtils.translate("message.thief.exists", villageName), true);
                }

            } else {
                LoggerUtils.instance.info(TextUtils.translate("message.thief.villagecheckfailed", villageName, villageLevel, villageCheck), true);
            }
        });
    }

    public static int getGracePeriod(Village village) {
        return village == null ? 0 : gracePeriods.getOrDefault(village, 0);
    }

    public static void reduceGracePeriods() {
    	List<Village> villages = new ArrayList<Village>(gracePeriods.keySet());
    	
    	for (int index = villages.size() - 1; index >= 0; index--) {
    		setGracePeriod(villages.get(index), gracePeriods.getOrDefault(villages.get(index), 0) - 1);
    	}
    }

    public static void resetGracePeriod(Village village) {
        if (village == null)
            return;

        setGracePeriod(village, ModConfig.thief.thiefgraceperiod);
    }

    public static void setGracePeriod(Village village, int gracePeriod) {
        if (village == null)
            return;

        if (gracePeriod <= 0)
            gracePeriods.remove(village);
        else
            gracePeriods.put(village, gracePeriod);
    }
}
