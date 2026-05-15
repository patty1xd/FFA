package com.ffa.listeners;

import com.ffa.FFAPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTeleportEvent;

public class NPCProtectListener implements Listener {

    private final FFAPlugin plugin;

    public NPCProtectListener(FFAPlugin plugin) { this.plugin = plugin; }

    // ── Damage ───────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDamage(EntityDamageEvent event) {
        if (isAnyNPC(event.getEntity())) event.setCancelled(true);
    }

    // ── Targeting ────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onTarget(EntityTargetEvent event) {
        if (isAnyNPC(event.getEntity())) event.setCancelled(true);
    }

    // ── Teleportation / portals ───────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onTeleport(EntityTeleportEvent event) {
        if (isAnyNPC(event.getEntity())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPortal(EntityPortalEvent event) {
        if (isAnyNPC(event.getEntity())) event.setCancelled(true);
    }

    // ── Death (should never happen — invulnerable — but safety net) ──

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (plugin.getNPCManager().isNPC(event.getEntity())) {
            event.getDrops().clear();
            event.setDroppedExp(0);
            plugin.getNPCManager().onEntityDeath();
            return;
        }
        if (plugin.getRTPManager().isNPC(event.getEntity())) {
            event.getDrops().clear();
            event.setDroppedExp(0);
            plugin.getRTPManager().onEntityDeath();
            return;
        }
        if (plugin.getRankNPCManager().isNPC(event.getEntity())) {
            event.getDrops().clear();
            event.setDroppedExp(0);
            plugin.getRankNPCManager().onEntityDeath();
        }
    }

    // ── Helper ───────────────────────────────────────────────────────

    private boolean isAnyNPC(org.bukkit.entity.Entity entity) {
        return plugin.getNPCManager().isNPC(entity)
                || plugin.getRTPManager().isNPC(entity)
                || plugin.getRankNPCManager().isNPC(entity);
    }
}
