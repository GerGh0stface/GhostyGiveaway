package ghostygiveaway;

import ghostygiveaway.commands.GiveawayCommand;
import ghostygiveaway.gui.GiveawaySetupGUI;
import ghostygiveaway.listeners.GUIListener;
import ghostygiveaway.utils.LanguageManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * GhostyGiveaway – A fully customizable giveaway plugin.
 *
 * Author  : Ger_Gh0stface
 * Version : 1.0.0
 * Supports: Paper & Spigot 1.21 – 1.21.11
 */
public class GhostyGiveaway extends JavaPlugin {

    private static GhostyGiveaway instance;

    private LanguageManager langManager;
    private GiveawayManager giveawayManager;
    private GiveawaySetupGUI setupGUI;
    private GUIListener guiListener;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        langManager     = new LanguageManager(this);
        giveawayManager = new GiveawayManager(this);
        setupGUI        = new GiveawaySetupGUI(this);
        guiListener     = new GUIListener(this);

        GiveawayCommand cmd = new GiveawayCommand(this);
        getCommand("giveaway").setExecutor(cmd);
        getCommand("giveaway").setTabCompleter(cmd);
        getServer().getPluginManager().registerEvents(guiListener, this);

        getLogger().info("==============================================");
        getLogger().info("  GhostyGiveaway v" + getDescription().getVersion() + " by Ger_Gh0stface");
        getLogger().info("  Language: " + getConfig().getString("language", "en"));
        getLogger().info("==============================================");
    }

    @Override
    public void onDisable() {
        if (giveawayManager != null) giveawayManager.cancelAll();
    }

    public static GhostyGiveaway getInstance() { return instance; }
    public LanguageManager getLangManager()     { return langManager; }
    public GiveawayManager getGiveawayManager() { return giveawayManager; }
    public GiveawaySetupGUI getSetupGUI()       { return setupGUI; }
    public GUIListener getGuiListener()         { return guiListener; }
}
