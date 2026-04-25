package com.ffa.listeners;

import com.ffa.FFAPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class NPCInteractListener implements Listener {

    private final FFAPlugin plugin;

    public NPCInteractListener(FFAPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();

        // Kit NPC
        if (plugin.getNPCManager().isNPC(event.getRightClicked())) {
            event.setCancelled(true);
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
