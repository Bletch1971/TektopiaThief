package bletch.tektopiathief.utils;

import net.minecraft.entity.EntityLiving;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import net.tangotek.tektopia.Village;
import net.tangotek.tektopia.VillageManager;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TektopiaUtils {

    public static final int MAX_VILLAGE_LEVEL = 5;
    public static final int MIN_VILLAGE_LEVEL = 1;

    public static String formatBlockPos(BlockPos blockPos) {
        if (blockPos == null) {
            return "";
        }

        return blockPos.getX() + ", " + blockPos.getY() + ", " + blockPos.getZ();
    }

    public static List<Village> getVillages(World world) {
        if (world == null)
            return null;

        VillageManager villageManager = VillageManager.get(world);
        if (villageManager == null)
            return null;

        try {
            Field field = VillageManager.class.getDeclaredField("villages");
            if (field != null) {
                field.setAccessible(true);

                Object fieldValue = field.get(villageManager);
                if (fieldValue instanceof Set<?>) {
                    return ((Set<?>) fieldValue).stream()
                            .filter(v -> v instanceof Village)
                            .map(v -> (Village) v)
                            .filter(v -> v.isValid())
                            .collect(Collectors.toList());
                }
            }
        } catch (Exception ex) {
            //do nothing if an error was encountered
        }

        return null;
    }

    public static int getVillageLevel(Village village) {
        if (village == null)
            return 0;

        int residentCount = village.getResidentCount();
        return Math.max(Math.min(residentCount / 10, MAX_VILLAGE_LEVEL), MIN_VILLAGE_LEVEL);
    }

    public static BlockPos getVillageSpawnPoint(World world, Village village) {
        int retries = 3;

        while (retries-- > 0) {
            BlockPos spawnPosition = village.getEdgeNode();

            if (isChunkFullyLoaded(world, spawnPosition)) {
                return spawnPosition;
            }
        }

        return null;
    }

    public static boolean isChunkFullyLoaded(World world, BlockPos pos) {
        if (world == null || world.isRemote || pos == null) {
            return true;
        }

        long i = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
        Chunk chunk = ((ChunkProviderServer) world.getChunkProvider()).id2ChunkMap.get(i);
        return chunk != null && !chunk.unloadQueued;
    }

    public static boolean trySpawnEntity(World world, BlockPos spawnPosition, Function<World, ?> createFunc) {
        if (world == null || spawnPosition == null || createFunc == null)
            return false;

        EntityLiving entity = (EntityLiving) createFunc.apply(world);
        if (entity == null)
            return false;

        entity.setLocationAndAngles((double) spawnPosition.getX() + 0.5D, spawnPosition.getY(), (double) spawnPosition.getZ() + 0.5D, 0.0F, 0.0F);
        entity.onInitialSpawn(world.getDifficultyForLocation(spawnPosition), null);
        entity.enablePersistence();
        return world.spawnEntity(entity);
    }

}
