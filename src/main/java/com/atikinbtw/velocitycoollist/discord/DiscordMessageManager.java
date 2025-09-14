package com.atikinbtw.velocitycoollist.discord;

import com.atikinbtw.velocitycoollist.Config;
import java.util.HashMap;
import java.util.Map;

public class DiscordMessageManager {
    
    private static DiscordMessageManager INSTANCE;
    private final Config config;
    
    public DiscordMessageManager(Config config) {
        this.config = config;
        INSTANCE = this;
    }
    
    public static DiscordMessageManager getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("DiscordMessageManager has not been initialized");
        }
        return INSTANCE;
    }
    
    
    public String getDiscordMessage(String type, String action, String... placeholders) {
        return getMessage("discord." + type + "." + action, "❌ Сообщение не найдено!", placeholders);
    }
    
    public String getLogMessage(String action, String... placeholders) {
        return getMessage("discord.logging." + action, "Лог сообщение не найдено!", placeholders);
    }
    
    private String getMessage(String key, String defaultMessage, String... placeholders) {
        String message = config.getMessages().getString(key);
        
        if (message == null) {
            com.atikinbtw.velocitycoollist.VelocityCoolList.LOGGER.warn("Сообщение не найдено: " + key);
            return defaultMessage;
        }
        
        return replacePlaceholders(message, placeholders);
    }
    
    private String replacePlaceholders(String message, String... placeholders) {
        if (placeholders.length == 0) {
            return message;
        }
        
        Map<String, String> replacements = new HashMap<>();
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                replacements.put(placeholders[i], placeholders[i + 1]);
            }
        }
        
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            message = message.replace("$" + entry.getKey(), entry.getValue());
        }
        
        return message;
    }
}
