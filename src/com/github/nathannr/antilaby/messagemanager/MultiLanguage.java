package com.github.nathannr.antilaby.messagemanager;

import java.io.File;
import java.io.IOException;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class MultiLanguage {

	private boolean languageLoaded = false;
	private JavaPlugin plugin;
	private String cprefix;
	private String fallbackFile = "en_us";
	private int resource;

	public MultiLanguage(JavaPlugin plugin, String cprefix) {
		this.plugin = plugin;
		this.cprefix = cprefix;

		this.initLanguage();
	}

	public MultiLanguage(JavaPlugin plugin, String cprefix, String fallbackFile) {
		this.plugin = plugin;
		this.cprefix = cprefix;
		this.fallbackFile = fallbackFile;

		this.initLanguage();
	}

	public MultiLanguage(JavaPlugin plugin, String cprefix, int resource) {
		this.plugin = plugin;
		this.cprefix = cprefix;
		this.resource = resource;

		this.initLanguage();
	}

	public MultiLanguage(JavaPlugin plugin, String cprefix, int resource, String fallbackFile) {
		this.plugin = plugin;
		this.cprefix = cprefix;
		this.resource = resource;
		this.fallbackFile = fallbackFile;

		this.initLanguage();
	}

	public void initLanguage() {
		// Create lang files in different languages
		String en_US = "en_us";
		File en_USfile = new File(this.plugin.getDataFolder() + "/language/" + en_US + ".yml");
		FileConfiguration en_UScfg = YamlConfiguration.loadConfiguration(en_USfile);
		en_UScfg.options().header("Language file of " + plugin.getName()
				+ " by NathanNr, https://www.spigotmc.org/resources/" + this.resource + "/");

		en_UScfg.addDefault("NoPermission", "&cYou do not have permission to use this command&r");
		en_UScfg.addDefault("LabyModPlayerKick", "&cYou are not allowed to use LabyMod!&r");

		en_UScfg.options().copyDefaults(true);

		String de_DE = "de_de";
		File de_DEfile = new File(this.plugin.getDataFolder() + "/language/" + de_DE + ".yml");
		FileConfiguration de_DEcfg = YamlConfiguration.loadConfiguration(de_DEfile);
		de_DEcfg.options().header("Language file of " + plugin.getName()
				+ " by NathanNr, https://www.spigotmc.org/resources/" + this.resource + "/");

		de_DEcfg.addDefault("NoPermission", "&cDu hast nicht die ben�tigte Berechtigung, diesen Befehl auszuf�hren&r");
		de_DEcfg.addDefault("LabyModPlayerKick", "&cDu darfst nicht mit LabyMod spielen!&r");

		de_DEcfg.options().copyDefaults(true);

		String fr_FR = "fr_fr";
		File fr_FRfile = new File(this.plugin.getDataFolder() + "/language/" + fr_FR + ".yml");
		FileConfiguration fr_FRcfg = YamlConfiguration.loadConfiguration(fr_FRfile);
		fr_FRcfg.options().header("Language file of " + plugin.getName()
				+ " by NathanNr, https://www.spigotmc.org/resources/" + this.resource + "/");

		fr_FRcfg.addDefault("NoPermission", "&cVous n'avez pas la permission d'utiliser cette commande&r");
		fr_FRcfg.addDefault("LabyModPlayerKick", "&cVous ne pouvez pas jouer avec LabyMod!&r");

		fr_FRcfg.options().copyDefaults(true);

		try {
			// Save language files
			en_UScfg.save(en_USfile);
			de_DEcfg.save(de_DEfile);
			fr_FRcfg.save(fr_FRfile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.languageLoaded = true;
	}

	public String getMultiLanguageMessage(Player player, String path, boolean translateAlternateColorCodes) {
		// Get a message in player's language
		if (this.languageLoaded == false) {
			this.initLanguage();
		}
		File file = new File(this.plugin.getDataFolder() + "/language/" + player.spigot().getLocale().toLowerCase() + ".yml");
		File fallbackFile = new File(this.plugin.getDataFolder() + "/language/" + this.fallbackFile + ".yml");
		FileConfiguration fallbackCfg = YamlConfiguration.loadConfiguration(fallbackFile);
		if (path.isEmpty() || path == null) {
			throw new MultiLanguageException("Plugin tried to send a MultiLanguageMessage with an empty or null path.");
		}
		if (file.exists()) {
			FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
			if (cfg.getString(path) != null) {
				if (translateAlternateColorCodes == true) {
					return ChatColor.translateAlternateColorCodes('&', cfg.getString(path));
				} else {
					return cfg.getString(path);
				}
			} else {
				if (fallbackCfg.getString(path) == null) {
					this.initLanguage();
				}
				if (fallbackCfg.getString(path) != null) {
					if (translateAlternateColorCodes == true) {
						return ChatColor.translateAlternateColorCodes('&', fallbackCfg.getString(path));
					} else {
						return fallbackCfg.getString(path);
					}
				} else {
					throw new MultiLanguageException(this.cprefix + "MultiLanguageMessage error: Path '" + path
							+ "' does not exists in the fallback language file.");
				}
			}
		} else {
			if (fallbackCfg.getString(path) == null) {
				this.initLanguage();
			}
			if (fallbackCfg.getString(path) != null) {
				if (translateAlternateColorCodes == true) {
					return ChatColor.translateAlternateColorCodes('&', fallbackCfg.getString(path));
				} else {
					return fallbackCfg.getString(path);
				}
			} else {
				throw new MultiLanguageException(this.cprefix + "MultiLanguageMessage error: Path '" + path
						+ "' does not exists in the fallback language file.");
			}
		}
	}

}
