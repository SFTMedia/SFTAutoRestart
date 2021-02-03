package com.blalp.sftautorestart.bukkit;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class BukkitListeners implements Listener {
	@EventHandler
	private static void onLeave(PlayerQuitEvent event){
		SFTAutoRestart.playersVoted.remove(event.getPlayer());
	}
	@EventHandler
	private static void onChat(AsyncPlayerChatEvent event){
		if(event.getMessage().equalsIgnoreCase("restart")){
			SFTAutoRestart.handleRestart(event.getPlayer(), new String[]{});
		} else if (event.getMessage().equalsIgnoreCase("don't restart")){
			SFTAutoRestart.handleRestart(event.getPlayer(), new String[]{"false"});
		}
	}
}
