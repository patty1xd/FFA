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

        // Victim drops a tier
        plugin.getTierManager().onDeath(victim.getUniqueId());

        if (killer != null && killer != victim) {
            String killerTier = plugin.getTierManager().getTierDisplay(plugin.getTierManager().getTier(killer.getUniqueId()));
            plugin.getTierManager().addKill(killer.getUniqueId());

            // Custom death message
            event.setDeathMessage(
                killerTier + " §f" + killer.getName() + " §7slew " + victimTier + " §f" + victim.getName()
            );
        } else {
            event.setDeathMessage(victimTier + " §f" + victim.getName() + " §7died.");
        }
    }
}
