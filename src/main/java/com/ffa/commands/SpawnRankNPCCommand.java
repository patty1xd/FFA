package com.ffa.commands;

import com.ffa.FFAPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /spawnranknpc — spawns the RANKS witch NPC at the player's location.
 * /removeranknpc — removes it.
 */
public class SpawnRankNPCCommand implements CommandExecutor {

    private final FFAPlugin plugin;

    public SpawnRankNPCCommand(FFAPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ffa.admin")) {
            sender.sendMessage("§cNo permission.");
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cMust be run by a player.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("spawnranknpc")) {
            plugin.getRankNPCManager().spawnNPC(player.getLocation());
            sender.sendMessage("§8[§6FFA§8] §aRANKS NPC spawned at your location.");
        } else if (command.getName().equalsIgnoreCase("removeranknpc")) {
            plugin.getRankNPCManager().removeNPC();
            sender.sendMessage("§8[§6FFA§8] §cRANKS NPC removed.");
        }
        return true;
    }
}
