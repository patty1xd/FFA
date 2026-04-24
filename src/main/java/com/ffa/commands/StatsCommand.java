package com.ffa.commands;

import com.ffa.FFAPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StatsCommand implements CommandExecutor {

    private final FFAPlugin plugin;

    public StatsCommand(FFAPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (label.equalsIgnoreCase("hologram")) {
            handleHologram(sender, args);
            return true;
        }

        // /stats or /stats <player>
        if (args.length == 0) {
            if (!(sender instanceof Player p)) { sender.sendMessage("§cPlayers only or specify a player."); return true; }
            showStats(sender, p.getUniqueId(), p.getName());
        } else {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            if (!target.hasPlayedBefore()) { sender.sendMessage("§cPlayer not found."); return true; }
            showStats(sender, target.getUniqueId(), target.getName());
        }
        return true;
    }

    private void showStats(CommandSender sender, java.util.UUID uuid, String name) {
        var stats = plugin.getStatsManager();
        sender.sendMessage("§8§m                        ");
        sender.sendMessage("§6§l⚔ Stats: §f" + name);
        sender.sendMessage("§7Kills: §e" + stats.getKills(uuid));
        sender.sendMessage("§7Deaths: §c" + stats.getDeaths(uuid));
        sender.sendMessage("§7K/D: §a" + stats.getKD(uuid));
        sender.sendMessage("§7Current Streak: §6" + stats.getCurrentStreak(uuid));
        sender.sendMessage("§7Best Streak: §6" + stats.getBestStreak(uuid));
        sender.sendMessage("§8§m                        ");
    }

    private void handleHologram(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ffa.admin")) { sender.sendMessage("§cNo permission."); return; }
        if (!(sender instanceof Player p)) { sender.sendMessage("§cPlayers only."); return; }
        if (args.length < 1) {
            sender.sendMessage("§cUsage: §f/hologram <kills|deaths|kd|remove <type>>");
            return;
        }

        switch (args[0].toLowerCase()) {
            case "kills", "deaths", "kd" -> {
                plugin.getHologramManager().spawnHologram(args[0].toLowerCase(), p.getLocation());
                sender.sendMessage("§a" + args[0] + " hologram placed!");
            }
            case "remove" -> {
                if (args.length < 2) { sender.sendMessage("§cUsage: /hologram remove <kills|deaths|kd>"); return; }
                plugin.getHologramManager().removeHologram(args[1].toLowerCase());
                sender.sendMessage("§cHologram removed.");
            }
            default -> sender.sendMessage("§cUsage: §f/hologram <kills|deaths|kd|remove <type>>");
        }
    }
}
