package ru.siaw.chatmotd;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.CachedServerIcon;
import org.jetbrains.annotations.Nullable;
import ru.mrbrikster.chatty.api.events.ChattyMessageEvent;
import ru.siaw.chatmotd.data.Config;
import ru.siaw.chatmotd.data.Database;

import javax.imageio.ImageIO;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public final class ChatMOTD extends JavaPlugin {
    public static ChatMOTD instance;

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Random random = new Random();

    private static final String SKIN_URL = "https://visage.surgeplay.com/face/64/";
    private static final String UUID_URL = "https://api.mojang.com/users/profiles/minecraft/";
    private static final Pattern UUID_PATTERN = Pattern.compile("(.{8})(.{4})(.{4})(.{4})(.{12})");

    private static ChatMessage chatMessage;
    private static CachedServerIcon nowIcon;

    private static final Map<String, CachedServerIcon> cachedIcons = new HashMap<>();
    private static final Map<String, UUID> uuids = new HashMap<>();

    public ChatMOTD() {
        instance = this;
    }

    @Override
    public void onEnable() {
        Config.init();
        Database.init();

        updateMessage(Database.calculateSavedMessage());
        List<String> motd = new ArrayList<>();

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            motd.clear();

            List<String> newMotd = Config.getStringList("motd");
            int size = newMotd.size();

            if (size > 0) {
                motd.add(newMotd.get(random.nextInt(size)));
            }

            if (chatMessage != null) {
                motd.add(Config.getString("message-line")
                        .replace("%player", chatMessage.getSenderName())
                        .replace("%message", chatMessage.getMessage()));
            }
        }, 20L, 20L);

        Bukkit.getPluginManager().registerEvents(new Listener() {

            @EventHandler
            public void onServerListPing(ServerListPingEvent event) {
                if (!motd.isEmpty()) {
                    event.setMotd(String.join("\n"));
                }

                if (nowIcon != null) {
                    event.setServerIcon(nowIcon);
                }
            }
        }, this);

        if (Bukkit.getPluginManager().isPluginEnabled("Chatty")) {
            getLogger().warning(() -> "Integration with Chatty was successful.");
            Bukkit.getPluginManager().registerEvents(new Listener() {

                @EventHandler(priority = EventPriority.MONITOR)
                public void onChat(ChattyMessageEvent e) {
                    if (e.getChat().getRange() < 0) {
                        executor.execute(() -> updateMessage(e.getPlayer().getName(), e.getMessage()));
                    }
                }
            }, this);
        } else {
            getLogger().warning(() -> "Vanilla chat is used.");
            Bukkit.getPluginManager().registerEvents(new Listener() {

                @EventHandler(priority = EventPriority.MONITOR)
                public void onChat(AsyncPlayerChatEvent e) {
                    if (!e.isCancelled()) {
                        executor.execute(() -> updateMessage(e.getPlayer().getName(), e.getMessage()));
                    }
                }
            }, this);
        }

        PluginCommand chatmotd = getCommand("chatmotd");

        chatmotd.setExecutor((sender, command, s, args) -> {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("chatmotd.reload")) {
                    Config.init();
                    Database.init();

                    sender.sendMessage(ChatColor.GREEN + "Plugin has been reloaded.");
                } else {
                    sender.sendMessage(ChatColor.RED + "You do not have permission!");
                }
            }

            return true;
        });

        chatmotd.setTabCompleter((sender, command, s, args) -> {
            if (args.length == 1 && sender.hasPermission("chatmotd.reload")) {
                return Collections.singletonList("reload");
            }

            return null;
        });
    }

    private static void updateMessage(@Nullable String senderName, String message) {
        if (senderName != null && !message.isEmpty()) {
            updateMessage(new ChatMessage(senderName, ChatColor.stripColor(message)));
        }
    }

    private static void updateMessage(@Nullable ChatMessage message) {
        if (message != null) {
            String name = message.getSenderName();
            CachedServerIcon icon = cachedIcons.get(name);

            if (icon == null) {
                UUID uuid = UUIDFromName(name);

                if (uuid != null) {
                    try {
                        icon = Bukkit.getServer().loadServerIcon(ImageIO.read(new URL(SKIN_URL + uuid)));
                        cachedIcons.put(name, icon);

                    } catch (Exception ignored) {
                    }
                }
            }

            nowIcon = icon;
            chatMessage = message;
        }
    }

    private static UUID UUIDFromName(String name) {
        UUID uuid = uuids.get(name);

        if (uuid != null) {
            return uuid;
        }

        try {
            HttpURLConnection con = (HttpURLConnection) new URL(UUID_URL + name).openConnection();
            con.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuilder builder = new StringBuilder();

            String inputLine;
            while ((inputLine = reader.readLine()) != null) {
                builder.append(inputLine);
            }

            reader.close();
            String result = builder.toString();

            if (result.contains("id")) {
                JsonObject jsonObject = new Gson().fromJson(result, JsonObject.class);
                uuid = UUID.fromString(UUID_PATTERN
                        .matcher(jsonObject.get("id").getAsString())
                        .replaceFirst("$1-$2-$3-$4-$5"));

                uuids.put(name, uuid);
                return uuid;
            }
        } catch (IOException ignored) {
        }

        return null;
    }

    @Override
    public void onDisable() {
        if (chatMessage != null) {
            Database.saveMessage(chatMessage);
        }
    }
}
