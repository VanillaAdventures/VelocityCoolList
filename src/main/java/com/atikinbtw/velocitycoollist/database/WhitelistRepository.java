package com.atikinbtw.velocitycoollist.database;

import com.atikinbtw.velocitycoollist.VelocityCoolList;

import java.util.List;

public class WhitelistRepository {
    private static WhitelistRepository INSTANCE;
    private final DatabaseManager databaseManager;
    
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
    }
    
    public boolean contains(String username) {
        try {
            return databaseManager.containsPlayer(username).get();
        } catch (Exception e) {
            VelocityCoolList.LOGGER.error("Ошибка при проверке игрока в whitelist: ", e);
            return false;
        }
    }
    
    public boolean addPlayer(String username) {
        try {
            return databaseManager.addPlayer(username).get();
        } catch (Exception e) {
            VelocityCoolList.LOGGER.error("Ошибка при добавлении игрока в whitelist: ", e);
            return false;
        }
    }
    
    public boolean removePlayer(String username) {
        try {
            return databaseManager.removePlayer(username).get();
        } catch (Exception e) {
            VelocityCoolList.LOGGER.error("Ошибка при удалении игрока из whitelist: ", e);
            return false;
        }
    }
    
    public List<String> getAllPlayers() {
        try {
            return databaseManager.getAllPlayers().get();
        } catch (Exception e) {
            VelocityCoolList.LOGGER.error("Ошибка при получении списка игроков: ", e);
            return List.of();
        }
    }
    
    public boolean clear() {
        try {
            return databaseManager.clearWhitelist().get();
        } catch (Exception e) {
            VelocityCoolList.LOGGER.error("Ошибка при очистке whitelist: ", e);
            return false;
        }
    }
    
    public boolean isEmpty() {
        try {
            return databaseManager.isEmpty().get();
        } catch (Exception e) {
            VelocityCoolList.LOGGER.error("Ошибка при проверке пустоты whitelist: ", e);
            return true;
        }
    }
    
    public List<String> getWhitelist() {
        return getAllPlayers();
    }
    
    public void shutdown() {
        databaseManager.shutdown();
    }
}
