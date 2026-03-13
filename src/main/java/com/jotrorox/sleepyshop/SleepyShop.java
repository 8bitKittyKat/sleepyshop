package com.jotrorox.sleepyshop;

import com.jotrorox.sleepyshop.command.SleepyShopCommand;
import com.jotrorox.sleepyshop.database.DatabaseManager;
import com.jotrorox.sleepyshop.listener.ShopListener;
import com.jotrorox.sleepyshop.listener.SignListener;
import com.jotrorox.sleepyshop.manager.ShopManager;
import com.jotrorox.sleepyshop.util.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class SleepyShop extends JavaPlugin {

    private DatabaseManager databaseManager;
    private ShopManager shopManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.databaseManager = new DatabaseManager(this);
        this.shopManager = new ShopManager(this);
        Bukkit.getPluginManager().registerEvents(
            new ShopListener(shopManager),
            this
        );
        Bukkit.getPluginManager().registerEvents(
            new SignListener(shopManager, this),
            this
        );
        registerCommands();

        getLogger().info("SleepyShop has been enabled! Made by Jotrorox.");

        (new UpdateChecker(this, "Jotrorox/SleepyShop")).performCheck();
    }

    @Override
    public void onDisable() {
        databaseManager.closeConnection();
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    private void registerCommands() {
        PluginCommand sleepyShopCommand = getCommand("sleepyshop");
        if (sleepyShopCommand == null) {
            getLogger().severe("Command registration failed for /sleepyshop.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        SleepyShopCommand executor = new SleepyShopCommand(this, shopManager);
        sleepyShopCommand.setExecutor(executor);
        sleepyShopCommand.setTabCompleter(executor);
    }
}
