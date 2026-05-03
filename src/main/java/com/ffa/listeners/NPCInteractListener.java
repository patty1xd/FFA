package com.ffa.listeners;

import com.ffa.FFAPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NPCInteractListener implements Listener {

    private final FFAPlugin plugin;
    private final Map<UUID, Long> kitCooldowns = new HashMap<>();
    private static final int KIT_COOLDOWN_SECONDS = 7;

    public NPCInteractListener(FFAPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();

        // Kit NPC
        if (plugin.getNPCManager().isNPC(event.getRightClicked())) {
            event.setCancelled(true);

            // Check cooldown
            long now = System.currentTimeMillis();
            Long last = kitCooldowns.get(player.getUniqueId());
            if (last != null && now - last < KIT_COOLDOWN_SECONDS * 1000L) {
                long remaining = KIT_COOLDOWN_SECONDS - (now - last) / 1000;
                player.sendMessage("§cKit is on cooldown for §e" + remaining + "s§c!");
                return;
            }
            kitCooldowns.put(player.getUniqueId(), now);
            plugin.getKitManager().giveKit(player);
            return;
        }

        // Arena RTP NPC
        if (plugin.getRTPManager().isNPC(event.getRightClicked())) {
            event.setCancelled(true);
            plugin.getRTPManager().teleportRandom(player);
        }
    }
}
