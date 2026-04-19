package com.ffa.listeners;

import com.ffa.FFAPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;

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
        event.setJoinMessage(null);

        List<String> lines = plugin.getConfig().getStringList("messages.join-broadcast");
        for (String line : lines) {
            String formatted = line
                .replace("{player}", player.getName())
                .replace("{tier}", tierDisplay)
                .replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()));
            Bukkit.broadcastMessage(formatted);
        }

        String welcome = plugin.getConfig().getString("messages.welcome", "§7Welcome!");
        player.sendMessage(welcome);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getTierManager().saveAll();
        String msg = plugin.getConfig().getString("messages.quit-message", "§7{player} left.")
            .replace("{player}", event.getPlayer().getName());
        event.setQuitMessage(msg);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        event.setCancelled(true);
        var player = event.getPlayer();
        String tierDisplay = plugin.getTierManager().getTierDisplay(plugin.getTierManager().getTier(player.getUniqueId()));
        String format = plugin.getConfig().getString("chat.format", "{tier} §f{player} §7» §f{message}")
            .replace("{tier}", tierDisplay)
            .replace("{player}", player.getName())
            .replace("{message}", event.getMessage());
        player.getServer().broadcastMessage(format);
    }
}
