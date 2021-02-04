package com.blalp.sftautorestart.sponge;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

import com.blalp.sftautorestart.common.Lag;
import com.google.inject.Inject;

import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppedServerEvent;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.command.spec.CommandSpec.Builder;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginManager;
import org.spongepowered.api.text.Text;

import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;

@Plugin(id = "sftautorestart", name = "SFTAutoRestart", version = "2.0.6", description = "Vote and Autorestart.")
public class SFTAutoRestart implements CommandExecutor {
	// thanks to
	// https://github.com/FuzzyWuzzie/SimpleRestart/blob/master/src/main/java/com/hamaluik/SimpleRestart/SimpleRestart.java
	// and https://bukkit.org/threads/get-server-tps.143410/ for some code
	private static int counter;
	private static boolean enabled = true;
	private static boolean voteAllowed = true;
	private static boolean restartedLast;
	private static boolean forceTimings = false;
	private static int[] tpsTriggers = new int[3];
	private static boolean restart = false;
	public static ArrayList<Player> playersVoted = new ArrayList<Player>();

	@Inject
	@DefaultConfig(sharedRoot = false)
	private Path defaultConfig;

	@Inject
	@DefaultConfig(sharedRoot = false)
	private ConfigurationLoader<CommentedConfigurationNode> configManager;
	private CommentedConfigurationNode configRoot;

	@Inject
	private PluginManager pluginManager;

	@Inject
	private Logger logger;

	@Inject
	private static Logger loggerInstance;

	@Listener
	public void onServerStart(GameStartedServerEvent e) {
		loggerInstance = logger;
		try {
			configRoot = configManager.load();
			if(!defaultConfig.toFile().exists()){
				Sponge.getAssetManager().getAsset(this, "default.conf").get().copyToFile(defaultConfig);
			}
		} catch (IOException exception) {
			exception.printStackTrace();
		}
        Builder myCommandSpec = CommandSpec.builder().permission("sftautorestart.vote").executor(new VoteRestartCommand()).arguments(GenericArguments.optional(GenericArguments.remainingRawJoinedStrings(Text.of("args"))));
		Sponge.getCommandManager().register(this, myCommandSpec.build(), "voterestart");
        myCommandSpec = CommandSpec.builder().permission("sftautorestart.admin").executor(this).arguments(GenericArguments.optional(GenericArguments.remainingRawJoinedStrings(Text.of("args"))));
		Sponge.getCommandManager().register(this, myCommandSpec.build(), "autorestart");
		enabled = configRoot.getNode("enabled").getBoolean();
		voteAllowed = configRoot.getNode("voteAllowed").getBoolean();
		forceTimings = configRoot.getNode("forceTimings").getBoolean();
		tpsTriggers[0] = configRoot.getNode("tpsThreshhold").getInt();// addTo counter test
		tpsTriggers[1] = configRoot.getNode("tpsEmergencyThreshold").getInt();// number of ticks if below will yield
																				// addition count
		tpsTriggers[2] = configRoot.getNode("ticksBelowThreshold").getInt();// number of counters needed for restart
		Lag.waitTime = 10;
		new Thread(new Lag()).start();
		new Thread(scheduling()).start();
		if (restartedLast || forceTimings) {
			Sponge.getCommandManager().process(Sponge.getServer().getConsole(), "sponge timings on");
		}
	}

	@Listener
	public void onServerStop(GameStoppedServerEvent e) {
		if (forceTimings) {
			Sponge.getCommandManager().process(Sponge.getServer().getConsole(), "sponge timings report");
		}
	}

	private static void restartServer() {
		try {
			loggerInstance.warn("[SFTAutoRestart] Restarting server!");
			Sponge.getServer().getBroadcastChannel().send(Text.of("[SFTAutoRestart] �7Restarting server!"));
			if (restartedLast || forceTimings) {
				Sponge.getCommandManager().process(Sponge.getServer().getConsole(), "sponge timings report");
			}
			for (Player player : Sponge.getServer().getOnlinePlayers()) {
				player.kick(Text.of("Server restarting"));
			}
			Sponge.getCommandManager().process(Sponge.getServer().getConsole(), "save-all");
			Sponge.getCommandManager().process(Sponge.getServer().getConsole(), "stop");
		} catch (Exception e) {
			loggerInstance.warn("[SFTAutoRestart] Something went wrong while saving & stoping!");
		}
	}

