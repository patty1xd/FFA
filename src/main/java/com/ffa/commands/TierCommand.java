package com.ffa.commands;
import com.ffa.FFAPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
public class TierCommand implements CommandExecutor {
    private final FFAPlugin plugin;
    public TierCommand(FFAPlugin plugin) { this.plugin = plugin; }
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("§cPlayers only."); return true; }
        int tier = plugin.getTierManager().getTier(p.getUniqueId());
        int kills = plugin.getTierManager().getTierKills(p.getUniqueId());
        int needed = plugin.getTierManager().getKillsNeeded(tier);
        p.sendMessage("§8[§6FFA§8] §7Tier: " + plugin.getTierManager().getTierDisplay(tier));
        if (tier < 5) p.sendMessage("§8[§6FFA§8] §7Progress: §e" + kills + "§7/§e" + needed + " kills");
        else p.sendMessage("§8[§6FFA§8] §6§lMAX TIER ★");
        return true;
    }
}
