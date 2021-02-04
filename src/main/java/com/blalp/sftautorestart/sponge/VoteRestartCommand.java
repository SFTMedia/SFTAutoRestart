package com.blalp.sftautorestart.sponge;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

/**
 * SpongeCommand
 */
public class VoteRestartCommand implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext context) throws CommandException {
		if(src.getCommandSource().get() instanceof Player){
			String[] args;
			if(context.hasAny("args")) {
				args = context.<String>getOne(Text.of("args")).get().split(" ");
			} else {
				args = new String[]{};
			}
			SFTAutoRestart.handleRestart((Player)src.getCommandSource().get(), args);
		}
		return CommandResult.success();
    }

    
}