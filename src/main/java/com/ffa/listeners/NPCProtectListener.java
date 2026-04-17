package com.ffa.listeners;

import com.ffa.FFAPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;

public class NPCProtectListener implements Listener {

    private final FFAPlugin plugin;

    public NPCProtectListener(FFAPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (plugin.getNPCManager().isNPC(event.getEntity())) event.setCancelled(true);
    }

    @EventHandler
    public void onTarget(EntityTargetEvent event) {
        if (plugin.getNPCManager().isNPC(event.getEntity())) event.setCancelled(true);
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (plugin.getNPCManager().isNPC(event.getEntity())) {
            event.getDrops().clear();
            // Respawn it
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () ->
                plugin.getNPCManager().restoreNPC(), 20L);
        }
    }
}
