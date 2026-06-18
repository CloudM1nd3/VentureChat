package mineverse.Aust1n46.chat.api;

import java.util.*;

public class ChatGroupAPI {
    private final static HashMap<UUID, ChatGroup> userGroups = new HashMap<>();

    public static boolean hasGroup(MineverseChatPlayer player){
        UUID uuid = player.getUUID();
        return userGroups.containsKey(uuid);
    }

    public static ChatGroup getGroup(MineverseChatPlayer player){
        if(!hasGroup(player)){
            return null;
        }
        return userGroups.get(player.getUUID());
    }

    public static void disbandGroup(ChatGroup group){
        if(group == null) return;
        List<MineverseChatPlayer> members = new ArrayList<>(group.getPlayerList());
        for(MineverseChatPlayer player : members){
            userGroups.remove(player.getUUID());
        }
        group.clearInvites();
    }

    public static void addPlayerToGroup(ChatGroup group, MineverseChatPlayer mcp){
        group.addPlayer(mcp);
        userGroups.put(mcp.getUUID(), group);
    }

    public static void removePlayerFromGroup(ChatGroup group, MineverseChatPlayer mcp){
        group.removePlayer(mcp);
        userGroups.remove(mcp.getUUID());
    }

    public static void disbandGroup(UUID uuid){
        ChatGroup group = userGroups.get(uuid);
        disbandGroup(group);
    }

    public static ChatGroup createGroup(MineverseChatPlayer owner){
        ChatGroup group = new ChatGroup(owner);
        userGroups.put(owner.getUUID(), group);
        return group;
    }
}
