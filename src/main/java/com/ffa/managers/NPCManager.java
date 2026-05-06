package com.ffa.managers;

import com.ffa.FFAPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Breeze;
import org.bukkit.entity.EntityType;

public class NPCManager {

    private final FFAPlugin plugin;
    private Breeze npcEntity;

    public NPCManager(FFAPlugin plugin) {
        this.plugin = plugin;
    }

    public void spawnNPC(Location loc) {
        removeNPC();

        String npcName = plugin.getConfig().getString("npc.name", "§6§lKit");

        npcEntity = (Breeze) loc.getWorld().spawnEntity(loc, EntityType.BREEZE);
        npcEntity.setCustomName(npcName);
        npcEntity.setCustomNameVisible(true);
        npcEntity.setAI(false);
        npcEntity.setInvulnerable(true);
        npcEntity.setSilent(true);
        npcEntity.setPersistent(true);
        // Named mobs never despawn in vanilla — no extra logic needed
    }

    public void removeNPC() {
        if (npcEntity != null && !npcEntity.isDead()) {
            npcEntity.remove();
        }
        npcEntity = null;
    }

    public boolean isNPC(org.bukkit.entity.Entity entity) {
        return npcEntity != null && entity.getUniqueId().equals(npcEntity.getUniqueId());
    }

    public Breeze getNPCEntity() {
        return npcEntity;
    }
}
