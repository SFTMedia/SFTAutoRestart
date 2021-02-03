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
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
		if(src.getCommandSource().get() instanceof Player){
			SFTAutoRestart.handleRestart((Player)src.getCommandSource().get(), args.<String>getOne(Text.of("args")).get().split(" "));
		}
		return CommandResult.success();
    }

    
}