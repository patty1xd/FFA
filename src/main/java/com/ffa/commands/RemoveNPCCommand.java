package com.ffa.commands;
import com.ffa.FFAPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
public class RemoveNPCCommand implements CommandExecutor {
    private final FFAPlugin plugin;
    public RemoveNPCCommand(FFAPlugin plugin) { this.plugin = plugin; }
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("ffa.admin")) { sender.sendMessage("§cNo permission."); return true; }
        plugin.getNPCManager().removeNPC();
        sender.sendMessage("§aKit NPC removed.");
        return true;
    }
}
