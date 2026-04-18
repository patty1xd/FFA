package com.ffa.listeners;

import com.ffa.FFAPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinListener implements Listener {

    private final FFAPlugin plugin;

    public PlayerJoinListener(FFAPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        plugin.getTierManager().initPlayer(player.getUniqueId());
        plugin.getBoardManager().updatePlayer(player);
        plugin.getBoardManager().updateNameTag(player);
        player.sendMessage("§8[§6FFA§8] §7Welcome! Right-click the §eKit §7NPC to get your kit.");
        player.sendMessage("§8[§6FFA§8] §7Your tier: " + plugin.getTierManager().getTierDisplay(plugin.getTierManager().getTier(player.getUniqueId())));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getTierManager().saveAll();
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        event.setCancelled(true);
        var player = event.getPlayer();
        String tierDisplay = plugin.getTierManager().getTierDisplay(plugin.getTierManager().getTier(player.getUniqueId()));
        String formatted = tierDisplay + " §f" + player.getName() + " §7» §f" + event.getMessage();
        player.getServer().broadcastMessage(formatted);
    }
}
