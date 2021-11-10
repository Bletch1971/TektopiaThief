package bletch.tektopiathief.commands;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.permission.PermissionAPI;

import java.util.Collections;
import java.util.List;

public abstract class CommandThiefBase extends CommandBase {

    protected final String name;

    public CommandThiefBase(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList(this.name);
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return ThiefCommands.COMMAND_PREFIX + this.name + ".usage";
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        try {
            return PermissionAPI.hasPermission(getCommandSenderAsPlayer(sender), ThiefCommands.COMMAND_PREFIX_WITH_MODID + this.getName());
        } catch (PlayerNotFoundException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 4;
    }
}
