package com.valrio.twofa;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.regex.Pattern;

public class AuthCommand implements CommandExecutor {
    private final ValrioTwoFAPlugin plugin;
    private final AccountStore store;
    private final TwoFaService service;

    public AuthCommand(ValrioTwoFAPlugin plugin, AccountStore store, TwoFaService service) {
        this.plugin = plugin;
        this.store = store;
        this.service = service;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        switch (command.getName().toLowerCase()) {
            case "register":
                return handleRegister(player, args);
            case "login":
                return handleLogin(player, args);
            case "twofa":
                return handleTwoFa(player, args);
            default:
                return false;
        }
    }

    private boolean handleRegister(Player player, String[] args) {
        if (store.isRegistered(player.getUniqueId())) {
            player.sendMessage(plugin.msg("already-registered"));
            return true;
        }
        if (args.length < 1) {
            player.sendMessage("/register <password>");
            return true;
        }
        store.register(player.getUniqueId(), args[0]);
        store.save();
        player.sendMessage(plugin.msg("register-success"));
        return true;
    }

    private boolean handleLogin(Player player, String[] args) {
        if (!store.isRegistered(player.getUniqueId())) {
            player.sendMessage(plugin.msg("not-registered"));
            return true;
        }
        if (args.length < 1) {
            player.sendMessage("/login <password> [2fa_code]");
            return true;
        }
        if (!store.checkPassword(player.getUniqueId(), args[0])) {
            player.sendMessage(plugin.msg("bad-password"));
            return true;
        }

        if (!store.isTwoFaEnabled(player.getUniqueId())) {
            player.sendMessage(plugin.msg("login-success"));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(plugin.msg("twofa-required"));
            return true;
        }

        String code = args[1];
        String regex = plugin.getConfig().getString("security.twofa-code-regex", "^[0-9]{15,20}$");
        if (!Pattern.compile(regex).matcher(code).matches()) {
            player.sendMessage(plugin.msg("twofa-invalid-format"));
            return true;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                boolean valid = service.verify(player, code);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (valid) {
                        player.sendMessage(plugin.msg("login-success"));
                    } else {
                        player.sendMessage(plugin.msg("twofa-invalid"));
                    }
                });
            } catch (IOException | InterruptedException e) {
                Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(plugin.msg("web-error")));
            }
        });
        return true;
    }

    private boolean handleTwoFa(Player player, String[] args) {
        if (!store.isRegistered(player.getUniqueId())) {
            player.sendMessage(plugin.msg("not-registered"));
            return true;
        }

        if (args.length < 1) {
            boolean enabled = store.isTwoFaEnabled(player.getUniqueId());
            player.sendMessage(enabled ? plugin.msg("twofa-status-on") : plugin.msg("twofa-status-off"));
            return true;
        }

        if (args[0].equalsIgnoreCase("enable")) {
            store.setTwoFaEnabled(player.getUniqueId(), true);
            store.save();
            player.sendMessage(plugin.msg("twofa-enable-success"));
            return true;
        }
        if (args[0].equalsIgnoreCase("disable")) {
            store.setTwoFaEnabled(player.getUniqueId(), false);
            store.save();
            player.sendMessage(plugin.msg("twofa-disable-success"));
            return true;
        }

        boolean enabled = store.isTwoFaEnabled(player.getUniqueId());
        player.sendMessage(enabled ? plugin.msg("twofa-status-on") : plugin.msg("twofa-status-off"));
        return true;
    }
}
