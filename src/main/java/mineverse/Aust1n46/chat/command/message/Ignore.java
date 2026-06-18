package mineverse.Aust1n46.chat.command.message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mineverse.Aust1n46.chat.MineverseChat;
import mineverse.Aust1n46.chat.api.MineverseChatAPI;
import mineverse.Aust1n46.chat.api.MineverseChatPlayer;
import mineverse.Aust1n46.chat.localization.LocalizedMessage;

public class Ignore extends Command {
	public Ignore() {
		super("ignore");
	}

	private MineverseChat plugin = MineverseChat.getInstance();

	@Override
	public boolean execute(CommandSender sender, String command, String[] args) {
		if (!(sender instanceof Player)) {
			plugin.getServer().getConsoleSender().sendMessage(LocalizedMessage.COMMAND_MUST_BE_RUN_BY_PLAYER.toString());
			return true;
		}
		MineverseChatPlayer mcp = MineverseChatAPI.getOnlineMineverseChatPlayer((Player) sender);
		if (args.length == 0) {
			mcp.getPlayer().sendMessage(LocalizedMessage.COMMAND_INVALID_ARGUMENTS_IGNORE.toString());
			return true;
		}
		if (args[0].equalsIgnoreCase("list")) {
			String ignoreList = "";
			for (UUID ignore : mcp.getIgnores()) {
				MineverseChatPlayer i = MineverseChatAPI.getMineverseChatPlayer(ignore);
				String iName = ignore.toString();
				if (i != null) {
					iName = i.getName();
				}
				ignoreList += ChatColor.RED + iName + ChatColor.WHITE + ", ";
			}
			mcp.getPlayer().sendMessage(LocalizedMessage.IGNORE_LIST_HEADER.toString());
			if (ignoreList.length() > 0) {
				mcp.getPlayer().sendMessage(ignoreList.substring(0, ignoreList.length() - 2));
			}
			return true;
		}
		if (mcp.getName().equalsIgnoreCase(args[0])) {
			mcp.getPlayer().sendMessage(LocalizedMessage.IGNORE_YOURSELF.toString());
			return true;
		}
		MineverseChatPlayer player = MineverseChatAPI.getOnlineMineverseChatPlayer(args[0]);
		if (player == null) {
			mcp.getPlayer().sendMessage(LocalizedMessage.PLAYER_OFFLINE.toString().replace("{player}", args[0]));
			return true;
		}
		if (mcp.getIgnores().contains(player.getUUID())) {
			mcp.getPlayer().sendMessage(LocalizedMessage.IGNORE_PLAYER_OFF.toString().replace("{player}", player.getName()));
			mcp.removeIgnore(player.getUUID());
			return true;
		}
		if (player.getPlayer().hasPermission("venturechat.ignore.bypass")) {
			mcp.getPlayer().sendMessage(LocalizedMessage.IGNORE_PLAYER_CANT.toString().replace("{player}", player.getName()));
			return true;
		}
		mcp.getPlayer().sendMessage(LocalizedMessage.IGNORE_PLAYER_ON.toString().replace("{player}", player.getName()));
		mcp.addIgnore(player.getUUID());
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String label, String[] args) {
		if (args.length == 1) {
			String prefix = args[0];
			List<String> completions = new ArrayList<>();
			for (Player player : Bukkit.getOnlinePlayers()) {
				if (sender instanceof Player && !((Player) sender).canSee(player)) {
					continue;
				}
				if (startsWithIgnoreCase(player.getName(), prefix)) {
					completions.add(player.getName());
				}
			}
			Collections.sort(completions);
			return completions;
		}
		return Collections.emptyList();
	}

	private boolean startsWithIgnoreCase(String value, String prefix) {
		return value.regionMatches(true, 0, prefix, 0, prefix.length());
	}
}
