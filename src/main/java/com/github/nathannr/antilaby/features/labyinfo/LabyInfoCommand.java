package com.github.nathannr.antilaby.features.labyinfo;

import org.apache.logging.log4j.Level;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.github.nathannr.antilaby.api.LabyPlayer;
import com.github.nathannr.antilaby.main.AntiLaby;
import com.github.nathannr.antilaby.messagemanager.MessageManager;

import de.heisluft.antilaby.lang.impl.LanguageManager;
import de.heisluft.antilaby.util.Constants;

public class LabyInfoCommand implements CommandExecutor {
	
	private static boolean commandAvailable = true;
	
	public static void setCommandAvailability() {
		if (!Bukkit.getOnlinePlayers().isEmpty()) commandAvailable = false;
		else commandAvailable = true;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (commandAvailable) {
			if (args.length == 1) {
				final Player targetPlayer = Bukkit.getPlayer(args[0]);
				if (Bukkit.getOnlinePlayers().contains(targetPlayer)) {
					final LabyPlayer labyPlayer = new LabyPlayer(targetPlayer);
					if (sender instanceof Player) {
						final Player player = (Player) sender;
						if (player.hasPermission(Constants.PERMISSION_LABYINFO)) {
							if (labyPlayer.usesLabyMod()) player.sendMessage(LanguageManager.INSTANCE
									.translate("antilaby.command.labyInfo.labyMod", player, targetPlayer.getName()));
							else player.sendMessage(LanguageManager.INSTANCE
									.translate("antilaby.command.labyInfo.noLabyMod", player, targetPlayer.getName()));
						} else {
							player.sendMessage(MessageManager.getNoPermissionMessage(player));
							AntiLaby.LOG.log(Level.WARN, "Player " + player.getName() + " (" + player.getUniqueId()
									+ ") to use LabyInfo: Permission 'antilaby.labyinfo' is missing!");
						}
					} else if (labyPlayer.usesLabyMod()) sender.sendMessage("Player '" + args[0] + "' uses LabyMod.");
					else sender.sendMessage("Player '" + args[0] + "' doesn't use LabyMod.");
				} else if (sender instanceof Player) {
					final Player player = (Player) sender;
					player.sendMessage(LanguageManager.INSTANCE.translate("antilaby.command.labyInfo.playerOffline",
							player, args[0]));
				} else sender.sendMessage(Constants.CPREFIXERROR + "Player '" + args[0] + "' is offline!");
			} else if (sender instanceof Player) {
				final Player player = (Player) sender;
				player.sendMessage(LanguageManager.INSTANCE.translate("antilaby.command.labyInfo.usage", player));
			} else sender.sendMessage("Usage: /labyinfo <player>");
		} else if (sender instanceof Player) {
			final Player player = (Player) sender;
			if (player.hasPermission(Constants.PERMISSION_LABYINFO))
				player.sendMessage(LanguageManager.INSTANCE.translate("antilaby.command.labyInfo.reload", player));
			else player.sendMessage(MessageManager.getNoPermissionMessage(player));
		} else sender.sendMessage(Constants.CPREFIXINFO
				+ "Sorry, but LabyInfo is currently not available after a server reload. Please restart your server to use LabyInfo! Reload-support will be available in a future update.");
		return true;
	}
	
}
