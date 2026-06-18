package mineverse.Aust1n46.chat.api;

import mineverse.Aust1n46.chat.MineverseChat;
import mineverse.Aust1n46.chat.localization.LocalizedMessage;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class ChatGroup {
    private String ownerName;
    private List<MineverseChatPlayer> playerList = new ArrayList<>();
    private final Map<UUID, BukkitTask> groupInviteTasks = new HashMap<>();

    public ChatGroup(MineverseChatPlayer owner){
        this.ownerName = owner.getName();
        playerList.add(owner);
    }

    public String getGroupName(){ return this.ownerName; }

    public List<MineverseChatPlayer> getPlayerList(){
        return playerList;
    }

    private List<String> getPlayerListNames(){
        List<String> members = new ArrayList<>();
        for(MineverseChatPlayer player : playerList){
            members.add(player.getName());
        }
        return members;
    }

    public String getPlayerListString(){
        return String.join(", ", getPlayerListNames());
    }

    public void invitePlayer(MineverseChatPlayer player){
        if(player == null || !player.isOnline()){
            return;
        }
        BukkitTask task = Bukkit.getScheduler().runTaskLater(MineverseChat.getInstance(), () -> {
            removeInvite(player.getUUID());
        }, 600L);

        groupInviteTasks.put(player.getPlayer().getUniqueId(), task);
    }

    public boolean inviteExists(MineverseChatPlayer player){
        return groupInviteTasks.containsKey(player.getPlayer().getUniqueId());
    }

    private void removeInvite(UUID uuid){
        BukkitTask task = groupInviteTasks.remove(uuid);
        if(task != null && !task.isCancelled()){
            task.cancel();
        }
    }

    public void clearInvites() {
        for (BukkitTask task : groupInviteTasks.values()) {
            task.cancel();
        }
        groupInviteTasks.clear();
    }

    public void addPlayer(MineverseChatPlayer player){
        removeInvite(player.getUUID());
        playerList.add(player);
        sendWelcomeToAllMembers(player);
    }

    public void removePlayer(MineverseChatPlayer player){
        sendLeftPlayerToAllMembers(player);
        playerList.remove(player);
    }

    private void sendLeftPlayerToAllMembers(MineverseChatPlayer leftPlayer){
        for(MineverseChatPlayer player : playerList){
            if(!player.isOnline()) continue;
            player.getPlayer().sendMessage(LocalizedMessage.GROUP_PLAYER_LEFT.toString().replace("{player}", leftPlayer.getName()));
        }
    }

    public void sendDisbandMessageToAllMembers(){
        for(MineverseChatPlayer player : playerList){
            if(!player.isOnline()) continue;
            player.getPlayer().sendMessage(LocalizedMessage.GROUP_DISBANDED_MEMBERS.toString().replace("{player}", ownerName));
        }
    }

    private void sendWelcomeToAllMembers(MineverseChatPlayer newPlayer){
        for(MineverseChatPlayer player : playerList){
            if(!player.isOnline()) continue;
            player.getPlayer().sendMessage(LocalizedMessage.GROUP_NEW_MEMBER.toString().replace("{player}", newPlayer.getName()));
        }
    }

    public boolean isOwner(MineverseChatPlayer player){
        return player.getPlayer().getName().equals(ownerName);
    }

    public boolean isEqual(ChatGroup group){
        if(group.getGroupName().equals(ownerName)){
            return true;
        }
        return  false;
    }


}
