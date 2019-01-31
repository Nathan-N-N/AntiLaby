package com.github.antilaby.antilaby.main;

import com.github.antilaby.antilaby.api.antilabypackages.AntiLabyPackager;
import com.github.antilaby.antilaby.command.AntiLabyCommand;
import com.github.antilaby.antilaby.command.LabyInfoCommand;
import com.github.antilaby.antilaby.compat.PluginFeature;
import com.github.antilaby.antilaby.compat.ProtocolLibSupport;
import com.github.antilaby.antilaby.config.ConfigFile;
import com.github.antilaby.antilaby.config.ConfigReader;
import com.github.antilaby.antilaby.events.EventsPost18;
import com.github.antilaby.antilaby.events.PlayerJoin;
import com.github.antilaby.antilaby.lang.LanguageManager;
import com.github.antilaby.antilaby.log.Logger;
import com.github.antilaby.antilaby.metrics.BStatsHandler;
import com.github.antilaby.antilaby.metrics.Metrics;
import com.github.antilaby.antilaby.pluginchannel.IncomingPluginChannel;
import com.github.antilaby.antilaby.updater.UpdateManager;
import com.github.antilaby.antilaby.util.Constants;
import com.github.antilaby.antilaby.util.DataManager;
import com.github.antilaby.antilaby.util.FeatureProvider;
import com.github.antilaby.antilaby.util.ServerHelper;
import com.github.zafarkhaja.semver.Version;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main class of AntiLaby Spigot plugin
 *
 * @author NathanNr
 */
public class AntiLaby extends JavaPlugin {

  /**
   * The main logger
   */
  public static final Logger LOG = new Logger("Main");
  // The singleton instance
  private static AntiLaby instance;
  /**
   * The AntiLaby version as SemVer.
   */
  private static Version version;
  /**
   * All loaded Features
   */
  private final Set<PluginFeature> loadedFeatures = new HashSet<>(PluginFeature.values().length);
  // The cleanup Thread deletes the saved LabyPlayer data
  private final Thread cleanup = new Thread(DataManager::cleanup, "AntiLabyCleanup");
  // Read from the configuration file
  private ConfigReader configReader = new ConfigReader();
  // Compatible?
  private boolean compatible;
  // is this pre 1.9?
  private boolean before19 = false;

  /**
   * Get the Version of AL.
   *
   * @return the version as SemVer
   */
  public static Version getVersion() {
    return version;
  }

  /**
   * Gets the singleton instance
   *
   * @return the singleton instance
   */
  public static AntiLaby getInstance() {
    return instance;
  }

  /**
   * This should be used instead of accessing {@link #loadedFeatures} directly
   *
   * @return an unmodifiable set of all enabled plugin features
   */
  public Set<PluginFeature> getLoadedFeatures() {
    return Collections.unmodifiableSet(loadedFeatures);
  }

  /**
   * Gets whether the mc version is pre 1.9, used by the {@link LanguageManager} system
   *
   * @return whether this is before 1.9
   */
  public boolean isPrior19() {
    return before19;
  }

  /**
   * {@inheritDoc}
   *
   * @return JavaPlugin#getFile
   */
  @Override
  public File getFile() {
    return super.getFile();
  }

  /**
   * @return getFile as Path
   */
  public Path getPath() {
    return getFile().toPath();
  }

  /**
   * @return getDataFolder as Path
   */
  public Path getDataPath() {
    return getDataFolder().toPath();
  }

  @Override
  public void onDisable() {
    if (ServerHelper.getImplementation() == ServerHelper.ImplementationType.GLOWSTONE) {
      return;
    }
    // Save Data if compatible
    if (compatible) {
      DataManager.saveData();
    } else { // If not, we need to remove the cleanup thread
      Runtime.getRuntime().removeShutdownHook(cleanup);
    }
    LOG.info("Disabled AntiLaby by the AntiLaby Team version " + getDescription().getVersion() + " successfully!");
  }

