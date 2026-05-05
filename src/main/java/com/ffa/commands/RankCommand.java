package com.ffa.commands;

import com.ffa.FFAPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * /grantrank <player> <days>
 *   — Grants the donor rank for the given number of days.
 *   — Valid days: 7, 14, 30, 90
 *   — Can be run from console (Tebex) or by ops for testing.
 *
 * /revokerank <player>
 *   — Immediately revokes the donor rank.
 *   — Used as Tebex expiry command.
 */
public class RankCommand implements CommandExecutor {

    private final FFAPlugin plugin;

    public RankCommand(FFAPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // ── /grantrank ────────────────────────────────────────────────
        if (command.getName().equalsIgnoreCase("grantrank")) {
            if (!sender.hasPermission("ffa.admin") && !(sender instanceof org.bukkit.command.ConsoleCommandSender)) {
                sender.sendMessage("§cNo permission.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage("§cUsage: /grantrank <player> <days>");
                sender.sendMessage("§7Valid days: 7, 14, 30, 90");
                return true;
            }

            // Resolve player — supports offline players for Tebex
            Player target = Bukkit.getPlayer(args[0]);
            UUID targetUUID;
            String targetName;

            if (target != null) {
                targetUUID = target.getUniqueId();
                targetName = target.getName();
            } else {
                // Try offline player lookup
                @SuppressWarnings("deprecation")
                org.bukkit.OfflinePlayer offline = Bukkit.getOfflinePlayer(args[0]);
                if (!offline.hasPlayedBefore()) {
                    sender.sendMessage("§cPlayer §e" + args[0] + " §chas never joined this server.");
                    return true;
                }
                targetUUID = offline.getUniqueId();
                targetName = offline.getName() != null ? offline.getName() : args[0];
            }

            int days;
            try {
                days = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid days value: §e" + args[1]);
                return true;
            }

            if (!com.ffa.managers.RankManager.SAVES_FOR_DAYS.containsKey(days)) {
                sender.sendMessage("§cInvalid duration. Valid options: §e7, 14, 30, 90");
                return true;
            }

            plugin.getRankManager().grantRank(targetUUID, days);
            sender.sendMessage("§8[§6FFA§8] §aGranted §dDonor §7rank to §e" + targetName
                + " §7for §e" + days + " §7day(s).");

            if (target == null) {
                sender.sendMessage("§7(Player is offline — rank will apply when they next join.)");
            }
            return true;
        }

        // ── /revokerank ───────────────────────────────────────────────
        if (command.getName().equalsIgnoreCase("revokerank")) {
            if (!sender.hasPermission("ffa.admin") && !(sender instanceof org.bukkit.command.ConsoleCommandSender)) {
                sender.sendMessage("§cNo permission.");
                return true;
            }
            if (args.length < 1) {
                sender.sendMessage("§cUsage: /revokerank <player>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            UUID targetUUID;
            String targetName;

            if (target != null) {
                targetUUID = target.getUniqueId();
                targetName = target.getName();
            } else {
                @SuppressWarnings("deprecation")
                org.bukkit.OfflinePlayer offline = Bukkit.getOfflinePlayer(args[0]);
                if (!offline.hasPlayedBefore()) {
                    sender.sendMessage("§cPlayer §e" + args[0] + " §chas never joined this server.");
                    return true;
                }
                targetUUID = offline.getUniqueId();
                targetName = offline.getName() != null ? offline.getName() : args[0];
            }

            if (!plugin.getRankManager().hasRank(targetUUID)) {
                sender.sendMessage("§7" + targetName + " §cdoes not have an active rank.");
                return true;
            }

            plugin.getRankManager().revokeRank(targetUUID);
            sender.sendMessage("§8[§6FFA§8] §cRevoked §dDonor §7rank from §e" + targetName + "§7.");
            return true;
        }

        return false;
    }
}
