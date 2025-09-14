package com.atikinbtw.velocitycoollist.discord;

import com.atikinbtw.velocitycoollist.VelocityCoolList;
import com.atikinbtw.velocitycoollist.Whitelist;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.util.List;

public class DiscordModule extends ListenerAdapter {
    
    private final VelocityCoolList plugin;
    private final JDA jda;
    private final String guildId;
    private final List<String> allowedUsers;
    private final DiscordMessageManager messageManager;
    
    public DiscordModule(VelocityCoolList plugin, String token, String guildId, List<String> allowedUsers) {
        this.plugin = plugin;
        this.guildId = guildId;
        this.allowedUsers = allowedUsers;
        this.messageManager = new DiscordMessageManager(com.atikinbtw.velocitycoollist.Config.getInstance());
        
        // Создаем JDA экземпляр
        this.jda = JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(this)
                .build();
        
        try {
            // Ждем готовности
            jda.awaitReady();
            
            // Регистрируем команды
            registerCommands();
            
            VelocityCoolList.LOGGER.info("Discord модуль успешно инициализирован!");
            
        } catch (Exception e) {
            VelocityCoolList.LOGGER.error("Ошибка при инициализации Discord модуля: " + e.getMessage(), e);
        }
    }
    
    private void registerCommands() {
        try {
            Guild guild = jda.getGuildById(guildId);
            if (guild == null) {
                VelocityCoolList.LOGGER.error("Сервер Discord с ID " + guildId + " не найден!");
                return;
            }
            
            // Создаем слеш команды
            SlashCommandData whitelistAddCommand = Commands.slash("whitelist-add", "Добавить игрока в вайтлист")
                    .addOption(OptionType.STRING, "username", "Ник игрока для добавления в вайтлист", true);
            
            SlashCommandData whitelistRemoveCommand = Commands.slash("whitelist-remove", "Удалить игрока из вайтлиста")
                    .addOption(OptionType.STRING, "username", "Ник игрока для удаления из вайтлиста", true);
            
            // Регистрируем команды
            guild.updateCommands()
                    .addCommands(whitelistAddCommand, whitelistRemoveCommand)
                    .queue();
            
            VelocityCoolList.LOGGER.info("Discord команды успешно зарегистрированы!");
            
        } catch (Exception e) {
            VelocityCoolList.LOGGER.error("Ошибка при регистрации Discord команд: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        try {
            // Проверяем разрешения
            if (!isUserAllowed(event.getUser().getId())) {
                event.reply(messageManager.getDiscordMessage("error", "no_permission")).setEphemeral(true).queue();
                return;
            }
            
            String commandName = event.getName();
            String username = event.getOption("username").getAsString();
            
            switch (commandName) {
                case "whitelist-add":
                    handleWhitelistAdd(event, username);
                    break;
                case "whitelist-remove":
                    handleWhitelistRemove(event, username);
                    break;
                default:
                    event.reply(messageManager.getDiscordMessage("error", "unknown_command")).setEphemeral(true).queue();
            }
            
        } catch (Exception e) {
            VelocityCoolList.LOGGER.error("Ошибка при обработке Discord команды: " + e.getMessage(), e);
            event.reply(messageManager.getDiscordMessage("error", "execution_error")).setEphemeral(true).queue();
        }
    }
    
    private void handleWhitelistAdd(SlashCommandInteractionEvent event, String username) {
        try {
            Whitelist whitelist = Whitelist.getInstance();
            
            if (whitelist.contains(username)) {
                event.reply(messageManager.getDiscordMessage("warning", "already_whitelisted", "USERNAME", username)).queue();
                VelocityCoolList.LOGGER.info(messageManager.getLogMessage("user_already_whitelisted", "USER", event.getUser().getName(), "USERNAME", username));
                return;
            }
            
            whitelist.addPlayer(username);
            whitelist.saveFile();
            
            event.reply(messageManager.getDiscordMessage("success", "whitelist_add", "USERNAME", username)).queue();
            VelocityCoolList.LOGGER.info(messageManager.getLogMessage("user_added", "USER", event.getUser().getName(), "USERNAME", username));
            
        } catch (Exception e) {
            VelocityCoolList.LOGGER.error("Ошибка при добавлении в вайтлист: " + e.getMessage(), e);
            event.reply(messageManager.getDiscordMessage("error", "add_failed", "USERNAME", username)).setEphemeral(true).queue();
        }
    }
    
    private void handleWhitelistRemove(SlashCommandInteractionEvent event, String username) {
        try {
            Whitelist whitelist = Whitelist.getInstance();
            
            if (!whitelist.contains(username)) {
                event.reply(messageManager.getDiscordMessage("warning", "not_whitelisted", "USERNAME", username)).queue();
                VelocityCoolList.LOGGER.info(messageManager.getLogMessage("user_not_whitelisted", "USER", event.getUser().getName(), "USERNAME", username));
                return;
            }
            
            whitelist.removePlayer(username);
            whitelist.saveFile();
            
            event.reply(messageManager.getDiscordMessage("success", "whitelist_remove", "USERNAME", username)).queue();
            VelocityCoolList.LOGGER.info(messageManager.getLogMessage("user_removed", "USER", event.getUser().getName(), "USERNAME", username));
            
        } catch (Exception e) {
            VelocityCoolList.LOGGER.error("Ошибка при удалении из вайтлиста: " + e.getMessage(), e);
            event.reply(messageManager.getDiscordMessage("error", "remove_failed", "USERNAME", username)).setEphemeral(true).queue();
        }
    }
    
    private boolean isUserAllowed(String userId) {
        return allowedUsers != null && allowedUsers.contains(userId);
    }
    
    public void shutdown() {
        try {
            if (jda != null) {
                jda.shutdown();
                VelocityCoolList.LOGGER.info("Discord модуль отключен!");
            }
        } catch (Exception e) {
            VelocityCoolList.LOGGER.error("Ошибка при отключении Discord модуля: " + e.getMessage(), e);
        }
    }
    
    public JDA getJDA() {
        return jda;
    }
    
    public boolean isConnected() {
        return jda != null && jda.getStatus() == net.dv8tion.jda.api.JDA.Status.CONNECTED;
    }
}
