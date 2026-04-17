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

        // Victim drops a tier
        plugin.getTierManager().onDeath(victim.getUniqueId());

        // Killer gains a kill
        if (killer != null && killer != victim) {
            plugin.getTierManager().addKill(killer.getUniqueId());
            String tierDisplay = plugin.getTierManager().getTierDisplay(plugin.getTierManager().getTier(killer.getUniqueId()));
            killer.sendMessage("§8[§6FFA§8] §eYou killed §f" + victim.getName() + "§e! " + tierDisplay);
        }
    }
}
