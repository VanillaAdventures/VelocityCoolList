package com.atikinbtw.velocitycoollist;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Обработчик событий LimboAPI с высоким приоритетом для проверки вайтлиста
 * перед событиями телепортации в LimboAuth
 */
public class LimboWhitelistHandler {
    private final VelocityCoolList plugin;

    public LimboWhitelistHandler(VelocityCoolList plugin) {
        this.plugin = plugin;
    }

    /**
     * Обработчик события входа игрока с максимальным приоритетом
     * Проверяет вайтлист до любых других событий LimboAPI
     */
    @Subscribe(priority = 10000) // Высокий приоритет
    public void onPlayerPreLogin(PreLoginEvent event) {
        if (!Config.getInstance().getBoolean("enabled") || !Config.getInstance().getBoolean("limbo_integration")) return;
        
        String username = event.getUsername();
        
        // Проверяем наличие в вайтлисте
        if (Whitelist.getInstance().contains(username)) {
            return; // Игрок может проходить дальше
        }
        
        // Игрок не в вайтлисте - блокируем вход
        event.setResult(PreLoginEvent.PreLoginComponentResult.denied(
            MiniMessage.miniMessage().deserialize(Config.getInstance().getMessage("kick_message"))
        ));
    }
}
