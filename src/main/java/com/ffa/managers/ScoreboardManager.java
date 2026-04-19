package com.ffa.managers;

import com.ffa.FFAPlugin;
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
        int interval = plugin.getConfig().getInt("scoreboard.update-interval", 20);
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

        String title = plugin.getConfig().getString("scoreboard.title", "§5§l✦ TIERSTERMC");
        obj = board.registerNewObjective("ffa", Criteria.DUMMY, title);
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        int tier = plugin.getTierManager().getTier(player.getUniqueId());
        int kills = plugin.getTierManager().getTierKills(player.getUniqueId());
        int needed = plugin.getTierManager().getKillsNeeded(tier);
        String tierDisplay = plugin.getTierManager().getTierDisplay(tier);

        String divider = plugin.getConfig().getString("scoreboard.divider", "§8▬▬▬▬▬▬▬▬▬▬▬▬");
        String tierLine = plugin.getConfig().getString("scoreboard.tier-line", "§7Tier: {tier}")
            .replace("{tier}", tierDisplay);
        String ipLine = plugin.getConfig().getString("scoreboard.ip-line", "§ftierstermc.ungsp.foo");

        int line = 8;
        setLine(obj, divider, line--);
        setLine(obj, tierLine, line--);
        if (tier < TierManager.MAX_TIER) {
            String progressLine = plugin.getConfig().getString("scoreboard.progress-line", "§7Progress: §e{kills}§7/§e{needed}")
                .replace("{kills}", String.valueOf(kills))
                .replace("{needed}", String.valueOf(needed));
            setLine(obj, progressLine, line--);
        } else {
            String maxLine = plugin.getConfig().getString("scoreboard.max-tier-line", "§6§l★ MAX TIER ★");
            setLine(obj, maxLine, line--);
        }
        setLine(obj, divider + " ", line--);
        setLine(obj, ipLine, line--);

        player.setScoreboard(board);

        // TAB header/footer
        String header = plugin.getConfig().getString("tab.header", "§5§lTIERSTERMC")
            .replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()));
        String footer = plugin.getConfig().getString("tab.footer", "§7Players online: §e{online}")
            .replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()));
        player.sendPlayerListHeaderAndFooter(
            net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(header),
            net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(footer)
        );
    }

    public void updateNameTag(Player player) {
        if (player == null) return;
        int tier = plugin.getTierManager().getTier(player.getUniqueId());
        String tierDisplay = plugin.getTierManager().getTierDisplay(tier);

        for (Player p : Bukkit.getOnlinePlayers()) {
            Scoreboard board = p.getScoreboard();
            if (board == null) continue;
            String teamName = "ffa_" + player.getName().substring(0, Math.min(12, player.getName().length()));
            Team team = board.getTeam(teamName);
            if (team == null) team = board.registerNewTeam(teamName);
            team.setPrefix(tierDisplay + " ");
            team.addEntry(player.getName());
        }

        String tabFormat = plugin.getConfig().getString("tab.format", "{tier} §f{player}")
            .replace("{tier}", tierDisplay)
            .replace("{player}", player.getName());
        player.setPlayerListName(tabFormat);
    }

    private void setLine(Objective obj, String text, int score) {
        obj.getScore(text).setScore(score);
    }
}
