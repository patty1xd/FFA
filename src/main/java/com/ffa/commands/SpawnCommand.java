package com.ffa.commands;

import com.ffa.FFAPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpawnCommand implements CommandExecutor {

    private final FFAPlugin plugin;

    public SpawnCommand(FFAPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("§cPlayers only."); return true; }

        if (label.equalsIgnoreCase("setspawn")) {
            if (!p.hasPermission("ffa.admin")) { p.sendMessage("§cNo permission."); return true; }
            plugin.getSpawnManager().setSpawn(p.getLocation());
            p.sendMessage("§aSpawn set!");
            return true;
        }

        // /spawn
        plugin.getSpawnManager().handleSpawnCommand(p);
        return true;
    }
}