  /**
   * This method is called by PluginManager when the plugin is enabling.
   */
  @Override
  public void onEnable() {
    // Glowstone is not supported yet
    if (ServerHelper.getImplementation() == ServerHelper.ImplementationType.GLOWSTONE) {
      return;
    }
    // Delete datamanager file on exit
    Runtime.getRuntime().addShutdownHook(cleanup);
    // Check if the server is compatible with AntiLaby
    final String mcVersion = FeatureProvider.getMCVersion();
    int version = 0;
    try {
      version = Integer.parseInt(mcVersion.split("\\.")[1]);
    } catch (final NumberFormatException e) {
      LOG.fatal("Unknown NMS version (" + mcVersion + ')');
      compatible = false;
      disableIfNotCompatible();
    }
    if (version >= 8) {
      // Ensure the DataFolder exists
      Path dataPath = getDataPath();
      if (!Files.isDirectory(dataPath)) {
        try {
          Files.createDirectory(dataPath);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      compatible = true;
      LOG.debug("Your server (NMS version " + mcVersion + ") is compatible with AntiLaby!");
    } else {
      compatible = false;
      LOG.error("Your server is not compatible with AntiLaby!");
      disableIfNotCompatible();
    }
    // Try to update AntiLaby
    new UpdateManager().run();
    // Init files, commands and events
    initConfig();
    // Register plug-in channels
    Bukkit.getMessenger().registerOutgoingPluginChannel(this, "DAMAGEINDICATOR");
    Bukkit.getMessenger().registerIncomingPluginChannel(this, Constants.LABYMOD_CHANNEL,
        new IncomingPluginChannel());
    Bukkit.getMessenger().registerOutgoingPluginChannel(this, Constants.LABYMOD_CHANNEL);
    Bukkit.getMessenger().registerOutgoingPluginChannel(this, Constants.LABYMOD_CHANNEL_OLD);
    Bukkit.getMessenger().registerIncomingPluginChannel(this, Constants.LABYMOD_CHANNEL_OLD,
        new IncomingPluginChannel());

    // Init ProtocolLib support
    if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
      ProtocolLibSupport.init();
      loadedFeatures.add(PluginFeature.PROTOCOL_LIB);
    } else if (version > 8) {
      LOG.debug("ProtocolLib is not installed, falling back to possibly inaccurate legacy implementation.");
    } else {
      LOG.debug("ProtocolLib is not installed and version is < 1.9, using reflection to get locale...");
      before19 = true;
    }
    initCmds();
    initEvents();
    // Load data
    DataManager.loadData();
    // Start plug-in metrics for MCStats.org
    try {
      Metrics metrics = new Metrics(this);
      metrics.start();
    } catch (final IOException e) {
      LOG.error(e.getMessage());
    }
    // Init LanguageManager
    try {
      LanguageManager.INSTANCE.initAL();
    } catch (IOException e) {
      e.printStackTrace();
      LOG.error("Could not convert language folder! ");
    }
    // Start plug-in metrics for bStats.org
    initBMetrics();
    // Resend AntiLaby packages (on reload)
    for (final Player all : Bukkit.getOnlinePlayers()) {
      new AntiLabyPackager(all).sendPackages();
    }
    LOG.info("Enabled AntiLaby by the AntiLaby Team version " + getDescription().getVersion() + " sucsessfully!");
    LOG.info("If you want to support us visit " + Constants.GITHUB_URL);
  }

  /**
   * Disables the plug-in if not compatible
   */
  public void disableIfNotCompatible() {
    if (!compatible) {
      getPluginLoader().disablePlugin(this);
    }
  }

  private void initConfig() {
    ConfigFile.load();
  }

  /**
   * Initializes and registers the AntiLaby commands
   */
  private void initCmds() {
    new AntiLabyCommand();
    new LabyInfoCommand();
  }

  /**
   * Initializes and registers the EventListeners
   */
  private void initEvents() {
    final PluginManager pm = Bukkit.getPluginManager();
    if (!before19) {
      pm.registerEvents(new EventsPost18(), this);
    }
    pm.registerEvents(new PlayerJoin(), this);
    pm.registerEvents(new IncomingPluginChannel(), this);
  }

  /**
   * Initializes the <a href="https://bstats.org/plugin/bukkit/AntiLaby">BStats</a> Metrics
   */
  private void initBMetrics() {
    // Start plug-in metrics for bStats.org
    BStatsHandler.initBStats(this);
  }

  /**
   * This method is called by PluginManager when the plugin is loading.
   */
  @Override
  public void onLoad() {
    if (ServerHelper.getImplementation() == ServerHelper.ImplementationType.GLOWSTONE) {
      LOG.error("Glowstone is not yet supported");
      Bukkit.getPluginManager().disablePlugin(this);
    }
    instance = this;
    version = Version.valueOf(getDescription().getVersion());
  }
}