package bletch.tektopiathief.commands;

import bletch.common.commands.CommonCommandBase;
import bletch.common.utils.TektopiaUtils;
import bletch.common.utils.TextUtils;
import bletch.tektopiathief.core.ModConfig;
import bletch.tektopiathief.core.ModDetails;
import bletch.tektopiathief.entities.EntityThief;
import bletch.tektopiathief.utils.LoggerUtils;
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

import java.util.List;

public class CommandThiefSpawn extends CommonCommandBase {

    private static final String COMMAND_NAME = "spawn";

    public CommandThiefSpawn() {
        super(ModDetails.MOD_ID, ThiefCommands.COMMAND_PREFIX, COMMAND_NAME);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1 || args.length > 2) {
            throw new WrongUsageException(this.prefix + COMMAND_NAME + ".usage");
        }

        boolean spawnNearMe = false;
        if (args.length > 1) {
            if (!args[1].equalsIgnoreCase("me")) {
                throw new WrongUsageException(this.prefix + COMMAND_NAME + ".usage");
            }

            spawnNearMe = true;
        }

        int argValue;
        try {
            argValue = Integer.parseInt(args[0]);
            argValue = Math.max(Math.min(argValue, EntityThief.MAX_LEVEL), EntityThief.MIN_LEVEL);
        } catch (Exception ex) {
            throw new WrongUsageException(this.prefix + COMMAND_NAME + ".usage");
        }

        int level = argValue;

        EntityPlayer entityPlayer = getCommandSenderAsPlayer(sender);
        World world = entityPlayer != null ? entityPlayer.getEntityWorld() : null;

        if (world == null || world.isRaining()) {
            notifyCommandListener(sender, this, this.prefix + COMMAND_NAME + ".badconditions");
            LoggerUtils.instance.info(TextUtils.translate(this.prefix + COMMAND_NAME + ".badconditions"), true);
            return;
        }

        if (world.getDifficulty() == EnumDifficulty.PEACEFUL && !ModConfig.thief.thiefSpawnsWhenPeaceful) {
            notifyCommandListener(sender, this, this.prefix + COMMAND_NAME + ".peaceful");
            LoggerUtils.instance.info(TextUtils.translate(this.prefix + COMMAND_NAME + ".peaceful"), true);
            return;
        }

        VillageManager villageManager = world != null ? VillageManager.get(world) : null;
        Village village = villageManager != null && entityPlayer != null ? villageManager.getVillageAt(entityPlayer.getPosition()) : null;
        if (village == null) {
            notifyCommandListener(sender, this, this.prefix + COMMAND_NAME + ".novillage");
            LoggerUtils.instance.info(TextUtils.translate(this.prefix + COMMAND_NAME + ".novillage"), true);
            return;
        }

        BlockPos spawnPosition = spawnNearMe ? entityPlayer.getPosition() : TektopiaUtils.getVillageSpawnPoint(world, village);

        if (spawnPosition == null) {
            notifyCommandListener(sender, this, this.prefix + COMMAND_NAME + ".noposition");
            LoggerUtils.instance.info(TextUtils.translate(this.prefix + COMMAND_NAME + ".noposition"), true);
            return;
        }

        List<EntityThief> entityList = world.getEntitiesWithinAABB(EntityThief.class, village.getAABB().grow(Village.VILLAGE_SIZE));
        if (entityList.size() > 0) {
            notifyCommandListener(sender, this, this.prefix + COMMAND_NAME + ".exists");
            LoggerUtils.instance.info(TextUtils.translate(this.prefix + COMMAND_NAME + ".exists"), true);
            return;
        }

        // attempt to spawn the thief
        if (!TektopiaUtils.trySpawnPersistenceEntity(world, spawnPosition, (World w) -> new EntityThief(w, level))) {
            notifyCommandListener(sender, this, this.prefix + COMMAND_NAME + ".failed");
            LoggerUtils.instance.info(TextUtils.translate(this.prefix + COMMAND_NAME + ".failed"), true);
            return;
        }

        notifyCommandListener(sender, this, this.prefix + COMMAND_NAME + ".success", TektopiaUtils.formatBlockPos(spawnPosition));
        LoggerUtils.instance.info(TextUtils.translate(this.prefix + COMMAND_NAME + ".success", TektopiaUtils.formatBlockPos(spawnPosition)), true);
    }

}
