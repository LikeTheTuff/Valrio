package com.valrio.twofa;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AccountStore {
    private final ValrioTwoFAPlugin plugin;
    private final Map<UUID, Account> accounts = new HashMap<>();
    private final File file;

    public AccountStore(ValrioTwoFAPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "accounts.yml");
    }

    public void load() {
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        for (String key : yml.getKeys(false)) {
            UUID uuid = UUID.fromString(key);
            String hash = yml.getString(key + ".passwordHash", "");
            boolean twofa = yml.getBoolean(key + ".twofa", false);
            accounts.put(uuid, new Account(hash, twofa));
        }
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();
        for (Map.Entry<UUID, Account> entry : accounts.entrySet()) {
            String base = entry.getKey().toString();
            yml.set(base + ".passwordHash", entry.getValue().passwordHash);
            yml.set(base + ".twofa", entry.getValue().twoFaEnabled);
        }
        try {
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save accounts.yml: " + e.getMessage());
        }
    }

    public boolean isRegistered(UUID uuid) {
        return accounts.containsKey(uuid);
    }

    public void register(UUID uuid, String password) {
        accounts.put(uuid, new Account(sha256(password), false));
    }

    public boolean checkPassword(UUID uuid, String password) {
        Account account = accounts.get(uuid);
        return account != null && account.passwordHash.equals(sha256(password));
    }

    public boolean isTwoFaEnabled(UUID uuid) {
        Account account = accounts.get(uuid);
        return account != null && account.twoFaEnabled;
    }

    public void setTwoFaEnabled(UUID uuid, boolean enabled) {
        Account account = accounts.get(uuid);
        if (account != null) {
            account.twoFaEnabled = enabled;
        }
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static final class Account {
        private final String passwordHash;
        private boolean twoFaEnabled;

        private Account(String passwordHash, boolean twoFaEnabled) {
            this.passwordHash = passwordHash;
            this.twoFaEnabled = twoFaEnabled;
        }
    }
}
