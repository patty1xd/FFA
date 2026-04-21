package com.ffa.commands;

import com.ffa.FFAPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MsgCommand implements CommandExecutor {
    private final FFAPlugin plugin;
    public MsgCommand(FFAPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("§cPlayers only."); return true; }
        if (args.length < 2) { p.sendMessage(plugin.getConfig().getString("messages.msg-usage", "§cUsage: /msg <player> <message>")); return true; }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) { p.sendMessage(plugin.getConfig().getString("messages.player-offline", "§cPlayer offline.")); return true; }
        String message = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        plugin.getChatManager().sendPrivateMessage(p, target, message);
        return true;
    }
}
