package bletch.tektopiathief.commands;

import bletch.common.commands.CommonCommandBase;
import bletch.common.utils.TextUtils;
import bletch.tektopiathief.core.ModDetails;
import bletch.tektopiathief.entities.EntityThief;
import bletch.tektopiathief.utils.LoggerUtils;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.tangotek.tektopia.Village;
import net.tangotek.tektopia.VillageManager;

import java.util.List;

public class CommandThiefKill extends CommonCommandBase {

    private static final String COMMAND_NAME = "kill";

    public CommandThiefKill() {
        super(ModDetails.MOD_ID, ThiefCommands.COMMAND_PREFIX, COMMAND_NAME);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length > 0) {
            throw new WrongUsageException(this.prefix + COMMAND_NAME + ".usage");
        }

        EntityPlayer entityPlayer = getCommandSenderAsPlayer(sender);
        World world = entityPlayer != null ? entityPlayer.getEntityWorld() : null;

        VillageManager villageManager = world != null ? VillageManager.get(world) : null;
        Village village = villageManager != null && entityPlayer != null ? villageManager.getVillageAt(entityPlayer.getPosition()) : null;
        if (village == null) {
            notifyCommandListener(sender, this, this.prefix + COMMAND_NAME + ".novillage");
            LoggerUtils.info(TextUtils.translate(this.prefix + COMMAND_NAME + ".novillage"), true);
            return;
        }

        List<EntityThief> entityList = world.getEntitiesWithinAABB(EntityThief.class, village.getAABB().grow(Village.VILLAGE_SIZE));
        if (entityList.size() == 0) {
            notifyCommandListener(sender, this, this.prefix + COMMAND_NAME + ".noexists");
            LoggerUtils.info(TextUtils.translate(this.prefix + COMMAND_NAME + ".noexists"), true);
            return;
        }

        for (EntityThief entity : entityList) {
            if (entity.isDead)
                continue;

            entity.setDead();

            notifyCommandListener(sender, this, this.prefix + COMMAND_NAME + ".success");
            LoggerUtils.info(TextUtils.translate(this.prefix + COMMAND_NAME + ".success"), true);
        }
    }

}
