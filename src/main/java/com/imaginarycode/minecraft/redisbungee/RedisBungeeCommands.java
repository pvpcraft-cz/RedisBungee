package com.imaginarycode.minecraft.redisbungee;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Command;

import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * This class contains subclasses that are used for the commands RedisBungee overrides or includes: /glist, /find and /lastseen.
 * <p>
 * All classes use the {@link RedisBungeeAPI}.
 *
 * @author tuxed
 * @since 0.2.3
 */
class RedisBungeeCommands {

    private static final BaseComponent[] NO_PLAYER_SPECIFIED =
            new ComponentBuilder("You must specify a player name.").color(ChatColor.RED).create();
    private static final BaseComponent[] PLAYER_NOT_FOUND =
            new ComponentBuilder("No such player found.").color(ChatColor.RED).create();
    private static final BaseComponent[] NO_COMMAND_SPECIFIED =
            new ComponentBuilder("You must specify a command to be run.").color(ChatColor.RED).create();

    private static String playerPlural(int num) {
        return num == 1 ? num + " player is" : num + " players are";
    }

    public static void send(CommandSender sender, String str) {
        if (!Strings.isNullOrEmpty(str))
            sender.sendMessage(format(str));
    }

    public static TextComponent format(String str) {
        return new TextComponent(color(str));
    }

    public static String color(String str) {
        return Strings.isNullOrEmpty(str) ? "" : ChatColor.translateAlternateColorCodes('&', str);
    }

    public static class GlistCommand extends Command {

        private final RedisBungee plugin;

        GlistCommand(RedisBungee plugin) {
            super("glist", "bungeecord.command.list", "redisbungee", "rglist");
            this.plugin = plugin;
        }

        @Override
        public void execute(final CommandSender sender, final String[] args) {
            plugin.getProxy().getScheduler().runAsync(plugin, new Runnable() {
                @Override
                public void run() {
                    int count = RedisBungee.getApi().getPlayerCount();

                    Multimap<String, UUID> serverToPlayers = RedisBungee.getApi().getServerToPlayers();
                    Multimap<String, String> human = HashMultimap.create();

                    for (Map.Entry<String, UUID> entry : serverToPlayers.entries()) {
                        human.put(entry.getKey(), plugin.getUuidTranslator().getNameFromUuid(entry.getValue(), false));
                    }

                    for (String server : new TreeSet<>(serverToPlayers.keySet())) {
                        send(sender, "&a[" + server + "]&e(" + serverToPlayers.get(server).size() + "): &f"
                                + Joiner.on(", ").join(human.get(server)));
                    }
                    send(sender, "&e" + playerPlural(count) + " currently online.");
                }
            });
        }
    }

    public static class FindCommand extends Command {
        private final RedisBungee plugin;

        FindCommand(RedisBungee plugin) {
            super("find", "bungeecord.command.find", "rfind");
            this.plugin = plugin;
        }

        @Override
        public void execute(final CommandSender sender, final String[] args) {
            plugin.getProxy().getScheduler().runAsync(plugin, new Runnable() {
                @Override
                public void run() {
                    if (args.length > 0) {
                        UUID uuid = plugin.getUuidTranslator().getTranslatedUuid(args[0], true);
                        if (uuid == null) {
                            sender.sendMessage(PLAYER_NOT_FOUND);
                            return;
                        }

                        ServerInfo si = RedisBungee.getApi().getServerFor(uuid);
                        if (si != null) {
                            send(sender, "&3" + args[0] + " is on " + si.getName() + ".");
                        } else {
                            sender.sendMessage(PLAYER_NOT_FOUND);
                        }
                    } else {
                        sender.sendMessage(NO_PLAYER_SPECIFIED);
                    }
                }
            });
        }
    }

    public static class LastSeenCommand extends Command {
        private final RedisBungee plugin;

        LastSeenCommand(RedisBungee plugin) {
            super("lastseen", "redisbungee.command.lastseen", "rlastseen");
            this.plugin = plugin;
        }

        @Override
        public void execute(final CommandSender sender, final String[] args) {
            plugin.getProxy().getScheduler().runAsync(plugin, new Runnable() {
                @Override
                public void run() {
                    if (args.length > 0) {
                        UUID uuid = plugin.getUuidTranslator().getTranslatedUuid(args[0], true);
                        if (uuid == null) {
                            sender.sendMessage(PLAYER_NOT_FOUND);
                            return;
                        }
                        long secs = RedisBungee.getApi().getLastOnline(uuid);
                        TextComponent message = new TextComponent();
                        if (secs == 0) {
                            message.setColor(ChatColor.GREEN);
                            message.setText(args[0] + " is currently online.");
                        } else if (secs != -1) {
                            message.setColor(ChatColor.BLUE);
                            message.setText(args[0] + " was last online on " + new SimpleDateFormat().format(secs) + ".");
                        } else {
                            message.setColor(ChatColor.RED);
                            message.setText(args[0] + " has never been online.");
                        }
                        sender.sendMessage(message);
                    } else {
                        sender.sendMessage(NO_PLAYER_SPECIFIED);
                    }
                }
            });
        }
    }

    public static class IpCommand extends Command {
        private final RedisBungee plugin;

        IpCommand(RedisBungee plugin) {
            super("ip", "redisbungee.command.ip", "playerip", "rip", "rplayerip");
            this.plugin = plugin;
        }

