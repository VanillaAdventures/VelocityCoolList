package com.atikinbtw.velocitycoollist;

import com.atikinbtw.velocitycoollist.commands.MainCommand;
import com.atikinbtw.velocitycoollist.discord.DiscordModule;
import com.google.inject.Inject;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Date;
import java.util.Scanner;

@Plugin(
        id = "velocitycoollist",
        name = "VelocityCoolList",
        version = "2.1.0-SNAPSHOT",
        description = "Plugin for Velocity that provides nickname-based whitelisting without complex!",
        url = "https://modrinth.com/plugin/velocitycoollist",
        authors = {"atikiNBTW"}
)
public class VelocityCoolList {
    public static final String VERSION = "2.1.0-SNAPSHOT";
    public static Logger LOGGER = LoggerFactory.getLogger("VelocityCoolList-TEMP");
    public final Path DATADIRECTORY;
    private final ProxyServer PROXY;
    private DiscordModule discordModule;

    @Inject
    public VelocityCoolList(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.PROXY = proxy;
        LOGGER = logger;
        this.DATADIRECTORY = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        Date startTime = new Date(System.currentTimeMillis());
        new Config(this).init();
        new Whitelist(this).init();

        // Регистрируем обработчик событий LimboAPI с высоким приоритетом
        // только если интеграция включена в конфигурации
        if (Config.getInstance().getBoolean("limbo_integration")) {
            PROXY.getEventManager().register(this, new LimboWhitelistHandler(this));
            LOGGER.info("LimboAPI integration enabled - whitelist check will have highest priority");
        }

        CommandManager commandManager = PROXY.getCommandManager();

        CommandMeta commandMeta = commandManager.metaBuilder("vclist")
                .aliases("vcl", "velocitycoollist")
                .plugin(this)
                .build();

        BrigadierCommand commandToRegister = MainCommand.createBrigadierCommand(PROXY);
        commandManager.register(commandMeta, commandToRegister);

        // Инициализация Discord модуля
        if (Config.getInstance().getBoolean("discord.enabled")) {
            try {
                String token = Config.getInstance().getString("discord.token");
                String guildId = Config.getInstance().getString("discord.guild_id");
                List<String> allowedUsers = Config.getInstance().getStringList("discord.allowed_users");
                
                if (token != null && !token.equals("YOUR_DISCORD_BOT_TOKEN") && 
                    guildId != null && !guildId.equals("YOUR_GUILD_ID")) {
                    this.discordModule = new DiscordModule(this, token, guildId, allowedUsers);
                } else {
                    LOGGER.warn("Discord интеграция включена, но токен или ID сервера не настроены!");
                }
            } catch (Exception e) {
                LOGGER.error("Ошибка при инициализации Discord модуля: " + e.getMessage(), e);
            }
        }

        LOGGER.info("VelocityCoolList has been enabled! Took {}ms", System.currentTimeMillis() - startTime.getTime());
        scheduleTask(() -> {
            if (Config.getInstance().getBoolean("update_checker"))
                checkForUpdates();
        });
    }

    @Subscribe(priority = 1000) // Низкий приоритет - работает как резервный обработчик
    private void onPlayerJoin(ServerPreConnectEvent event) {
        if (!Config.getInstance().getBoolean("enabled")) return;
        Player player = event.getPlayer();

        Config.getInstance().getInt("config_version");
        if (player.hasPermission("vclist.bypass") || Whitelist.getInstance().contains(player.getUsername()))
            return;

        player.disconnect(MiniMessage.miniMessage().deserialize(Config.getInstance().getMessage("kick_message")).asComponent());
        event.setResult(ServerPreConnectEvent.ServerResult.denied());
    }

    private void checkForUpdates() {
        Runnable task = (() -> {
            String apiURL = "https://api.modrinth.com/v2/project/velocitycoollist/version?featured=true";

            try {
                URL url = URI.create(apiURL).toURL();
                URLConnection conn = url.openConnection();
                InputStream is = conn.getInputStream();

                String text;
                try (Scanner scanner = new Scanner(is, StandardCharsets.UTF_8)) {
                    text = scanner.useDelimiter("\\A").next();
                }

                if (text.isEmpty()) {
                    LOGGER.error("Error happened while getting the latest plugin version");
                    return;
                }

                String version = text.split("\"version_number\":\"")[1].split("\"")[0];
                String newVerUrl = text.split("\"url\":\"")[1].split("\"")[0];

                if (!version.equals(VERSION))
                    LOGGER.info("New version {} of the plugin is available, please update! Url to the new version: {}", version, newVerUrl);
            } catch (IOException e) {
                LOGGER.error("Error happened while getting the latest plugin version: ", e);
            }
        });

        scheduleTask(task);
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        try {
            LOGGER.info("Отключение плагина VelocityCoolList...");
            
            if (discordModule != null) {
                discordModule.shutdown();
            }
            
            // Отключаем базу данных
            if (Whitelist.getInstance() != null) {
                Whitelist.getInstance().shutdown();
            }
            
            LOGGER.info("Плагин VelocityCoolList успешно отключен!");
            
        } catch (Exception e) {
            LOGGER.error("Ошибка при отключении плагина: " + e.getMessage(), e);
        }
    }

    public void scheduleTask(Runnable runnable) {
        PROXY.getScheduler().buildTask(this, runnable).schedule();
    }
}