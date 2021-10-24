package bletch.tektopiathief.commands;

import java.util.List;

import bletch.tektopiathief.core.ModCommands;
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

public class CommandSpawn extends ThiefCommandBase {

	private static final String COMMAND_NAME = "spawn";
	
	public CommandSpawn() {
		super(COMMAND_NAME);
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length < 1 || args.length > 2) {
			throw new WrongUsageException(ModCommands.COMMAND_PREFIX + COMMAND_NAME + ".usage", new Object[0]);
		}
		
		Boolean spawnNearMe = false;
		if (args.length > 1) {
			if (!args[1].equalsIgnoreCase("me")) {
				throw new WrongUsageException(ModCommands.COMMAND_PREFIX + COMMAND_NAME + ".usage", new Object[0]);
			}
			
			spawnNearMe = true;
		}

		int value = 0;
		try {
			value = Integer.parseInt(args[0]);
			value = Math.max(Math.min(value, EntityThief.MAX_LEVEL), EntityThief.MIN_LEVEL);
		}
		catch (Exception ex) {
			throw new WrongUsageException(ModCommands.COMMAND_PREFIX + COMMAND_NAME + ".usage", new Object[0]);
		}
		
		int level = value;
		
		EntityPlayer entityPlayer = super.getCommandSenderAsPlayer(sender);
		World world = entityPlayer != null ? entityPlayer.getEntityWorld() : null;
		
		if (world == null || world.isRaining()) {
			notifyCommandListener(sender, this, "commands.thief.spawn.badconditions", new Object[0]);
			LoggerUtils.info(TextUtils.translate("commands.thief.spawn.badconditions", new Object[0]), true);
			return;
		}
		
		if (world.getDifficulty() == EnumDifficulty.PEACEFUL && !ModConfig.thief.thiefSpawnsWhenPeaceful) {
			notifyCommandListener(sender, this, "commands.thief.spawn.peaceful", new Object[0]);
			LoggerUtils.info(TextUtils.translate("commands.thief.spawn.peaceful", new Object[0]), true);
			return;
		}
		
		VillageManager villageManager = world != null ? VillageManager.get(world) : null;
		Village village = villageManager != null && entityPlayer != null ? villageManager.getVillageAt(entityPlayer.getPosition()) : null;
		if (village == null) {
			notifyCommandListener(sender, this, "commands.thief.spawn.novillage", new Object[0]);
			LoggerUtils.info(TextUtils.translate("commands.thief.spawn.novillage", new Object[0]), true);
			return;
		}

		BlockPos spawnPosition = spawnNearMe ? entityPlayer.getPosition().north(2) : TektopiaUtils.getVillageSpawnPoint(world, village);
		
		if (spawnPosition == null) {
			notifyCommandListener(sender, this, "commands.thief.spawn.noposition", new Object[0]);
			LoggerUtils.info(TextUtils.translate("commands.thief.spawn.noposition", new Object[0]), true);
			return;
		}

        List<EntityThief> entityList = world.getEntitiesWithinAABB(EntityThief.class, village.getAABB().grow(Village.VILLAGE_SIZE));
        if (entityList.size() > 0) {
			notifyCommandListener(sender, this, "commands.thief.spawn.exists", new Object[0]);
			LoggerUtils.info(TextUtils.translate("commands.thief.spawn.exists", new Object[0]), true);
			return;
        }
        
		// attempt to spawn the thief
		if (!TektopiaUtils.trySpawnEntity(world, spawnPosition, (World w) -> new EntityThief(w, level))) {
			notifyCommandListener(sender, this, "commands.thief.spawn.failed", new Object[0]);
			LoggerUtils.info(TextUtils.translate("commands.thief.spawn.failed", new Object[0]), true);
			return;
		}
		
		notifyCommandListener(sender, this, "commands.thief.spawn.success", new Object[] { level, TektopiaUtils.formatBlockPos(spawnPosition) });
		LoggerUtils.info(TextUtils.translate("commands.thief.spawn.success", new Object[] { level, TektopiaUtils.formatBlockPos(spawnPosition) }), true);
	}
    
}
