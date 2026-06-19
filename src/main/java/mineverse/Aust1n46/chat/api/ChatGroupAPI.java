package mineverse.Aust1n46.chat.api;

import java.util.*;

public class ChatGroupAPI {
    private final static HashMap<String, ChatGroup> userGroups = new HashMap<>();

    public static boolean hasGroup(MineverseChatPlayer player){
        String name = player.getName();
        return userGroups.containsKey(name);
    }

    public static Set<String> getPlayerSet(){
        return userGroups.keySet();
    }

    public static ChatGroup getGroup(MineverseChatPlayer player){
        if(!hasGroup(player)){
            return null;
        }
        return userGroups.get(player.getName());
    }

    public static void disbandGroup(ChatGroup group){
        if(group == null) return;
        List<MineverseChatPlayer> members = new ArrayList<>(group.getPlayerList());
        for(MineverseChatPlayer player : members){
            userGroups.remove(player.getName());
        }
        group.clearInvites();
    }

    public static void addPlayerToGroup(ChatGroup group, MineverseChatPlayer mcp){
        group.addPlayer(mcp);
        userGroups.put(mcp.getName(), group);
    }

    public static void removePlayerFromGroup(ChatGroup group, MineverseChatPlayer mcp){
        group.removePlayer(mcp);
        userGroups.remove(mcp.getName());
    }

    public static void disbandGroup(String player){
        ChatGroup group = userGroups.get(player);
        disbandGroup(group);
    }

    public static ChatGroup createGroup(MineverseChatPlayer owner){
        ChatGroup group = new ChatGroup(owner);
        userGroups.put(owner.getName(), group);
        return group;
    }
}
