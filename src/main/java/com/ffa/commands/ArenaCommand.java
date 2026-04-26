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
        if (!sender.hasPermission("ffa.admin")) { sender.sendMessage("§cNo permission."); return true; }

        switch (label.toLowerCase()) {
            case "setarena1" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("§cPlayers only."); return true; }
                plugin.getRTPManager().setCorner1(p.getLocation());
                sender.sendMessage("§aArena corner 1 set!");
            }
            case "setarena2" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("§cPlayers only."); return true; }
                plugin.getRTPManager().setCorner2(p.getLocation());
                sender.sendMessage("§aArena corner 2 set!");
            }
            case "spawnarnanpc" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("§cPlayers only."); return true; }
                plugin.getRTPManager().spawnNPC(p.getLocation());
                sender.sendMessage("§aArena NPC spawned!");
            }
            case "removearnanpc" -> {
                plugin.getRTPManager().removeNPC();
                sender.sendMessage("§cArena NPC removed.");
            }
            case "savearena" -> {
                sender.sendMessage("§eSaving arena...");
                boolean success = plugin.getArenaResetManager().saveArena();
                if (success) sender.sendMessage("§aArena saved! Auto-reset will use this state.");
                else sender.sendMessage("§cFailed! Make sure arena corners are set with /setarena1 and /setarena2.");
            }
            case "resetarena" -> {
                sender.sendMessage("§eResetting arena...");
                org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    boolean success = plugin.getArenaResetManager().resetArena();
                    org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                        if (success) sender.sendMessage("§aArena reset complete!");
                        else sender.sendMessage("§cFailed! Run /savearena first.");
                    });
                });
            }
        }
        return true;
    }
}
