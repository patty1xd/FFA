package com.ffa.commands;
import com.ffa.FFAPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
public class ResetPlayerCommand implements CommandExecutor {
    private final FFAPlugin plugin;
    public ResetPlayerCommand(FFAPlugin plugin) { this.plugin = plugin; }
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("ffa.admin")) { sender.sendMessage("§cNo permission."); return true; }
        if (args.length < 1) { sender.sendMessage("§cUsage: /resetplayer <player>"); return true; }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore()) { sender.sendMessage("§cPlayer not found."); return true; }
        plugin.getTierManager().resetPlayer(target.getUniqueId());
        sender.sendMessage("§aReset " + target.getName() + " to tier 1.");
        return true;
    }
}
