package mineverse.Aust1n46.chat.command.chat;

import mineverse.Aust1n46.chat.api.ChatGroup;
import mineverse.Aust1n46.chat.api.ChatGroupAPI;
import mineverse.Aust1n46.chat.localization.LocalizedMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mineverse.Aust1n46.chat.MineverseChat;
import mineverse.Aust1n46.chat.api.MineverseChatAPI;
import mineverse.Aust1n46.chat.api.MineverseChatPlayer;

import java.util.*;
import java.util.stream.Collectors;

public class Group extends Command {
    private MineverseChat plugin = MineverseChat.getInstance();

    private static final Set<String> FIRST_ARGUMENT = Set.of(
            "create", "invite", "accept", "leave", "kick", "help", "info");

    private static final Set<String> SECOND_ARGUMENT = Set.of("invite", "accept", "kick", "info");

    public Group() {
        super("chatgroup");
    }

    @Override
    public boolean execute(CommandSender sender, String command, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getServer().getConsoleSender().sendMessage(ChatColor.RED + "This command must be run by a player.");
            return true;
        }
        MineverseChatPlayer mcp = MineverseChatAPI.getOnlineMineverseChatPlayer((Player) sender);
        ChatGroup mcpChatGroup = ChatGroupAPI.getGroup(mcp);
        if (!mcp.getPlayer().hasPermission("venturechat.group")) {
            mcp.getPlayer().sendMessage(LocalizedMessage.NO_PERMISSION.toString());
            return true;
        }
        try {
            switch (args[0].toLowerCase()) {
                case "create": {
                    if (!mcp.getPlayer().hasPermission("venturechat.group.create")) {
                        mcp.getPlayer().sendMessage(LocalizedMessage.NO_PERMISSION.toString());
                        return true;
                    }

                    if (mcpChatGroup != null) {
                        mcp.getPlayer().sendMessage(LocalizedMessage.GROUP_ALREADY_IN.toString().replace("{player}", mcpChatGroup.getGroupName()));
                        return true;
                    }

                    ChatGroupAPI.createGroup(mcp);
                    mcp.getPlayer().sendMessage(LocalizedMessage.GROUP_CREATED.toString());
                    return true;
                }
                case "invite": {
                    if (!mcp.getPlayer().hasPermission("venturechat.group.invite")) {
                        mcp.getPlayer().sendMessage(LocalizedMessage.NO_PERMISSION.toString());
                        return true;
                    }
                    if (args.length < 2) {
                        mcp.getPlayer().sendMessage(LocalizedMessage.COMMAND_INVALID_ARGUMENTS
                                .toString()
                                .replace("{command}", "/chatgroup invite")
                                .replace("{args}", "[nickname]"));
                        return true;
                    }

                    MineverseChatPlayer targetPlayer = MineverseChatAPI.getMineverseChatPlayer(args[1]);
                    if(targetPlayer == null || !targetPlayer.isOnline()){
                        mcp.getPlayer().sendMessage(LocalizedMessage.PLAYER_OFFLINE.toString().replace("{player}", args[1]));
                        return true;
                    }

                    if (targetPlayer.equals(mcp)) {
                        mcp.getPlayer().sendMessage(LocalizedMessage.GROUP_INVITE_YOURSELF.toString());
                        return true;
                    }

                    ChatGroup targetGroup = ChatGroupAPI.getGroup(targetPlayer);
                    if (targetGroup != null) {
                        if (mcpChatGroup != null && mcpChatGroup.isEqual(targetGroup)) {
                            mcp.getPlayer().sendMessage(LocalizedMessage.GROUP_INVITE_ALREADY_MEMBER.toString()
                                    .replace("{player}", targetPlayer.getName()));
                        } else {
                            mcp.getPlayer().sendMessage(LocalizedMessage.GROUP_INVITE_ALREADY_MEMBER_ANOTHER.toString()
                                    .replace("{player}", targetPlayer.getName()));
                        }
                        return true;
                    }

                    if (mcpChatGroup == null) {
                        mcp.getPlayer().sendMessage(LocalizedMessage.GROUP_CREATED.toString());
                        mcpChatGroup = ChatGroupAPI.createGroup(mcp);
                    } else {
                        if (!mcpChatGroup.isOwner(mcp)) {
                            mcp.getPlayer().sendMessage(LocalizedMessage.GROUP_NOT_OWNER.toString());
                            return true;
                        }
                    }

                    if (mcpChatGroup.inviteExists(targetPlayer)) {
                        mcp.getPlayer().sendMessage(LocalizedMessage.GROUP_INVITE_ALREADY_EXISTS.toString()
                                .replace("{player}", targetPlayer.getName()));
                        return true;
                    }

                    mcp.getPlayer().sendMessage(LocalizedMessage.GROUP_INVITE_SUCCESS.toString()
                            .replace("{player}", targetPlayer.getName()));

                    targetPlayer.getPlayer().sendMessage(LocalizedMessage.GROUP_INVITED_BY_PLAYER.toString()
                            .replace("{player}", mcp.getName()));

                    mcpChatGroup.invitePlayer(targetPlayer);

                    return true;
                }
                case "accept": {
                    if (!mcp.getPlayer().hasPermission("venturechat.group.accept")) {
                        mcp.getPlayer().sendMessage(LocalizedMessage.NO_PERMISSION.toString());
                        return true;
                    }
                    if (args.length < 2) {
                        mcp.getPlayer().sendMessage(LocalizedMessage.COMMAND_INVALID_ARGUMENTS
                                .toString()
                                .replace("{command}", "/chatgroup accept")
                                .replace("{args}", "[nickname]"));
                        return true;
                    }

                    if (mcpChatGroup != null) {
                        mcp.getPlayer().sendMessage(LocalizedMessage.GROUP_ALREADY_IN.toString().replace("{player}", mcpChatGroup.getGroupName()));
                        return true;
                    }

                    MineverseChatPlayer senderMCP = MineverseChatAPI.getMineverseChatPlayer(args[1]);
                    if (senderMCP == null ) {
                        mcp.getPlayer().sendMessage(LocalizedMessage.PLAYER_NOT_EXIST.toString().replace("{player}", args[1]));
                        return true;
                    }

                    ChatGroup senderGroup = ChatGroupAPI.getGroup(senderMCP);
                    if (senderGroup == null || !senderGroup.inviteExists(mcp)) {
                        mcp.getPlayer().sendMessage(LocalizedMessage.GROUP_NO_INVITE_GROM_PLAYER.toString().replace("{player}", args[1]));
                        return true;
                    }

                    ChatGroupAPI.addPlayerToGroup(senderGroup, mcp);
                    return true;
                }
                case "leave": {
                    if (!mcp.getPlayer().hasPermission("venturechat.group.leave")) {
                        mcp.getPlayer().sendMessage(LocalizedMessage.NO_PERMISSION.toString());
                        return true;
                    }
                    if (mcpChatGroup == null) {
                        mcp.getPlayer().sendMessage(LocalizedMessage.GROUP_NOT_MEMBER.toString());
                        return true;
                    }

                    if (mcpChatGroup.isOwner(mcp)) {
                        mcpChatGroup.sendDisbandMessageToAllMembers();
                        ChatGroupAPI.disbandGroup(mcp.getName());
                        return true;
                    }

                    ChatGroupAPI.removePlayerFromGroup(mcpChatGroup, mcp);
                    return true;
                }
                case "kick": {
                    if (!mcp.getPlayer().hasPermission("venturechat.group.kick")) {
                        mcp.getPlayer().sendMessage(LocalizedMessage.NO_PERMISSION.toString());
                        return true;
                    }

                    if (args.length < 2) {
                        mcp.getPlayer().sendMessage(
                                LocalizedMessage.COMMAND_INVALID_ARGUMENTS
                                        .toString()
                                        .replace("{command}", "/chatgroup kick")
                                        .replace("{args}", "[nickname]"));
                        return true;
                    }

                    if (mcpChatGroup == null) {
                        mcp.getPlayer().sendMessage(LocalizedMessage.GROUP_NOT_MEMBER.toString());
                        return true;
                    }

                    if (!mcpChatGroup.isOwner(mcp)) {
                        mcp.getPlayer().sendMessage(LocalizedMessage.GROUP_NOT_OWNER.toString());
                        return true;
                    }

                    MineverseChatPlayer targetPlayer = MineverseChatAPI.getMineverseChatPlayer(args[1]);

                    if(targetPlayer == null){
                        mcp.getPlayer().sendMessage(LocalizedMessage.GROUP_KICK_NOT_MEMBER.toString()
                                .replace("{player}", args[1]));
                        return true;
                    }

                    if (targetPlayer.equals(mcp)) {
                        mcp.getPlayer().sendMessage(LocalizedMessage.GROUP_KICK_YOURSELF.toString());
                        return true;
                    }

                    ChatGroup targetGroup = ChatGroupAPI.getGroup(targetPlayer);

                    if (targetGroup == null || !targetGroup.isEqual(mcpChatGroup)) {
                        mcp.getPlayer().sendMessage(LocalizedMessage.GROUP_KICK_NOT_MEMBER.toString()
                                .replace("{player}", targetPlayer.getName()));
                        return true;
                    }

                    ChatGroupAPI.removePlayerFromGroup(mcpChatGroup, targetPlayer);

                    return true;
                }
                case "info": {
                    if (args.length < 2) {
                        if (!mcp.getPlayer().hasPermission("venturechat.group.info")) {
                            mcp.getPlayer().sendMessage(LocalizedMessage.NO_PERMISSION.toString());
                            return true;
                        }
                        if (mcpChatGroup == null) {
                            mcp.getPlayer().sendMessage(LocalizedMessage.GROUP_NOT_MEMBER.toString());
                            return true;
                        }

                        mcp.getPlayer().sendMessage(LocalizedMessage.GROUP_INFORMATION.toString()
                                .replace("{owner}", mcpChatGroup.getGroupName())
                                .replace("{members}", mcpChatGroup.getPlayerListString()));

                        return true;
                    }

                    if (!mcp.getPlayer().hasPermission("venturechat.group.info.others")) {
                        mcp.getPlayer().sendMessage(LocalizedMessage.NO_PERMISSION.toString());
                        return true;
                    }

                    MineverseChatPlayer targetPlayer = MineverseChatAPI.getOnlineMineverseChatPlayer(args[1]);
                    if (targetPlayer == null || !targetPlayer.isOnline()) {
                        mcp.getPlayer().sendMessage(LocalizedMessage.PLAYER_OFFLINE.toString().replace("{player}", args[1]));
                        return true;
                    }

                    ChatGroup targetGroup = ChatGroupAPI.getGroup(targetPlayer);
                    if (targetGroup == null) {
                        mcp.getPlayer().sendMessage(LocalizedMessage.GROUP_PLAYER_NOT_A_GROUP_MEMBER.toString()
                                .replace("{player}", targetPlayer.getName()));
                        return true;
                    }

                    mcp.getPlayer().sendMessage(LocalizedMessage.GROUP_INFORMATION.toString()
                            .replace("{player}", targetGroup.getGroupName())
                            .replace("{members}", targetGroup.getPlayerListString()));

                    return true;
                }
                case "chat":{
                    return true;
                }
                case "help": {
                    if (!mcp.getPlayer().hasPermission("venturechat.group.help")) {
                        mcp.getPlayer().sendMessage(LocalizedMessage.NO_PERMISSION.toString());
                        return true;
                    }

                    mcp.getPlayer().sendMessage(ChatColor.GREEN + "Future help message");
                    return true;
                }
            }
        } catch (Exception e) {
            mcp.getPlayer().sendMessage(ChatColor.RED + "Invalid arguments, /chatgroup help");
        }
        return true;
    }


    @Override
    public List<String> tabComplete(CommandSender sender, String label, String[] args) {
        switch(args.length){
            case 1: {
                return FIRST_ARGUMENT.stream()
                        .filter(cmd -> startsWithIgnoreCase(cmd, args[0]))
                        .collect(Collectors.toList());
            }

            case 2: {
                if (!SECOND_ARGUMENT.contains(args[0].toLowerCase())) {
                    return Collections.emptyList();
                }

                return MineverseChatAPI.getOnlineMineverseChatPlayers().stream()
                        .map(MineverseChatPlayer::getName)
                        .filter(name -> startsWithIgnoreCase(name, args[1]))
                        .toList();
            }

            default: return Collections.emptyList();
        }
    }


    private boolean startsWithIgnoreCase(String value, String prefix) {
        return value.regionMatches(true, 0, prefix, 0, prefix.length());
    }
}
