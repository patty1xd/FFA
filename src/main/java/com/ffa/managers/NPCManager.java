package com.ffa.managers;

import com.ffa.FFAPlugin;
import org.bukkit.entity.Breeze;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.Location;

public class NPCManager {

    private final FFAPlugin plugin;
    private static final String NPC_NAME = "§6§lKit";

    public NPCManager(FFAPlugin plugin) { this.plugin = plugin; }

    public void spawnNPC(Location loc) {
        Breeze npc = (Breeze) loc.getWorld().spawnEntity(loc, EntityType.BREEZE);
        npc.setCustomName(NPC_NAME);
        npc.setCustomNameVisible(true);
        npc.setAI(false);
        npc.setInvulnerable(true);
        npc.setSilent(true);
    }

    public boolean isNPC(Entity entity) {
        return entity instanceof Breeze && NPC_NAME.equals(entity.getCustomName());
    }
}
