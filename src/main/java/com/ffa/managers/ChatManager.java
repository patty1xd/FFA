package com.ffa.managers;

import com.ffa.FFAPlugin;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChatManager {

    private final FFAPlugin plugin;
    private final Map<UUID, UUID> lastMessageTarget = new HashMap<>();
    private final boolean hasPAPI;

    public ChatManager(FFAPlugin plugin) {
        this.plugin = plugin;
        this.hasPAPI = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    public String getLPPrefix(Player player) {
        if (!hasPAPI) return "";
        try {
            String prefix = PlaceholderAPI.setPlaceholders(player, "%luckperms_prefix%");
            // If null, empty, or literally "null" return empty string
            if (prefix == null || prefix.isEmpty() || prefix.equalsIgnoreCase("null")) return "";
            return prefix;
        } catch (Exception e) {
            return "";
        }
    }

    public String applyPlaceholders(Player player, String text) {
        if (!hasPAPI) return text.replace("&", "§");
        return PlaceholderAPI.setPlaceholders(player, text).replace("&", "§");
    }

    public void sendPrivateMessage(Player sender, Player target, String message) {
        String format = plugin.getConfig().getString("chat.msg-format",
            "§8[§dPM§8] §f{sender} §7→ §f{receiver}§7: §f{message}")
            .replace("{sender}", sender.getName())
            .replace("{receiver}", target.getName())
            .replace("{message}", message)
            .replace("&", "§");
        sender.sendMessage(format);
        target.sendMessage(format);
        lastMessageTarget.put(sender.getUniqueId(), target.getUniqueId());
        lastMessageTarget.put(target.getUniqueId(), sender.getUniqueId());
    }

    public Player getLastTarget(UUID uuid) {
        UUID targetUUID = lastMessageTarget.get(uuid);
        if (targetUUID == null) return null;
        return Bukkit.getPlayer(targetUUID);
    }

    public void clearPlayer(UUID uuid) { lastMessageTarget.remove(uuid); }

    public String formatChat(Player player, String message) {
        String tierDisplay = plugin.getTierManager().getTierDisplay(plugin.getTierManager().getTier(player.getUniqueId()));
        String lpPrefix = getLPPrefix(player);
        String prefixPart = lpPrefix.isEmpty() ? "" : lpPrefix + " ";

        return plugin.getConfig().getString("chat.format", "{lp_prefix}{tier} §f{player} §7» §f{message}")
            .replace("{lp_prefix}", prefixPart)
            .replace("{tier}", tierDisplay)
            .replace("{player}", player.getName())
            .replace("{message}", message)
            .replace("&", "§");
    }
}
