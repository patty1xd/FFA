package com.ffa.listeners;

import com.ffa.FFAPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class ArenaProtectListener implements Listener {

    private final FFAPlugin plugin;

    public ArenaProtectListener(FFAPlugin plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("ffa.admin")) return;
        if (!plugin.getArenaResetManager().isArenaBlock(event.getBlock().getLocation())) return;

        event.setCancelled(true);
        player.sendMessage("§cYou cannot break arena blocks!");
    }
}
