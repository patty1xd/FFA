package com.ffa.managers;

import com.ffa.FFAPlugin;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.*;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArenaResetManager {

    private final FFAPlugin plugin;
    private File schematicFile;
    private int resetTaskId = -1;
    private int cleanupTaskId = -1;
    private final Map<String, Material> originalBlocks = new HashMap<>();

    public ArenaResetManager(FFAPlugin plugin) {
        this.plugin = plugin;
        schematicFile = new File(plugin.getDataFolder(), "arena_backup.schem");
        plugin.getLogger().info("Arena schematic path: " + schematicFile.getAbsolutePath());
        plugin.getLogger().info("Arena schematic exists: " + schematicFile.exists());

        // Auto-reset is disabled by default — block decay handles per-block cleanup instead.
        // Set arena-reset.enabled: true in config.yml to re-enable full periodic resets.
        if (plugin.getConfig().getBoolean("arena-reset.enabled", false)) startResetTimer();
        if (plugin.getConfig().getBoolean("arena-cleanup.enabled", true)) startCleanupTimer();
    }

    // ── Save / Reset ─────────────────────────────────────────────────

    public boolean saveArena() {
        RandomTeleportManager rtp = plugin.getRTPManager();
        if (!rtp.hasArena()) {
            plugin.getLogger().warning("saveArena: Arena corners not set!");
            return false;
        }
        Location c1 = rtp.getCorner1();
        Location c2 = rtp.getCorner2();
        try {
            BlockVector3 min = BlockVector3.at(
                    Math.min(c1.getBlockX(), c2.getBlockX()),
                    Math.min(c1.getBlockY(), c2.getBlockY()),
                    Math.min(c1.getBlockZ(), c2.getBlockZ()));
            BlockVector3 max = BlockVector3.at(
                    Math.max(c1.getBlockX(), c2.getBlockX()),
                    Math.max(c1.getBlockY(), c2.getBlockY()),
                    Math.max(c1.getBlockZ(), c2.getBlockZ()));

            CuboidRegion region = new CuboidRegion(BukkitAdapter.adapt(c1.getWorld()), min, max);
            BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
            clipboard.setOrigin(min);

            try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(c1.getWorld()))) {
                ForwardExtentCopy copy = new ForwardExtentCopy(editSession, region, clipboard, region.getMinimumPoint());
                copy.setCopyingEntities(false);
                Operations.complete(copy);
            }

            schematicFile.getParentFile().mkdirs();
            ClipboardFormat format = ClipboardFormats.findByAlias("sponge.3");
            if (format == null) format = ClipboardFormats.findByAlias("sponge");
            if (format == null) {
                plugin.getLogger().warning("Could not find sponge schematic format!");
                return false;
            }
            try (ClipboardWriter writer = format.getWriter(new FileOutputStream(schematicFile))) {
                writer.write(clipboard);
            }
            plugin.getLogger().info("Arena saved successfully to: " + schematicFile.getAbsolutePath());
            snapshotOriginalBlocks(c1, c2);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("saveArena error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean resetArena() {
        if (!schematicFile.exists()) {
            plugin.getLogger().warning("resetArena: Schematic not found at " + schematicFile.getAbsolutePath());
            return false;
        }
        RandomTeleportManager rtp = plugin.getRTPManager();
        if (!rtp.hasArena()) {
            plugin.getLogger().warning("resetArena: Arena corners not set!");
            return false;
        }
        Location c1 = rtp.getCorner1();
        try {
            ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
            if (format == null) format = ClipboardFormats.findByAlias("sponge");
            if (format == null) {
                plugin.getLogger().warning("resetArena: Could not determine schematic format!");
                return false;
            }
            Clipboard clipboard;
            try (ClipboardReader reader = format.getReader(new FileInputStream(schematicFile))) {
                clipboard = reader.read();
            }
            try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(c1.getWorld()))) {
                ClipboardHolder holder = new ClipboardHolder(clipboard);
                com.sk89q.worldedit.function.operation.Operation operation = holder
                        .createPaste(editSession)
                        .to(clipboard.getOrigin())
                        .ignoreAirBlocks(false)
                        .build();
                Operations.complete(operation);
            }

            // Clear decay-tracked blocks so they don't re-remove freshly restored blocks
            if (plugin.getBlockDecayManager() != null) {
                plugin.getBlockDecayManager().clearAll();
            }
            clearItemsAndArrows();
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("resetArena error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ── Item / arrow cleanup ─────────────────────────────────────────

    public void clearItemsAndArrows() {
        RandomTeleportManager rtp = plugin.getRTPManager();
        if (!rtp.hasArena()) return;
        Location c1 = rtp.getCorner1();
        Location c2 = rtp.getCorner2();
        World world = c1.getWorld();
        double minX = Math.min(c1.getX(), c2.getX()), maxX = Math.max(c1.getX(), c2.getX());
        double minY = Math.min(c1.getY(), c2.getY()), maxY = Math.max(c1.getY(), c2.getY());
        double minZ = Math.min(c1.getZ(), c2.getZ()), maxZ = Math.max(c1.getZ(), c2.getZ());
        int count = 0;
        for (Entity entity : world.getEntities()) {
            Location loc = entity.getLocation();
            if (loc.getX() >= minX && loc.getX() <= maxX
                    && loc.getY() >= minY && loc.getY() <= maxY
                    && loc.getZ() >= minZ && loc.getZ() <= maxZ) {
                if (entity instanceof Item || entity instanceof Arrow) {
                    entity.remove();
                    count++;
                }
            }
        }
        if (count > 0) {
            String msg = plugin.getConfig().getString("arena-cleanup.message", "&7Arena items cleared.")
                    .replace("&", "§");
            Bukkit.broadcastMessage(msg);
        }
    }

    // ── Timers ───────────────────────────────────────────────────────

    public void startResetTimer() {
        if (resetTaskId != -1) Bukkit.getScheduler().cancelTask(resetTaskId);
        int intervalMinutes = plugin.getConfig().getInt("arena-reset.interval-minutes", 15);
        long intervalTicks = intervalMinutes * 60 * 20L;
        List<Integer> warnings = plugin.getConfig().getIntegerList("arena-reset.warning-seconds");

        resetTaskId = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            long ticksLeft = intervalTicks;
            @Override
            public void run() {
                ticksLeft -= 20;
                long secondsLeft = ticksLeft / 20;
                for (int w : warnings) {
                    if (secondsLeft == w) {
                        String msg = plugin.getConfig().getString("arena-reset.warning-message",
                                "&c⚠ Arena resetting in &f{time}&c!")
                                .replace("{time}", w + "s").replace("&", "§");
                        Bukkit.broadcastMessage(msg);
                    }
                }
                if (ticksLeft <= 0) {
                    ticksLeft = intervalTicks;
                    Bukkit.broadcastMessage(plugin.getConfig().getString("arena-reset.reset-message",
                            "&c⚠ Arena is resetting!").replace("&", "§"));
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        if (!schematicFile.exists()) {
                            Bukkit.getScheduler().runTask(plugin, () ->
                                    Bukkit.broadcastMessage("§cArena reset failed! Run /savearena first."));
                            return;
                        }
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            boolean success = resetArena();
                            if (success)
                                Bukkit.broadcastMessage(plugin.getConfig().getString(
                                        "arena-reset.done-message", "&aArena has been reset!").replace("&", "§"));
                            else
                                Bukkit.broadcastMessage("§cArena reset failed! Check console for details.");
                        });
                    });
                }
            }
        }, 20L, 20L).getTaskId();
    }

    public void startCleanupTimer() {
        if (cleanupTaskId != -1) Bukkit.getScheduler().cancelTask(cleanupTaskId);
        int intervalSeconds = plugin.getConfig().getInt("arena-cleanup.interval-seconds", 60);
        cleanupTaskId = Bukkit.getScheduler().runTaskTimer(plugin,
                this::clearItemsAndArrows, intervalSeconds * 20L, intervalSeconds * 20L).getTaskId();
    }

    public void stopResetTimer() {
        if (resetTaskId   != -1) { Bukkit.getScheduler().cancelTask(resetTaskId);   resetTaskId   = -1; }
        if (cleanupTaskId != -1) { Bukkit.getScheduler().cancelTask(cleanupTaskId); cleanupTaskId = -1; }
    }

    // ── Block queries ────────────────────────────────────────────────

    public boolean isArenaBlock(Location loc) {
        RandomTeleportManager rtp = plugin.getRTPManager();
        if (!rtp.hasArena()) return false;
        if (!schematicFile.exists()) return false;
        Location c1 = rtp.getCorner1();
        Location c2 = rtp.getCorner2();
        if (!loc.getWorld().equals(c1.getWorld())) return false;
        double minX = Math.min(c1.getX(), c2.getX()), maxX = Math.max(c1.getX(), c2.getX());
        double minY = Math.min(c1.getY(), c2.getY()), maxY = Math.max(c1.getY(), c2.getY());
        double minZ = Math.min(c1.getZ(), c2.getZ()), maxZ = Math.max(c1.getZ(), c2.getZ());
        return loc.getX() >= minX && loc.getX() <= maxX
                && loc.getY() >= minY && loc.getY() <= maxY
                && loc.getZ() >= minZ && loc.getZ() <= maxZ;
    }

    public boolean isOriginalArenaBlock(Location loc) {
        if (!schematicFile.exists()) return false;
        if (originalBlocks.isEmpty()) return false;
        String key = loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
        Material saved = originalBlocks.get(key);
        if (saved == null) return false;
        return loc.getBlock().getType() == saved;
    }

    private void snapshotOriginalBlocks(Location c1, Location c2) {
        originalBlocks.clear();
        World world = c1.getWorld();
        int minX = Math.min(c1.getBlockX(), c2.getBlockX());
        int maxX = Math.max(c1.getBlockX(), c2.getBlockX());
        int minY = Math.min(c1.getBlockY(), c2.getBlockY());
        int maxY = Math.max(c1.getBlockY(), c2.getBlockY());
        int minZ = Math.min(c1.getBlockZ(), c2.getBlockZ());
        int maxZ = Math.max(c1.getBlockZ(), c2.getBlockZ());
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Material mat = world.getBlockAt(x, y, z).getType();
                    if (mat != Material.AIR) {
                        originalBlocks.put(x + "," + y + "," + z, mat);
                    }
                }
            }
        }
    }
}
