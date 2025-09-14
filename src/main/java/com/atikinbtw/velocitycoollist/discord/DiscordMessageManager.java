package com.atikinbtw.velocitycoollist.discord;

import com.atikinbtw.velocitycoollist.Config;
import org.simpleyaml.configuration.file.YamlFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class DiscordMessageManager {
    
    private static DiscordMessageManager INSTANCE;
    private final Config config;
    private YamlFile messages;
    
    public DiscordMessageManager(Config config) {
        this.config = config;
        this.messages = new YamlFile(Path.of(config.getPlugin().DATADIRECTORY + "/messages.yml").toUri());
        INSTANCE = this;
    }
    
    public static DiscordMessageManager getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("DiscordMessageManager has not been initialized");
        }
        return INSTANCE;
    }
    
    public void reload() {
        try {
            messages.loadWithComments();
        } catch (IOException e) {
            com.atikinbtw.velocitycoollist.VelocityCoolList.LOGGER.error("Ошибка при перезагрузке Discord сообщений: " + e.getMessage(), e);
        }
    }
    
    public String getDiscordMessage(String type, String action, String... placeholders) {
        String key = "discord." + type + "." + action;
        String message = messages.getString(key);
        
        if (message == null) {
            com.atikinbtw.velocitycoollist.VelocityCoolList.LOGGER.warn("Discord сообщение не найдено: " + key);
            return "❌ Сообщение не найдено!";
        }
        
        // Заменяем плейсхолдеры
        if (placeholders.length > 0) {
            Map<String, String> replacements = new HashMap<>();
            for (int i = 0; i < placeholders.length; i += 2) {
                if (i + 1 < placeholders.length) {
                    replacements.put(placeholders[i], placeholders[i + 1]);
                }
            }
            
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                message = message.replace("$" + entry.getKey(), entry.getValue());
            }
        }
        
        return message;
    }
    
    public String getLogMessage(String action, String... placeholders) {
        String key = "discord.logging." + action;
        String message = messages.getString(key);
        
        if (message == null) {
            com.atikinbtw.velocitycoollist.VelocityCoolList.LOGGER.warn("Лог сообщение не найдено: " + key);
            return "Лог сообщение не найдено!";
        }
        
        // Заменяем плейсхолдеры
        if (placeholders.length > 0) {
            Map<String, String> replacements = new HashMap<>();
            for (int i = 0; i < placeholders.length; i += 2) {
                if (i + 1 < placeholders.length) {
                    replacements.put(placeholders[i], placeholders[i + 1]);
                }
            }
            
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                message = message.replace("$" + entry.getKey(), entry.getValue());
            }
        }
        
        return message;
    }
}
