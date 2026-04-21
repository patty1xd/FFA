package com.ffa.commands;

import com.ffa.FFAPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReplyCommand implements CommandExecutor {
    private final FFAPlugin plugin;
    public ReplyCommand(FFAPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("§cPlayers only."); return true; }
        if (args.length < 1) { p.sendMessage("§cUsage: /reply <message>"); return true; }
        Player target = plugin.getChatManager().getLastTarget(p.getUniqueId());
        if (target == null) { p.sendMessage(plugin.getConfig().getString("messages.no-reply-target", "§cNo one to reply to.")); return true; }
        String message = String.join(" ", args);
        plugin.getChatManager().sendPrivateMessage(p, target, message);
        return true;
    }
}