        @Override
        public void execute(final CommandSender sender, final String[] args) {
            plugin.getProxy().getScheduler().runAsync(plugin, new Runnable() {
                @Override
                public void run() {
                    if (args.length > 0) {
                        UUID uuid = plugin.getUuidTranslator().getTranslatedUuid(args[0], true);
                        if (uuid == null) {
                            sender.sendMessage(PLAYER_NOT_FOUND);
                            return;
                        }

                        InetAddress ia = RedisBungee.getApi().getPlayerIp(uuid);

                        if (ia != null) {
                            send(sender, "&a" + args[0] + " is connected from " + ia.toString() + ".");
                        } else {
                            sender.sendMessage(PLAYER_NOT_FOUND);
                        }
                    } else {
                        sender.sendMessage(NO_PLAYER_SPECIFIED);
                    }
                }
            });
        }
    }

    public static class PlayerProxyCommand extends Command {
        private final RedisBungee plugin;

        PlayerProxyCommand(RedisBungee plugin) {
            super("pproxy", "redisbungee.command.pproxy");
            this.plugin = plugin;
        }

        @Override
        public void execute(final CommandSender sender, final String[] args) {
            plugin.getProxy().getScheduler().runAsync(plugin, new Runnable() {
                @Override
                public void run() {
                    if (args.length > 0) {
                        UUID uuid = plugin.getUuidTranslator().getTranslatedUuid(args[0], true);
                        if (uuid == null) {
                            sender.sendMessage(PLAYER_NOT_FOUND);
                            return;
                        }
                        String proxy = RedisBungee.getApi().getProxy(uuid);
                        if (proxy != null) {
                            send(sender, "&a" + args[0] + " is connected to " + proxy + ".");
                        } else {
                            sender.sendMessage(PLAYER_NOT_FOUND);
                        }
                    } else {
                        sender.sendMessage(NO_PLAYER_SPECIFIED);
                    }
                }
            });
        }
    }

    public static class SendToAll extends Command {
        private final RedisBungee plugin;

        SendToAll(RedisBungee plugin) {
            super("sendtoall", "redisbungee.command.sendtoall", "rsendtoall");
            this.plugin = plugin;
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length > 0) {
                String command = Joiner.on(" ").skipNulls().join(args);
                RedisBungee.getApi().sendProxyCommand(command);
                send(sender, "&aSent the command /" + command + " to all proxies.");
            } else {
                sender.sendMessage(NO_COMMAND_SPECIFIED);
            }
        }
    }

    public static class ServerId extends Command {
        private final RedisBungee plugin;

        ServerId(RedisBungee plugin) {
            super("serverid", "redisbungee.command.serverid", "rserverid");
            this.plugin = plugin;
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            send(sender, "&eYou are on " + RedisBungee.getApi().getServerId() + ".");
        }
    }

    public static class ServerIds extends Command {
        public ServerIds() {
            super("serverids", "redisbungee.command.serverids", "servers");
        }

        @Override
        public void execute(CommandSender sender, String[] strings) {
            send(sender, "&eServers: " + Joiner.on(", ").join(RedisBungee.getApi().getAllServers()));
        }
    }

    public static class PlistCommand extends Command {
        private final RedisBungee plugin;

        PlistCommand(RedisBungee plugin) {
            super("plist", "redisbungee.command.plist", "rplist");
            this.plugin = plugin;
        }

        @Override
        public void execute(final CommandSender sender, final String[] args) {
            plugin.getProxy().getScheduler().runAsync(plugin, new Runnable() {
                @Override
                public void run() {
                    String proxy = args.length >= 1 ? args[0] : RedisBungee.getConfiguration().getServerId();

                    if (!plugin.getServerIds().contains(proxy)) {
                        send(sender, "&c" + proxy + " is not a valid proxy. See /serverids for valid proxies.");
                        return;
                    }

                    Set<UUID> players = RedisBungee.getApi().getPlayersOnProxy(proxy);

                    Multimap<String, UUID> serverToPlayers = RedisBungee.getApi().getServerToPlayers();
                    Multimap<String, String> human = HashMultimap.create();

                    for (Map.Entry<String, UUID> entry : serverToPlayers.entries()) {
                        if (players.contains(entry.getValue())) {
                            human.put(entry.getKey(), plugin.getUuidTranslator().getNameFromUuid(entry.getValue(), false));
                        }
                    }

                    for (String server : new TreeSet<>(serverToPlayers.keySet())) {
                        send(sender, "&c[" + server + "]&e(" + serverToPlayers.get(server).size() + "): &f"
                                + Joiner.on(", ").join(human.get(server)));
                    }
                    send(sender, "&e" + playerPlural(players.size()) + " currently on proxy " + proxy + ".");
                }
            });
        }
    }

    public static class DebugCommand extends Command {
        private final RedisBungee plugin;

        DebugCommand(RedisBungee plugin) {
            super("rdebug", "redisbungee.command.debug");
            this.plugin = plugin;
        }

        @Override
        public void execute(final CommandSender sender, final String[] args) {
            send(sender, "Currently active pool objects: " + plugin.getPool().getNumActive());
            send(sender, "Currently idle pool objects: " + plugin.getPool().getNumIdle());
            send(sender, "Waiting on free objects: " + plugin.getPool().getNumWaiters());
        }
    }
}
