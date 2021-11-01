package bletch.tektopiathief.commands;

import java.util.List;

import bletch.tektopiathief.core.ModConfig;
import bletch.tektopiathief.entities.EntityThief;
import bletch.tektopiathief.utils.LoggerUtils;
import bletch.tektopiathief.utils.TektopiaUtils;
import bletch.tektopiathief.utils.TextUtils;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.tangotek.tektopia.Village;
import net.tangotek.tektopia.VillageManager;

public class CommandThiefSpawn extends CommandThiefBase {

	private static final String COMMAND_NAME = "spawn";
	
	public CommandThiefSpawn() {
		super(COMMAND_NAME);
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length < 1 || args.length > 2) {
			throw new WrongUsageException(ThiefCommands.COMMAND_PREFIX + COMMAND_NAME + ".usage", new Object[0]);
		}
		
		Boolean spawnNearMe = false;
		if (args.length > 1) {
			if (!args[1].equalsIgnoreCase("me")) {
				throw new WrongUsageException(ThiefCommands.COMMAND_PREFIX + COMMAND_NAME + ".usage", new Object[0]);
			}
			
			spawnNearMe = true;
		}

		int argValue = 0;
		try {
			argValue = Integer.parseInt(args[0]);
			argValue = Math.max(Math.min(argValue, EntityThief.MAX_LEVEL), EntityThief.MIN_LEVEL);
		}
		catch (Exception ex) {
			throw new WrongUsageException(ThiefCommands.COMMAND_PREFIX + COMMAND_NAME + ".usage", new Object[0]);
		}
		
		int level = argValue;
		
		EntityPlayer entityPlayer = super.getCommandSenderAsPlayer(sender);
		World world = entityPlayer != null ? entityPlayer.getEntityWorld() : null;
		
		if (world == null || world.isRaining()) {
			notifyCommandListener(sender, this, ThiefCommands.COMMAND_PREFIX + COMMAND_NAME + ".badconditions", new Object[0]);
			LoggerUtils.info(TextUtils.translate(ThiefCommands.COMMAND_PREFIX + COMMAND_NAME + ".badconditions", new Object[0]), true);
			return;
		}
		
		if (world.getDifficulty() == EnumDifficulty.PEACEFUL && !ModConfig.thief.thiefSpawnsWhenPeaceful) {
			notifyCommandListener(sender, this, ThiefCommands.COMMAND_PREFIX + COMMAND_NAME + ".peaceful", new Object[0]);
			LoggerUtils.info(TextUtils.translate(ThiefCommands.COMMAND_PREFIX + COMMAND_NAME + ".peaceful", new Object[0]), true);
			return;
		}
		
		VillageManager villageManager = world != null ? VillageManager.get(world) : null;
		Village village = villageManager != null && entityPlayer != null ? villageManager.getVillageAt(entityPlayer.getPosition()) : null;
		if (village == null) {
			notifyCommandListener(sender, this, ThiefCommands.COMMAND_PREFIX + COMMAND_NAME + ".novillage", new Object[0]);
			LoggerUtils.info(TextUtils.translate(ThiefCommands.COMMAND_PREFIX + COMMAND_NAME + ".novillage", new Object[0]), true);
			return;
		}

		BlockPos spawnPosition = spawnNearMe ? entityPlayer.getPosition() : TektopiaUtils.getVillageSpawnPoint(world, village);
		
		if (spawnPosition == null) {
			notifyCommandListener(sender, this, ThiefCommands.COMMAND_PREFIX + COMMAND_NAME + ".noposition", new Object[0]);
			LoggerUtils.info(TextUtils.translate(ThiefCommands.COMMAND_PREFIX + COMMAND_NAME + ".noposition", new Object[0]), true);
			return;
		}

        List<EntityThief> entityList = world.getEntitiesWithinAABB(EntityThief.class, village.getAABB().grow(Village.VILLAGE_SIZE));
        if (entityList.size() > 0) {
			notifyCommandListener(sender, this, ThiefCommands.COMMAND_PREFIX + COMMAND_NAME + ".exists", new Object[0]);
			LoggerUtils.info(TextUtils.translate(ThiefCommands.COMMAND_PREFIX + COMMAND_NAME + ".exists", new Object[0]), true);
			return;
        }
        
		// attempt to spawn the thief
		if (!TektopiaUtils.trySpawnEntity(world, spawnPosition, (World w) -> new EntityThief(w, level))) {
			notifyCommandListener(sender, this, ThiefCommands.COMMAND_PREFIX + COMMAND_NAME + ".failed", new Object[0]);
			LoggerUtils.info(TextUtils.translate(ThiefCommands.COMMAND_PREFIX + COMMAND_NAME + ".failed", new Object[0]), true);
			return;
		}
		
		notifyCommandListener(sender, this, ThiefCommands.COMMAND_PREFIX + COMMAND_NAME + ".success", new Object[] { TektopiaUtils.formatBlockPos(spawnPosition) });
		LoggerUtils.info(TextUtils.translate(ThiefCommands.COMMAND_PREFIX + COMMAND_NAME + ".success", new Object[] { TektopiaUtils.formatBlockPos(spawnPosition) }), true);
	}
    
}
