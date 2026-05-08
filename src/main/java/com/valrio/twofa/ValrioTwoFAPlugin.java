package com.valrio.twofa;

import org.bukkit.plugin.java.JavaPlugin;

public class ValrioTwoFAPlugin extends JavaPlugin {
    private AccountStore accountStore;
    private TwoFaService twoFaService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.accountStore = new AccountStore(this);
        this.accountStore.load();
        this.twoFaService = new TwoFaService(this);

        AuthCommand authCommand = new AuthCommand(this, accountStore, twoFaService);
        getCommand("register").setExecutor(authCommand);
        getCommand("login").setExecutor(authCommand);
        getCommand("twofa").setExecutor(authCommand);

        getLogger().info("Valrio2FA enabled.");
    }

    @Override
    public void onDisable() {
        accountStore.save();
    }

    public String msg(String key) {
        String prefix = getConfig().getString("messages.prefix", "");
        String value = getConfig().getString("messages." + key, key);
        return Chat.color(prefix + value);
    }
}
