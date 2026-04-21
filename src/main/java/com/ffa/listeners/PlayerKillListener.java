package com.ffa.listeners;

import com.ffa.FFAPlugin;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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
            plugin.getTierManager().addKill(killer.getUniqueId(), victim.getUniqueId());

            String msg = plugin.getConfig().getString("messages.kill",
                "§8[§c☠§8] {killer_tier} §f{killer} §7killed {victim_tier} §f{victim}")
                .replace("{killer_tier}", killerTier)
                .replace("{killer}", killer.getName())
                .replace("{victim_tier}", victimTier)
                .replace("{victim}", victim.getName());
            event.setDeathMessage(msg);
        } else {
            String msg = plugin.getConfig().getString("messages.death",
                "§8[§c☠§8] {victim_tier} §f{victim} §7died.")
                .replace("{victim_tier}", victimTier)
                .replace("{victim}", victim.getName());
            event.setDeathMessage(msg);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;
        plugin.getGearEffectManager().applyOffensiveEffects(attacker, victim);
        double newDamage = plugin.getGearEffectManager().applyDefensiveEffects(victim, attacker, event.getDamage());
        if (newDamage <= 0) event.setCancelled(true);
        else event.setDamage(newDamage);
        double hp = attacker.getHealth(), maxHp = attacker.getAttribute(Attribute.MAX_HEALTH).getValue();
        if (hp / maxHp < 0.5 && plugin.getGearEffectManager().hasArmorPiece(attacker, "§c§lSkull of the Frenzied"))
            event.setDamage(event.getDamage() + 2.0);
    }
}
