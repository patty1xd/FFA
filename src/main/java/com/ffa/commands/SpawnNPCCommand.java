package com.ffa.commands;
import com.ffa.FFAPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
public class SpawnNPCCommand implements CommandExecutor {
    private final FFAPlugin plugin;
    public SpawnNPCCommand(FFAPlugin plugin) { this.plugin = plugin; }
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("§cPlayers only."); return true; }
        if (!p.hasPermission("ffa.admin")) { p.sendMessage("§cNo permission."); return true; }
        plugin.getNPCManager().spawnNPC(p.getLocation());
        p.sendMessage("§aKit NPC spawned at your location!");
        return true;
    }
}
