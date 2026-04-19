package com.ffa.listeners;

import com.ffa.FFAPlugin;
import org.bukkit.Bukkit;
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

        String tierDisplay = plugin.getTierManager().getTierDisplay(plugin.getTierManager().getTier(player.getUniqueId()));

        // Custom join message
        event.setJoinMessage(null);
        Bukkit.broadcastMessage("§8§m            §r §5§lTIERSTERMC §8§m            ");
        Bukkit.broadcastMessage("  §7Welcome, " + tierDisplay + " §f" + player.getName() + "§7!");
        Bukkit.broadcastMessage("  §7There are now §e" + Bukkit.getOnlinePlayers().size() + " §7players online.");
        Bukkit.broadcastMessage("§8§m            §r §5§l⚔ §8§m            ");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getTierManager().saveAll();
        event.setQuitMessage("§8[§c-§8] §7" + event.getPlayer().getName() + " §7left the game.");
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