	public Runnable scheduling() {
		return new Runnable() {
			@Override
			public void run() {
				// Bukkit.getLogger().log(Level.WARNING, "[SFTAutoRestarter] testing tps
				// "+Lag.getTPS());
				if (enabled) {
					if (Lag.getTPS() < tpsTriggers[0]) {
						if (Lag.getTPS() < tpsTriggers[1]) {
							counter += 3;
						} else {
							counter++;
						}
						if (counter >= tpsTriggers[2] && enabled) {
							restart = true;
						}
						loggerInstance.info("[SFTAutoRestarter] EE TPS LOW " + Lag.getTPS() + " counter at " + counter);
					} else {
						counter = 0;
					}
				}
				if (restart) {
					restartServer();
				}
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
	}
	public static String StringUtilsjoin(String[] args,String sep){
		boolean first = true;
		String output = "";
		for (String arg:args){
			if(first) {
				first=false;
			} else {
				output +=sep;
			}
			output+=arg;
		}
		return output;
	}
	public static void handleRestart(Player sender,String[] args){
		if(args.length==0){
			boolean found=false;
			for(Player player:playersVoted){
				if(player.getUniqueId().equals(((Player)sender).getUniqueId())){
					found=true;
				}
			}
			if(!found){
				playersVoted.add(((Player)sender));
				if(sender.hasPermission("sftautorestart.staff")){
					boolean restart = true;
					for(Player player:Sponge.getServer().getOnlinePlayers()){
						if(player.hasPermission("sftautorestart.staff")&&!playersVoted.contains(player)){
							restart=false;
						}
					}
					if(restart&&voteAllowed){
						restartServer();
					}
				}
				if(Sponge.getServer().getOnlinePlayers().size()==playersVoted.size()&&voteAllowed){
					restartServer();
				}
			}
			if(Sponge.getServer().getOnlinePlayers().size()==playersVoted.size()&&voteAllowed){
				restartServer();
			}
			sender.sendMessage(Text.of("[AutoRestart] Added your vote"));
		} else if (args.length==1){
			if(!Boolean.parseBoolean(args[0])){
				playersVoted.remove(((Player)sender));
				sender.sendMessage(Text.of("[AutoRestart] Removed your vote"));
			}
			boolean found=false;
			for(Player player:playersVoted){
				if(player.getUniqueId().equals(((Player)sender).getUniqueId())){
					if(Sponge.getServer().getOnlinePlayers().size()==playersVoted.size()&&voteAllowed){
						restartServer();
					}
					found=true;
				}
			}
			if(!found&&Boolean.parseBoolean(args[0])){
				playersVoted.add(((Player)sender));
				sender.sendMessage(Text.of("[AutoRestart] Added your vote"));
			}
			if(Sponge.getServer().getOnlinePlayers().size()==playersVoted.size()&&voteAllowed){
				restartServer();
			}
		} else {
			sender.sendMessage(Text.of("-----[AutoRestart]------\n","Too man args.","/voterestart - Votes for a restart\n","/voterestart no - sets your vote to no.\n","/voterestart yes - sets your vote to yes."));
		}
	}
    
    @Listener
    public void onChat(MessageChannelEvent.Chat event) {
		if(event.getCause().containsType(Player.class)) {
			if(event.getMessage().toPlain().equalsIgnoreCase("restart")){
				SFTAutoRestart.handleRestart(event.getCause().first(Player.class).get(),new String[]{});
			} else if (event.getMessage().toPlain().equalsIgnoreCase("don't restart")){
				SFTAutoRestart.handleRestart(event.getCause().first(Player.class).get(), new String[]{"false"});
			}
		}
    }

    @Listener
    public void onLogout(ClientConnectionEvent.Disconnect event) {
		if(event.getCause().containsType(Player.class)) {
			SFTAutoRestart.playersVoted.remove(event.getCause().first(Player.class).get());
		}
	}

	private void saveConfig(){
		try { 
			configManager.save(configRoot);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
    @Override
    public CommandResult execute(CommandSource src, CommandContext context) throws CommandException {
		String[] args;
		if(context.hasAny("args")) {
			args = context.<String>getOne(Text.of("args")).get().split(" ");
		} else {
			args = new String[]{};
		}
		CommandSource sender = src.getCommandSource().get();
		if(args.length>0&&args[0].equalsIgnoreCase("toggle")&&sender.hasPermission("sftautorestart.toggle")){
			if(enabled){
				enabled=false;
			} else {
				enabled=true;
				counter=0;
			}
			sender.sendMessage(Text.of("[AutoRestart] Plugin enabled? "+enabled));
		} else if (args.length>0&&args[0].equalsIgnoreCase("vars")&&sender.hasPermission("sftautorestart.vars")){
			sender.sendMessage(Text.of("[AutoRestart] enabled "+enabled,"tpsTriggers (\ntpsThreshold="+SFTAutoRestart.tpsTriggers[0]+",\ntpsEmergancyThreshold="+tpsTriggers[1]+",\nticksBelowThreshold="+tpsTriggers[2]+")\n forceTimings "+forceTimings,"\nvoteAllowed "+voteAllowed));
		} else if(args.length>0&&args[0].equalsIgnoreCase("varset")&&sender.hasPermission("sftautorestart.varset")){
			if(args.length==1){
				sender.sendMessage(Text.of("Please include valid vars. "+StringUtilsjoin(new String[]{"enabled","tpsTriggers","forceTimings","voteAllowed"}," ")));
			} else {
				if (args[1].equalsIgnoreCase("enabled")){
					if(args.length>=3){
						sender.sendMessage(Text.of("[AutoRestart] Too many args, please just put true or false after enabled."));
					} else {
						enabled=Boolean.parseBoolean(args[2]);
						configRoot.getNode("enabled").setValue(enabled);
						saveConfig();
					}
				} else if (args[1].equalsIgnoreCase("tpsTriggers")){
					if(args.length==4){
						tpsTriggers[Integer.parseInt(args[2])]=Integer.parseInt(args[3]);
						switch(Integer.parseInt(args[2])){
							case 0:
								configRoot.getNode("tpsThreshhold").setValue(Integer.parseInt(args[3]));
								break;
							case 1:
								configRoot.getNode("tpsEmergencyThreshold").setValue(Integer.parseInt(args[3]));
								break;
							case 2:
								configRoot.getNode("ticksBelowThreshold").setValue(Integer.parseInt(args[3]));
								break;
						}
						saveConfig();
						sender.sendMessage(Text.of("[AutoRestart] Set tpsTriggers["+Integer.parseInt(args[2])+"] to ."+Integer.parseInt(args[3])));
					} else {
						sender.sendMessage(Text.of("[AutoRestart] Wrong number of args. "));
					}
				} else if (args[1].equalsIgnoreCase("forceTimings")){
					if(args.length>3){
						sender.sendMessage(Text.of("[AutoRestart] Too many args, please just put true or false after forceTimings."));
					} else {
						forceTimings=Boolean.parseBoolean(args[2]);
						configRoot.getNode("forceTimings").setValue(forceTimings);
						saveConfig();
						sender.sendMessage(Text.of("[AutoRestart] Set forceTimings to: "+forceTimings));
					}
				} else if (args[1].equalsIgnoreCase("voteAllowed")){
					if(args.length>3){
						sender.sendMessage(Text.of("[AutoRestart] Too many args, please just put true or false after voteAllowed."));
					} else {
						voteAllowed=Boolean.parseBoolean(args[2]);
						configRoot.getNode("voteAllowed").setValue(voteAllowed);
						saveConfig();
						sender.sendMessage(Text.of("[AutoRestart] Set voteAllowed to: "+voteAllowed));
						if(Sponge.getServer().getOnlinePlayers().size()==playersVoted.size()&&voteAllowed){
							restartServer();
						}
					}
				} else {
					sender.sendMessage(Text.of("Please include valid vars. "+StringUtilsjoin(new String[]{"enabled","tpsTriggers","forceTimings","voteAllowed"}," ")));
				}
			}
		} else {
			if(sender.hasPermission("sftautorestart.help")){
				sender.sendMessage(Text.of("------[AutoRestart]------\n","/autorestart vars - gets vars.\n","/autorestart varset [Var] [value]- sets var.\n","/autorestart toggle - toggles whether the plugin does anything or not.\n"));
			}
		}
		return CommandResult.success();
    }

}