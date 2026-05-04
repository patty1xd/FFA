package com.ffa.listeners;

import com.ffa.FFAPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class ArenaProtectListener implements Listener {

    private final FFAPlugin plugin;

    public ArenaProtectListener(FFAPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("ffa.admin")) return;

        // Only cancel if this specific block was part of the original saved arena
        if (!plugin.getArenaResetManager().isOriginalArenaBlock(event.getBlock().getLocation())) return;

        event.setCancelled(true);
        player.sendMessage("§cYou cannot break original arena blocks!");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("ffa.admin")) return;

        // Prevent placing blocks that would overwrite an original arena block
        if (!plugin.getArenaResetManager().isOriginalArenaBlock(event.getBlock().getLocation())) return;

        event.setCancelled(true);
        player.sendMessage("§cYou cannot place blocks over original arena blocks!");
    }
}
