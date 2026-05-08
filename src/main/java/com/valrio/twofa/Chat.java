package com.valrio.twofa;

import org.bukkit.ChatColor;

public final class Chat {
    private Chat() {}

    public static String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}
