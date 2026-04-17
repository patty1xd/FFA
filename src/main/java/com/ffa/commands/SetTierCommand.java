package com.ffa.commands;
import com.ffa.FFAPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
public class SetTierCommand implements CommandExecutor {
    private final FFAPlugin plugin;
    public SetTierCommand(FFAPlugin plugin) { this.plugin = plugin; }
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("ffa.admin")) { sender.sendMessage("§cNo permission."); return true; }
        if (args.length < 2) { sender.sendMessage("§cUsage: /settier <player> <tier>"); return true; }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore()) { sender.sendMessage("§cPlayer not found."); return true; }
        int tier; try { tier = Integer.parseInt(args[1]); } catch (NumberFormatException e) { sender.sendMessage("§cInvalid tier."); return true; }
        plugin.getTierManager().setTier(target.getUniqueId(), tier);
        sender.sendMessage("§aSet " + target.getName() + " to tier " + tier);
        return true;
    }
}
