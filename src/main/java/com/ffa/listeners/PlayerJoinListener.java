package com.ffa.listeners;

import com.ffa.FFAPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
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
        plugin.getStatsManager().initPlayer(player.getUniqueId(), player.getName());
        plugin.getBoardManager().updatePlayer(player);
        plugin.getBoardManager().updateNameTag(player);
        plugin.getNormalizationManager().normalizePlayer(player, false);
        String tierDisplay = plugin.getTierManager().getTierDisplay(plugin.getTierManager().getTier(player.getUniqueId()));
        event.setJoinMessage(null);
        List<String> lines = plugin.getConfig().getStringList("messages.join");
        for (String line : lines) {
            Bukkit.broadcastMessage(line
                .replace("{player}", player.getName())
                .replace("{tier}", tierDisplay)
                .replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("&", "§"));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getTierManager().saveAll();
        plugin.getStatsManager().saveAll();
        plugin.getChatManager().clearPlayer(event.getPlayer().getUniqueId());
        String tierDisplay = plugin.getTierManager().getTierDisplay(plugin.getTierManager().getTier(event.getPlayer().getUniqueId()));
        String msg = plugin.getConfig().getString("messages.quit", "§8[§c-§8] {tier} §f{player} §7left.")
            .replace("{player}", event.getPlayer().getName())
            .replace("{tier}", tierDisplay)
            .replace("&", "§");
        event.setQuitMessage(msg);
    }

    // NORMAL priority (default) — runs after LOW priority filter has had its chance to cancel
    @EventHandler(priority = EventPriority.NORMAL)
    public void onChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) return; // Respect filter cancellation
        event.setCancelled(true);
        String formatted = plugin.getChatManager().formatChat(event.getPlayer(), event.getMessage());
        event.getPlayer().getServer().broadcastMessage(formatted);
    }
}
