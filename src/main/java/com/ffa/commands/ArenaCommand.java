package com.ffa.commands;

import com.ffa.FFAPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ArenaCommand implements CommandExecutor {

    private final FFAPlugin plugin;

    public ArenaCommand(FFAPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("§cPlayers only."); return true; }
        if (!p.hasPermission("ffa.admin")) { p.sendMessage("§cNo permission."); return true; }

        switch (label.toLowerCase()) {
            case "setarena1" -> {
                plugin.getRTPManager().setCorner1(p.getLocation());
                p.sendMessage("§aArena corner 1 set!");
            }
            case "setarena2" -> {
                plugin.getRTPManager().setCorner2(p.getLocation());
                p.sendMessage("§aArena corner 2 set!");
            }
            case "spawnarnanpc" -> {
                plugin.getRTPManager().spawnNPC(p.getLocation());
                p.sendMessage("§aArena NPC spawned!");
            }
            case "removearnanpc" -> {
                plugin.getRTPManager().removeNPC();
                p.sendMessage("§cArena NPC removed.");
            }
        }
        return true;
    }
}
