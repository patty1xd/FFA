package com.ffa.listeners;

import com.ffa.FFAPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerKillListener implements Listener {

    private final FFAPlugin plugin;

    public PlayerKillListener(FFAPlugin plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        String victimTier = plugin.getTierManager().getTierDisplay(plugin.getTierManager().getTier(victim.getUniqueId()));
        plugin.getTierManager().onDeath(victim.getUniqueId());

        if (killer != null && killer != victim) {
            String killerTier = plugin.getTierManager().getTierDisplay(plugin.getTierManager().getTier(killer.getUniqueId()));
            plugin.getTierManager().addKill(killer.getUniqueId());
            String msg = plugin.getConfig().getString("messages.kill-message", "{killer_tier} §f{killer} §7slew {victim_tier} §f{victim}")
                .replace("{killer_tier}", killerTier)
                .replace("{killer}", killer.getName())
                .replace("{victim_tier}", victimTier)
                .replace("{victim}", victim.getName());
            event.setDeathMessage(msg);
        } else {
            String msg = plugin.getConfig().getString("messages.death-message", "{victim_tier} §f{victim} §7died.")
                .replace("{victim_tier}", victimTier)
                .replace("{victim}", victim.getName());
            event.setDeathMessage(msg);
        }
    }
}
