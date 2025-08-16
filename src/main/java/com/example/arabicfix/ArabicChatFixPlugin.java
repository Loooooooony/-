package com.example.arabicfix;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.regex.Pattern;

public class ArabicChatFixPlugin extends JavaPlugin implements Listener {

    private boolean enabledForAll = true;

    private static final Pattern ARABIC_RANGE = Pattern.compile(
            "[\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF\\uFB50-\\uFDFF\\uFE70-\\uFEFF]"
    );
    private static final Pattern LATIN_OR_DIGITS = Pattern.compile("([0-9A-Za-z@#:/._+-]+)");

    private boolean applyOnlyIfArabic;
    private boolean wrapWithDirectionMarks;
    private boolean convertToEasternDigits;
    private boolean onlyBedrockPlayers;

    private static final char[] EASTERN_DIGITS = {'\u0660','\u0661','\u0662','\u0663','\u0664','\u0665','\u0666','\u0667','\u0668','\u0669'};
    private static final char RLM = '\u200F';
    private static final char ALM = '\u061C';

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadLocalConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("ArabicChatFix enabled.");
    }

    private void reloadLocalConfig() {
        reloadConfig();
        applyOnlyIfArabic      = getConfig().getBoolean("apply_only_if_arabic", true);
        wrapWithDirectionMarks = getConfig().getBoolean("wrap_with_direction_marks", true);
        convertToEasternDigits = getConfig().getBoolean("convert_to_eastern_digits", false);
        onlyBedrockPlayers     = getConfig().getBoolean("only_bedrock_players", false);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();

        if (!enabledForAll) return;
        if (p.hasPermission("arabicfix.bypass")) return;
        if (onlyBedrockPlayers && !isBedrock(p)) return;

        String msg = e.getMessage();
        if (applyOnlyIfArabic && !containsArabic(msg)) return;

        String fixed = fixArabic(msg);
        e.setMessage(fixed);
    }

    private boolean containsArabic(String s) {
        return ARABIC_RANGE.matcher(s).find();
    }

    private boolean isBedrock(Player p) {
        try {
            Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            UUID uuid = p.getUniqueId();
            Boolean isFlood = (Boolean) apiClass.getMethod("isFloodgatePlayer", UUID.class).invoke(api, uuid);
            return Boolean.TRUE.equals(isFlood);
        } catch (Throwable t) {
            return false;
        }
    }

    private String fixArabic(String text) {
        String s = text;

        if (convertToEasternDigits) {
            s = mapWesternDigitsToEastern(s);
        }

        s = LATIN_OR_DIGITS.matcher(s).replaceAll(m -> ALM + m.group(1));

        int[] cps = s.codePoints().toArray();
        int n = cps.length;
        int[] reversed = new int[n];
        for (int i = 0; i < n; i++) reversed[i] = cps[n - 1 - i];
        String rev = new String(reversed, 0, n);

        if (wrapWithDirectionMarks) {
            return RLM + rev + RLM;
        } else {
            return rev;
        }
    }

    private String mapWesternDigitsToEastern(String s) {
        StringBuilder out = new StringBuilder(s.length());
        s.codePoints().forEach(cp -> {
            if (cp >= '0' && cp <= '9') {
                out.append(EASTERN_DIGITS[cp - '0']);
            } else {
                out.appendCodePoint(cp);
            }
        });
        return out.toString();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("arabicfix")) return false;

        if (args.length == 0) {
            sender.sendMessage("§aArabicChatFix: §f/arabicfix reload أو /arabicfix toggle");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("arabicfix.admin")) {
                    sender.sendMessage("§cما عندك صلاحية.");
                    return true;
                }
                reloadLocalConfig();
                sender.sendMessage("§aتم إعادة تحميل الإعدادات.");
                return true;
            case "toggle":
                if (!sender.hasPermission("arabicfix.admin")) {
                    sender.sendMessage("§cما عندك صلاحية.");
                    return true;
                }
                enabledForAll = !enabledForAll;
                sender.sendMessage("§eArabicChatFix " + (enabledForAll ? "§aمفعل" : "§cموقّف") + "§e.");
                return true;
            default:
                sender.sendMessage("§aArabicChatFix: §f/arabicfix reload أو /arabicfix toggle");
                return true;
        }
    }
}
