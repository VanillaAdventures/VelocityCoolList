package com.atikinbtw.velocitycoollist.commands;

import com.atikinbtw.velocitycoollist.Config;
import com.atikinbtw.velocitycoollist.VelocityCoolList;
import com.atikinbtw.velocitycoollist.Whitelist;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class CommandHelper {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    public static int about(CommandContext<CommandSource> context) {
        context.getSource().sendMessage(MINI_MESSAGE.deserialize(Config.getInstance().getString("prefix") + " " + "VelocityCoolList v" + VelocityCoolList.VERSION));

        return Command.SINGLE_SUCCESS;
    }

    public static int status(CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();

        String status = Config.getInstance().getBoolean("enabled") ? Config.getInstance().getMessage("whitelist_enabled") : Config.getInstance().getMessage("whitelist_disabled");

        source.sendMessage(MINI_MESSAGE.deserialize(Config.getInstance().getString("prefix") + " " +
                replacePlaceholders(Config.getInstance().getMessage("status"), 
                        "$STATUS", status,
                        "$SOURCE", getSourceName(source)
                )
        ));

        return Command.SINGLE_SUCCESS;
    }

    public static int enable(CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();

        if (Config.getInstance().getBoolean("enabled")) {
            source.sendMessage(MINI_MESSAGE.deserialize(replacePlaceholders(Config.getInstance().getString("prefix") + " " + Config.getInstance().getMessage("already_enabled"), 
                    "$SOURCE", getSourceName(source))));
        } else {
            Config.getInstance().setAndSave("enabled", true);
            source.sendMessage(MINI_MESSAGE.deserialize(Config.getInstance().getString("prefix") + " " + replacePlaceholders(Config.getInstance().getMessage("enable"), 
                    "$SOURCE", getSourceName(source))));
        }

        return Command.SINGLE_SUCCESS;
    }

    public static int disable(CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();

        if (!(boolean) Config.getInstance().getBoolean("enabled")) {
            source.sendMessage(MINI_MESSAGE.deserialize(Config.getInstance().getString("prefix") + " " + replacePlaceholders(Config.getInstance().getMessage("already_disabled"), 
                    "$SOURCE", getSourceName(source))));
        } else {
            Config.getInstance().setAndSave("enabled", false);
            source.sendMessage(MINI_MESSAGE.deserialize(Config.getInstance().getString("prefix") + " " + replacePlaceholders(Config.getInstance().getMessage("disable"), 
                    "$SOURCE", getSourceName(source))));
        }

        return Command.SINGLE_SUCCESS;
    }

    public static int addUser(CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();
        String username;

        try {
            username = context.getArgument("username", String.class);
        } catch (Exception e) {
            source.sendMessage(MINI_MESSAGE.deserialize(Config.getInstance().getString("prefix") + " " + replacePlaceholders(Config.getInstance().getMessage("add_incorrect_usage"), 
                    "$SOURCE", getSourceName(source))));
            return Command.SINGLE_SUCCESS;
        }

        if (Whitelist.getInstance().contains(username)) {
            source.sendMessage(MINI_MESSAGE.deserialize(Config.getInstance().getString("prefix") + " " +
                            replacePlaceholders(Config.getInstance().getMessage("already_on_whitelist"), 
                                    "$SOURCE", getSourceName(source),
                                    "$PLAYER", username)
                    )
            );

            return Command.SINGLE_SUCCESS;
        }

        // add and save the file
        Whitelist.getInstance().addPlayer(username);
        Whitelist.getInstance().saveFile();
        source.sendMessage(MINI_MESSAGE.deserialize(Config.getInstance().getString("prefix") + " " +
                        replacePlaceholders(Config.getInstance().getMessage("add"), 
                                "$SOURCE", getSourceName(source),
                                "$PLAYER", username)
                )
        );

        return Command.SINGLE_SUCCESS;
    }

    public static int removeUser(CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();
        String username;

        try {
            username = context.getArgument("username", String.class);
        } catch (Exception e) {
            source.sendMessage(MINI_MESSAGE.deserialize(Config.getInstance().getString("prefix") + " " + replacePlaceholders(Config.getInstance().getMessage("remove_incorrect_usage"), 
                    "$SOURCE", getSourceName(source))));
            return Command.SINGLE_SUCCESS;
        }

        if (!Whitelist.getInstance().contains(username)) {
            source.sendMessage(MINI_MESSAGE.deserialize(Config.getInstance().getString("prefix") + " " +
                            replacePlaceholders(Config.getInstance().getMessage("not_on_whitelist"), 
                                    "$SOURCE", getSourceName(source),
                                    "$PLAYER", username)
                    )
            );
            return Command.SINGLE_SUCCESS;
        }

        // remove and save the file
        Whitelist.getInstance().removePlayer(username);
        Whitelist.getInstance().saveFile();
        source.sendMessage(MINI_MESSAGE.deserialize(Config.getInstance().getString("prefix") + " " +
                        replacePlaceholders(Config.getInstance().getMessage("remove"), 
                                "$SOURCE", getSourceName(source),
                                "$PLAYER", username)
                )
        );

        return Command.SINGLE_SUCCESS;
    }

    public static int list(CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();

        if (Whitelist.getInstance().isWhitelistEmpty()) {
            source.sendMessage(MINI_MESSAGE.deserialize(Config.getInstance().getString("prefix") + " " + replacePlaceholders(Config.getInstance().getMessage("list_no_players"), 
                    "$SOURCE", getSourceName(source))));
            return Command.SINGLE_SUCCESS;
        }

        source.sendMessage(MINI_MESSAGE.deserialize(Config.getInstance().getString("prefix") + " " +
                        replacePlaceholders(Config.getInstance().getMessage("list"),
                                "$SOURCE", getSourceName(source),
                                "$WHITELIST_SIZE", String.valueOf(Whitelist.getInstance().getWhitelist().size()),
                                "$WHITELIST", String.join(", ", Whitelist.getInstance().getWhitelist())
                        )
                )
        );

        return Command.SINGLE_SUCCESS;
    }

    public static int clear(CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();

        if (!Config.getInstance().getBoolean("enable_clear_command")) {
            source.sendMessage(MINI_MESSAGE.deserialize(Config.getInstance().getString("prefix") + " " + replacePlaceholders(Config.getInstance().getMessage("clear_disabled"), 
                    "$SOURCE", getSourceName(source))));
            return Command.SINGLE_SUCCESS;
        }

        Whitelist.getInstance().clear();
        Whitelist.getInstance().saveFile();

        source.sendMessage(MINI_MESSAGE.deserialize(Config.getInstance().getString("prefix") + " " + replacePlaceholders(Config.getInstance().getMessage("clear"), 
                "$SOURCE", getSourceName(source))));
        return Command.SINGLE_SUCCESS;
    }

    public static int reload(CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();

        Config.getInstance().reload();
        Whitelist.getInstance().reload();
        
        // DiscordMessageManager теперь автоматически использует перезагруженный Config

        source.sendMessage(MINI_MESSAGE.deserialize(Config.getInstance().getString("prefix") + " " + replacePlaceholders(Config.getInstance().getMessage("reload"), 
                "$SOURCE", getSourceName(source))));
        return Command.SINGLE_SUCCESS;
    }

    private static String replacePlaceholders(String message, String... replacements) {
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }
        return message;
    }

    private static String getSourceName(CommandSource source) {
        if (source instanceof Player) {
            return ((Player) source).getUsername();
        } else {
            return "CONSOLE";
        }
    }
}