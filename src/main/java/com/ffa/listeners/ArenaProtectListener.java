package com.ffa.listeners;

import com.ffa.FFAPlugin;
import com.ffa.managers.BlockDecayManager;
import org.bukkit.Location;
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

        Location loc = event.getBlock().getLocation();

        // Player-placed blocks (tracked by BlockDecayManager) can always be broken early
        BlockDecayManager bdm = plugin.getBlockDecayManager();
        if (bdm != null && bdm.isTracked(loc)) {
            bdm.removeTracked(loc);
            return;
        }

        // Original arena blocks are protected
        if (plugin.getArenaResetManager().isOriginalArenaBlock(loc)) {
            event.setCancelled(true);
            player.sendMessage("§cYou cannot break original arena blocks!");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("ffa.admin")) return;

        // Prevent placing blocks directly over an original arena block position
        if (plugin.getArenaResetManager().isOriginalArenaBlock(event.getBlock().getLocation())) {
            event.setCancelled(true);
            player.sendMessage("§cYou cannot place blocks over original arena blocks!");
        }
        // Non-original positions are allowed — BlockDecayManager will handle their timed removal
    }
}
