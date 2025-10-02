package com.atikinbtw.velocitycoollist.database;

import com.atikinbtw.velocitycoollist.VelocityCoolList;
import lombok.Getter;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class WhitelistRepository {
    private static WhitelistRepository INSTANCE;
    private final DatabaseManager databaseManager;
    
    @Getter
    private List<String> cachedWhitelist;
    
    public WhitelistRepository(VelocityCoolList plugin) {
        this.databaseManager = new DatabaseManager(plugin);
        INSTANCE = this;
    }
    
    public static WhitelistRepository getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("WhitelistRepository has not been initialized");
        }
        return INSTANCE;
    }
    
    public void initialize() {
        databaseManager.initialize();
        refreshCache();
    }
    
    public void refreshCache() {
        databaseManager.getAllPlayers().thenAccept(players -> {
            this.cachedWhitelist = players;
            VelocityCoolList.LOGGER.debug("Кеш whitelist обновлен, загружено {} игроков", players.size());
        }).exceptionally(throwable -> {
            VelocityCoolList.LOGGER.error("Ошибка при обновлении кеша whitelist: ", throwable);
            return null;
        });
    }
    
    public boolean contains(String username) {
        if (cachedWhitelist == null) {
            // Если кеш еще не загружен, делаем синхронный запрос
            try {
                return databaseManager.containsPlayer(username).get();
            } catch (Exception e) {
                VelocityCoolList.LOGGER.error("Ошибка при синхронной проверке игрока: ", e);
                return false;
            }
        }
        
        // Используем кеш для быстрой проверки (case-insensitive)
        String lowercaseUsername = username.toLowerCase();
        return cachedWhitelist.stream()
                .anyMatch(player -> player.toLowerCase().equals(lowercaseUsername));
    }
    
    public CompletableFuture<Boolean> addPlayer(String username) {
        return databaseManager.addPlayer(username).thenApply(success -> {
            if (success) {
                refreshCache();
            }
            return success;
        });
    }
    
    public CompletableFuture<Boolean> removePlayer(String username) {
        return databaseManager.removePlayer(username).thenApply(success -> {
            if (success) {
                refreshCache();
            }
            return success;
        });
    }
    
    public CompletableFuture<List<String>> getAllPlayers() {
        return databaseManager.getAllPlayers();
    }
    
    public CompletableFuture<Boolean> clear() {
        return databaseManager.clearWhitelist().thenApply(success -> {
            if (success) {
                refreshCache();
            }
            return success;
        });
    }
    
    public boolean isEmpty() {
        if (cachedWhitelist == null) {
            try {
                return databaseManager.isEmpty().get();
            } catch (Exception e) {
                VelocityCoolList.LOGGER.error("Ошибка при синхронной проверке пустоты: ", e);
                return true;
            }
        }
        
        return cachedWhitelist.isEmpty();
    }
    
    public List<String> getWhitelist() {
        return cachedWhitelist;
    }
    
    public void shutdown() {
        databaseManager.shutdown();
    }
}
