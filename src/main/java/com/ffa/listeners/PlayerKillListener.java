package com.ffa.listeners;

import com.ffa.FFAPlugin;
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

        // Stats tracking
        plugin.getStatsManager().addDeath(victim.getUniqueId());

        if (killer != null && killer != victim) {
            String killerTier = plugin.getTierManager().getTierDisplay(plugin.getTierManager().getTier(killer.getUniqueId()));
            int killerTierInt = plugin.getTierManager().getTier(killer.getUniqueId());

            plugin.getTierManager().onDeath(victim.getUniqueId(), killerTierInt);
            plugin.getStatsManager().addKill(killer.getUniqueId());
            plugin.getTierManager().addKill(killer.getUniqueId(), victim.getUniqueId());
            plugin.getHologramManager().updateAll();

            String msg = plugin.getConfig().getString("messages.kill",
                "§8[§c☠§8] {killer_tier} §f{killer} §7killed {victim_tier} §f{victim}")
                .replace("{killer_tier}", killerTier)
                .replace("{killer}", killer.getName())
                .replace("{victim_tier}", victimTier)
                .replace("{victim}", victim.getName());
            event.setDeathMessage(msg);
        } else {
            plugin.getTierManager().onDeath(victim.getUniqueId());
            String msg = plugin.getConfig().getString("messages.death",
                "§8[§c☠§8] {victim_tier} §f{victim} §7died.")
                .replace("{victim_tier}", victimTier)
                .replace("{victim}", victim.getName());
            event.setDeathMessage(msg);
        }
    }
}
