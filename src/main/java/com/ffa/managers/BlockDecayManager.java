package com.ffa.managers;

import com.ffa.FFAPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tracks every block a player places inside the arena and removes it after
 * a configurable delay (block-decay-seconds in config.yml).
 * Water/lava placed by bucket is also tracked and removed correctly —
 * removing the source block lets Minecraft's fluid physics retract the flow.
 */
public class BlockDecayManager implements Listener {

    private final FFAPlugin plugin;
    // Insertion-ordered map keeps the oldest entries at the front for fast iteration
    private final Map<Location, Long> trackedBlocks = new LinkedHashMap<>();
    private final long decayMillis;
    private BukkitTask decayTask;

    public BlockDecayManager(FFAPlugin plugin) {
        this.plugin = plugin;
        int seconds = plugin.getConfig().getInt("block-decay-seconds", 60);
        this.decayMillis = seconds * 1000L;
        startDecayTask();
    }

    // ── Event listeners ──────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Location loc = event.getBlock().getLocation();
        if (!isInsideArena(loc)) return;
        trackedBlocks.put(loc, System.currentTimeMillis());
    }

    // Bucket-placed water / lava is placed 1 tick after the event fires
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Location loc = event.getBlock().getLocation();
        if (!isInsideArena(loc)) return;
        Bukkit.getScheduler().runTaskLater(plugin,
                () -> trackedBlocks.put(loc, System.currentTimeMillis()), 1L);
    }

    // When a player breaks a tracked block before it decays, remove from tracking
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        trackedBlocks.remove(event.getBlock().getLocation());
    }

    // ── Public API ───────────────────────────────────────────────────

    public boolean isTracked(Location loc) {
        return trackedBlocks.containsKey(loc);
    }

    public void removeTracked(Location loc) {
        trackedBlocks.remove(loc);
    }

    /** Called by ArenaResetManager after a manual /resetarena so stale entries don't resurface. */
    public void clearAll() {
        trackedBlocks.clear();
    }

    public void shutdown() {
        if (decayTask != null) {
            decayTask.cancel();
            decayTask = null;
        }
    }

    // ── Internal ─────────────────────────────────────────────────────

    private void startDecayTask() {
        // Check every 10 ticks (0.5 s) — fine-grained enough without being heavy
        decayTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 10L);
    }

    private void tick() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Location, Long>> it = trackedBlocks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Location, Long> entry = it.next();
            if (now - entry.getValue() < decayMillis) break; // LinkedHashMap: oldest first, stop early

            Location loc = entry.getKey();

            // If the arena was manually reset, this location is already the original block — skip setting AIR
            ArenaResetManager arm = plugin.getArenaResetManager();
            if (arm != null && arm.isOriginalArenaBlock(loc)) {
                it.remove();
                continue;
            }

            // applyPhysics=true so fluids retract naturally when the source block is removed
            loc.getBlock().setType(Material.AIR, true);
            it.remove();
        }
    }

    private boolean isInsideArena(Location loc) {
        ArenaResetManager arm = plugin.getArenaResetManager();
        return arm != null && arm.isArenaBlock(loc);
    }
}
