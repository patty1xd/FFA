package com.ffa.managers;

import com.ffa.FFAPlugin;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScoreboardManager {

    private final FFAPlugin plugin;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();

    public ScoreboardManager(FFAPlugin plugin) { this.plugin = plugin; }

    public void startUpdater() {
        int interval = plugin.getConfig().getInt("scoreboard-update-interval", 20);
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) updatePlayer(p);
        }, interval, interval);
    }

    public void updatePlayer(Player player) {
        if (player == null) return;
        Scoreboard board = boards.computeIfAbsent(player.getUniqueId(),
            k -> Bukkit.getScoreboardManager().getNewScoreboard());

        Objective obj = board.getObjective("ffa");
        if (obj != null) obj.unregister();
        obj = board.registerNewObjective("ffa", Criteria.DUMMY, "§5§l✦ KOALAFFA");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        int tier = plugin.getTierManager().getTier(player.getUniqueId());
        int kills = plugin.getTierManager().getTierKills(player.getUniqueId());
        int needed = plugin.getTierManager().getKillsNeeded(tier);
        String tierDisplay = plugin.getTierManager().getTierDisplay(tier);

        int line = 8;
        setLine(obj, "§8▬▬▬▬▬▬▬▬▬▬▬▬", line--);
        setLine(obj, "§7Tier: " + tierDisplay, line--);
        if (tier < TierManager.MAX_TIER) {
            setLine(obj, "§7Kill Progress: §e" + kills + "§7/§e" + needed, line--);
        } else {
            setLine(obj, "§4§l☠ MAX TIER ☠", line--);
        }
        setLine(obj, "§8▬▬▬▬▬▬▬▬▬▬▬▬ ", line--);
        setLine(obj, "§ftierstermc.ungsp.foo", line--);

        player.setScoreboard(board);

        // TAB header/footer
        String header = plugin.getConfig().getString("tab.header", "§5§lKOALAFFA")
            .replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()))
            .replace("&", "§");
        String footer = plugin.getConfig().getString("tab.footer", "§7Players: §e{online}")
            .replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()))
            .replace("&", "§");
        player.sendPlayerListHeaderAndFooter(
            LegacyComponentSerializer.legacySection().deserialize(header),
            LegacyComponentSerializer.legacySection().deserialize(footer)
        );

        updateNameTag(player);
    }

    public void updateNameTag(Player player) {
        if (player == null) return;
        int tier = plugin.getTierManager().getTier(player.getUniqueId());
        String tierDisplay = plugin.getTierManager().getTierDisplay(tier);
        int ping = player.getPing();
        String pingColor = ping < 50 ? "§a" : ping < 100 ? "§e" : ping < 200 ? "§6" : "§c";

        // Get LP prefix — empty string if none
        String lpPrefix = plugin.getChatManager().getLPPrefix(player);
        String prefixPart = lpPrefix.isEmpty() ? "" : lpPrefix + " ";

        // TAB format from config
        String tabFormat = plugin.getConfig().getString("tab.player-format", "{lp_prefix}{tier} §f{player} §8| {ping}ms")
            .replace("{lp_prefix}", prefixPart)
            .replace("{tier}", tierDisplay)
            .replace("{player}", player.getName())
            .replace("{ping}", pingColor + ping)
            .replace("&", "§");
        player.setPlayerListName(tabFormat);

        // Nametag above head
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Scoreboard board = viewer.getScoreboard();
            if (board == Bukkit.getScoreboardManager().getMainScoreboard()) continue;
            String teamKey = "ffa_" + player.getName().substring(0, Math.min(12, player.getName().length()));
            Team team = board.getTeam(teamKey);
            if (team == null) team = board.registerNewTeam(teamKey);
            team.setPrefix((prefixPart + tierDisplay + " ").replace("&", "§"));
            team.addEntry(player.getName());
        }
    }

    private void setLine(Objective obj, String text, int score) {
        obj.getScore(text).setScore(score);
    }
}
