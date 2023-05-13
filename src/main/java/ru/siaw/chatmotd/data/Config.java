package ru.siaw.chatmotd.data;

import org.bukkit.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static ru.siaw.chatmotd.ChatMOTD.instance;

public class Config {
    private static final YamlConfiguration configuration = new YamlConfiguration();

    public static void init() {
        File file = new File(instance.getDataFolder(), "config.yml");

        if (!file.exists()) {
            instance.saveResource("config.yml", true);
        }

        try {
            configuration.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<String> getStringList(String path) {
        return configuration.getStringList(path).stream()
                .map(Config::translateColorCodes)
                .collect(Collectors.toList());
    }

    public static String getString(String path) {
        return translateColorCodes(configuration.getString(path));
    }

    private static String translateColorCodes(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
