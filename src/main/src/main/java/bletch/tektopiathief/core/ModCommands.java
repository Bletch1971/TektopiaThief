package bletch.tektopiathief.core;

import bletch.tektopiathief.commands.CommandKill;
import bletch.tektopiathief.commands.CommandSpawn;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.command.CommandTreeBase;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;

public class ModCommands extends CommandTreeBase {

	public static final String COMMAND_PREFIX = "commands.thief.";
	public static final String COMMAND_PREFIX_WITH_MODID = ModDetails.MOD_ID + "." + COMMAND_PREFIX;
	
	public ModCommands() {
		super.addSubcommand(new CommandKill());
		super.addSubcommand(new CommandSpawn());
	}

	public void registerNodes() {
		this.getSubCommands().stream().forEach((c) -> {
			PermissionAPI.registerNode(COMMAND_PREFIX_WITH_MODID + c.getName(), DefaultPermissionLevel.OP, c.getName());
		});
	}
	
	@Override
	public String getName() {
		return "thief";
	}    
	
	@Override
	public int getRequiredPermissionLevel() {
		return 0;
    }
	
	@Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
		return true;
    }

	@Override
	public String getUsage(ICommandSender sender) {
		return COMMAND_PREFIX + "usage";
	}
}
