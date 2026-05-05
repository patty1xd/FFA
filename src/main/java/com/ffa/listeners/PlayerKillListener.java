package com.ffa.listeners;

import com.ffa.FFAPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class PlayerKillListener implements Listener {

    private final FFAPlugin plugin;

    public PlayerKillListener(FFAPlugin plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        String victimTier = plugin.getTierManager().getTierDisplay(
            plugin.getTierManager().getTier(victim.getUniqueId()));

        plugin.getStatsManager().addDeath(victim.getUniqueId());

        if (killer != null && killer != victim) {
            String killerTier    = plugin.getTierManager().getTierDisplay(
                plugin.getTierManager().getTier(killer.getUniqueId()));
            int killerTierInt    = plugin.getTierManager().getTier(killer.getUniqueId());

            plugin.getTierManager().onDeath(victim.getUniqueId(), killerTierInt);
            plugin.getStatsManager().addKill(killer.getUniqueId());
            plugin.getTierManager().addKill(killer.getUniqueId(), victim.getUniqueId());

            // Build kill message — check for custom sword name
            String swordPart = getKillerSwordPart(killer);
            String msg;
            if (swordPart != null) {
                // Custom format: killer killed victim using <sword name>
                msg = plugin.getConfig().getString("messages.kill",
                    "§8[§c☠§8] {killer_tier} §f{killer} §7killed {victim_tier} §f{victim}")
                    .replace("{killer_tier}", killerTier)
                    .replace("{killer}",      killer.getName())
                    .replace("{victim_tier}", victimTier)
                    .replace("{victim}",      victim.getName())
                    + " §7using " + swordPart;
            } else {
                msg = plugin.getConfig().getString("messages.kill",
                    "§8[§c☠§8] {killer_tier} §f{killer} §7killed {victim_tier} §f{victim}")
                    .replace("{killer_tier}", killerTier)
                    .replace("{killer}",      killer.getName())
                    .replace("{victim_tier}", victimTier)
                    .replace("{victim}",      victim.getName());
            }
            event.setDeathMessage(msg);

            // Trigger random particle kill effect if killer has rank
            plugin.getKillEffectManager().playKillEffect(killer, victim);

        } else {
            plugin.getTierManager().onDeath(victim.getUniqueId());
            String msg = plugin.getConfig().getString("messages.death",
                "§8[§c☠§8] {victim_tier} §f{victim} §7died.")
                .replace("{victim_tier}", victimTier)
                .replace("{victim}",      victim.getName());
            event.setDeathMessage(msg);
        }
    }

    /**
     * Returns the colored custom sword name if the killer is holding a donor-named sword,
     * or null if they are not.
     */
    private String getKillerSwordPart(Player killer) {
        // Check main hand first
        ItemStack hand = killer.getInventory().getItemInMainHand();
        if (isDonorSword(hand)) return hand.getItemMeta().getDisplayName();

        // Check slot 0 as fallback
        ItemStack slot0 = killer.getInventory().getItem(0);
        if (isDonorSword(slot0)) return slot0.getItemMeta().getDisplayName();

        // Also check via RankManager in case they have a rank and a named sword
        String coloredName = plugin.getRankManager().getColoredSwordName(killer.getUniqueId());
        return coloredName; // will be null if not set
    }

    private boolean isDonorSword(ItemStack item) {
        if (item == null) return false;
        if (item.getType() != Material.DIAMOND_SWORD && item.getType() != Material.NETHERITE_SWORD) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasDisplayName()) return false;
        List<String> lore = meta.getLore();
        return lore != null && lore.contains("§8[Donor Sword]");
    }
}
