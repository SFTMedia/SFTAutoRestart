package com.blalp.sftautorestart.bukkit;

import java.util.ArrayList;
import java.util.logging.Level;

import com.blalp.sftautorestart.common.Lag;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SFTAutoRestart extends JavaPlugin {
	//thanks to https://github.com/FuzzyWuzzie/SimpleRestart/blob/master/src/main/java/com/hamaluik/SimpleRestart/SimpleRestart.java
	// and https://bukkit.org/threads/get-server-tps.143410/ for some code
	private static int counter;
	private static boolean enabled=true;
	private static boolean voteAllowed=true;
	private static boolean restartedLast;
	private static boolean forceTimings =false;
	private static int[] tpsTriggers = new int[3];
	private static boolean restart = false;
	public static ArrayList<Player> playersVoted = new ArrayList<Player>();
	@SuppressWarnings("deprecation")
	@Override
	public void onEnable(){
        getConfig().options().copyDefaults(true);
		saveConfig();
		enabled = getConfig().getBoolean("enabled");
		voteAllowed = getConfig().getBoolean("voteAllowed");
		forceTimings=getConfig().getBoolean("forceTimings");
		tpsTriggers[0]=getConfig().getInt("tpsThreashhold");//addTo counter test
		tpsTriggers[1]=getConfig().getInt("tpsEmergancyThreshold");//number of ticks if below will yield addition count
		tpsTriggers[2]=getConfig().getInt("ticksBelowThreshold");//number of counters needed for restart
	    Bukkit.getServer().getScheduler().scheduleAsyncRepeatingTask(this, new Lag(), 100L, 1L);
	    Bukkit.getPluginManager().registerEvents(new BukkitListeners(), this);
		Bukkit.getScheduler().runTaskTimerAsynchronously(this, scheduling(),5,5);
		new BukkitRunnable() {
			@Override
			public void run() {
				if(restart){
					restartServer();
				}
			}
		}.runTaskTimer(this, 5, 5);
		if(restartedLast||forceTimings){
        	Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "timings on");
        }
	}
	@Override
	public void onDisable(){
		if(forceTimings){
			Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "timings report");
		}
	}
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		if(cmd.getName().equals("voterestart")&&sender.hasPermission("sftautorestart.vote")){
			handleRestart(((Player)sender), args);
		}
		if(cmd.getName().equals("autorestart")){
			if(args.length>0&&args[0].equalsIgnoreCase("toggle")&&sender.hasPermission("sftautorestart.toggle")){
				if(enabled){
					enabled=false;
				} else {
					enabled=true;
					counter=0;
				}
				sender.sendMessage("[AutoRestart] Plugin enabled? "+enabled);
			} else if (args.length>0&&args[0].equalsIgnoreCase("vars")&&sender.hasPermission("sftautorestart.vars")){
				sender.sendMessage(new String[]{"[AutoRestart] enabled "+enabled,"tpsTriggers (0:tpsThreshold="+tpsTriggers[0]+",1:tpsEmergancyThreshold="+tpsTriggers[1]+",2:ticksBelowThreshold="+tpsTriggers[2]+") forceTimings "+forceTimings,"voteAllowed "+voteAllowed});
			} else if(args.length>0&&args[0].equalsIgnoreCase("varset")&&sender.hasPermission("sftautorestart.varset")){
				if(args.length==1){
					sender.sendMessage("Please include valid vars. "+StringUtilsjoin(new String[]{"enabled","tpsTriggers","forceTimings","voteAllowed"}," "));
				} else {
					if (args[1].equalsIgnoreCase("enabled")){
						if(args.length>3){
							sender.sendMessage("[AutoRestart] Too many args, please just put true or false after enabled.");
						} else {
							enabled=Boolean.parseBoolean(args[2]);
							getConfig().set("enabled", enabled);
							saveConfig();
						}
					} else if (args[1].equalsIgnoreCase("tpsTriggers")){
						if(args.length==4){
							tpsTriggers[Integer.parseInt(args[2])]=Integer.parseInt(args[3]);
							switch(Integer.parseInt(args[2])){
								case 0:
									getConfig().set("tpsThreashhold", Integer.parseInt(args[3]));
									break;
								case 1:
									getConfig().set("tpsEmergancyThreshold", Integer.parseInt(args[3]));
									break;
								case 2:
									getConfig().set("ticksBelowThreshold", Integer.parseInt(args[3]));
									break;
							}
							saveConfig();
							sender.sendMessage("[AutoRestart] Set tpsTriggers["+Integer.parseInt(args[2])+"] to ."+Integer.parseInt(args[3]));
						} else {
							sender.sendMessage("[AutoRestart] Wrong number of args. ");
						}
					} else if (args[1].equalsIgnoreCase("forceTimings")){
						if(args.length>3){
							sender.sendMessage("[AutoRestart] Too many args, please just put true or false after forceTimings.");
						} else {
							forceTimings=Boolean.parseBoolean(args[2]);
							getConfig().set("forceTimings", forceTimings);
							saveConfig();
							sender.sendMessage("[AutoRestart] Set forceTimings to: "+forceTimings);
						}
					} else if (args[1].equalsIgnoreCase("voteAllowed")){
						if(args.length>3){
							sender.sendMessage("[AutoRestart] Too many args, please just put true or false after voteAllowed.");
						} else {
							voteAllowed=Boolean.parseBoolean(args[2]);
							getConfig().set("voteAllowed", voteAllowed);
							saveConfig();
							sender.sendMessage("[AutoRestart] Set voteAllowed to: "+voteAllowed);
							if(Bukkit.getOnlinePlayers().length==playersVoted.size()&&voteAllowed){
								restartServer();
							}
						}
					} else {
						sender.sendMessage("Please include valid vars. "+StringUtilsjoin(new String[]{"enabled","tpsTriggers","forceTimings","voteAllowed"}," "));
					}
				}
			} else {
				if(sender.hasPermission("sftautorestart.help")){
					sender.sendMessage(new String[]{"------[AutoRestart]------","/autorestart vars - gets vars.","/autorestart varset [Var] [value]- sets var.","/autorestart toggle - toggles whether the plugin does anything or not."});
				}
			}
		}
		return false;
	}
	private static void restartServer() {
		try {
			Bukkit.getLogger().log(Level.WARNING, "[SFTAutoRestart] Restarting server!");
			Bukkit.getServer().broadcastMessage("[SFTAutoRestart] �7Restarting server!");
			if(restartedLast||forceTimings){
				Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "timings report");
			}
			for(Player player:Bukkit.getOnlinePlayers()){
				player.kickPlayer("Server restarting");
			}
			Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "save-all");
            Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "stop");
		} catch (Exception e) {
			Bukkit.getLogger().log(Level.WARNING, "[SFTAutoRestart] Something went wrong while saving & stoping!");
		}
	}
	public Runnable scheduling() {
		return new BukkitRunnable() {
			@Override
			public void run() {
				//Bukkit.getLogger().log(Level.WARNING, "[SFTAutoRestarter] testing tps "+Lag.getTPS());
				if(enabled){
					if(Lag.getTPS()<tpsTriggers[0]){
						if(Lag.getTPS()<tpsTriggers[1]){
							counter+=3;
						} else {
							counter++;
						}
						if(counter>=tpsTriggers[2]&&enabled){
							restart=true;
						}
						Bukkit.getLogger().log(Level.WARNING, "[SFTAutoRestarter] EE TPS LOW "+Lag.getTPS()+" counter at "+counter);
					} else {
						counter=0;
					}
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
					for(Player player:Bukkit.getOnlinePlayers()){
						if(player.hasPermission("sftautorestart.staff")&&!playersVoted.contains(player)){
							restart=false;
						}
					}
					if(restart&&voteAllowed){
						restartServer();
					}
				}
				if(Bukkit.getOnlinePlayers().length==playersVoted.size()&&voteAllowed){
					restartServer();
				}
			}
			if(Bukkit.getOnlinePlayers().length==playersVoted.size()&&voteAllowed){
				restartServer();
			}
			sender.sendMessage("[AutoRestart] Added your vote");
		} else if (args.length==1){
			if(!Boolean.parseBoolean(args[0])){
				playersVoted.remove(((Player)sender));
				sender.sendMessage("[AutoRestart] Removed your vote");
			}
			boolean found=false;
			for(Player player:playersVoted){
				if(player.getUniqueId().equals(((Player)sender).getUniqueId())){
					if(Bukkit.getOnlinePlayers().length==playersVoted.size()&&voteAllowed){
						restartServer();
					}
					found=true;
				}
			}
			if(!found&&Boolean.parseBoolean(args[0])){
				playersVoted.add(((Player)sender));
				sender.sendMessage("[AutoRestart] Added your vote");
			}
			if(Bukkit.getOnlinePlayers().length==playersVoted.size()&&voteAllowed){
				restartServer();
			}
		} else {
			sender.sendMessage(new String[]{"-----[AutoRestart]------","Too man args.","/voterestart - Votes for a restart","/voterestart no - sets your vote to no.","/voterestart yes - sets your vote to yes."});
		}
	}

}
