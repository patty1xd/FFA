package com.ffa.commands;

import com.ffa.FFAPlugin;
import com.ffa.gui.TrimsGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /trims — opens the donor customisation GUI.
 * Only available to players with an active donor rank.
 */
public class TrimsCommand implements CommandExecutor {

    private final FFAPlugin plugin;
    private final TrimsGUI  gui;

    public TrimsCommand(FFAPlugin plugin, TrimsGUI gui) {
        this.plugin = plugin;
        this.gui    = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be run by a player.");
            return true;
        }
        gui.open(player);
        return true;
    }
}
