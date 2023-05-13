package ru.siaw.chatmotd.data;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;
import ru.siaw.chatmotd.ChatMessage;

import java.io.File;
import java.io.IOException;
import java.util.Base64;

import static ru.siaw.chatmotd.ChatMOTD.instance;

public class Database {
    private static final File file = new File(instance.getDataFolder(), "data.yml");
    private static final YamlConfiguration configuration = new YamlConfiguration();

    public static void init() {
        try {
            if (!file.exists()) {
                file.createNewFile();
            }

            configuration.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    public static ChatMessage calculateSavedMessage() {
        String name = configuration.getString("senderName");

        if (name != null) {
            String message = configuration.getString("message");

            if (message != null) {
                Base64.Decoder decoder = Base64.getDecoder();

                name = new String(decoder.decode(name));
                message = new String(decoder.decode(message));

                return new ChatMessage(name, message);
            }
        }

        return null;
    }

    public static void saveMessage(ChatMessage message) {
        Base64.Encoder encoder = Base64.getEncoder();

        configuration.set("senderName", new String(
                encoder.encode(message.getSenderName().getBytes())));

        configuration.set("message", new String(
                encoder.encode(message.getMessage().getBytes())));

        try {
            configuration.save(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}