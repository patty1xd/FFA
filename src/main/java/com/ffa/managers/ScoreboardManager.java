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
        obj = board.registerNewObjective("ffa", Criteria.DUMMY, "§6§l⚔ FFA");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        int tier = plugin.getTierManager().getTier(player.getUniqueId());
        int kills = plugin.getTierManager().getTierKills(player.getUniqueId());
        int needed = plugin.getTierManager().getKillsNeeded(tier);
        String tierDisplay = plugin.getTierManager().getTierDisplay(tier);

        int line = 10;
        setLine(obj, "§7──────────────", line--);
        setLine(obj, "§7Tier: " + tierDisplay, line--);
        setLine(obj, "§7──────────────", line--);
        if (tier < TierManager.MAX_TIER) {
            setLine(obj, "§7Progress: §e" + kills + "§7/§e" + needed, line--);
        } else {
            setLine(obj, "§6§lMAX TIER ★", line--);
        }
        setLine(obj, "§7──────────────", line--);
        setLine(obj, "§7Right-click §eKit", line--);
        setLine(obj, "§7to get your kit!", line--);
        setLine(obj, "§7──────────────", line--);

        player.setScoreboard(board);
    }

    private void setLine(Objective obj, String text, int score) {
        obj.getScore(text).setScore(score);
    }
}
