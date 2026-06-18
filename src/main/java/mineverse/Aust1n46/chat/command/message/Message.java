package mineverse.Aust1n46.chat.command.message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.clip.placeholderapi.PlaceholderAPI;
import mineverse.Aust1n46.chat.MineverseChat;
import mineverse.Aust1n46.chat.api.MineverseChatAPI;
import mineverse.Aust1n46.chat.api.MineverseChatPlayer;
import mineverse.Aust1n46.chat.localization.LocalizedMessage;
import mineverse.Aust1n46.chat.utilities.Format;

public class Message extends Command {
	public Message() {
		super("message");
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
			mcp.getPlayer().sendMessage(LocalizedMessage.COMMAND_INVALID_ARGUMENTS.toString().replace("{command}", "/" + command).replace("{args}", "[player] [message]"));
			return true;
		}

		MineverseChatPlayer player = MineverseChatAPI.getOnlineMineverseChatPlayer(args[0]);
		if (player == null) {
			mcp.getPlayer().sendMessage(LocalizedMessage.PLAYER_OFFLINE.toString().replace("{player}", args[0]));
			return true;
		}
		if (!mcp.getPlayer().canSee(player.getPlayer())) {
			mcp.getPlayer().sendMessage(LocalizedMessage.PLAYER_OFFLINE.toString().replace("{player}", args[0]));
			return true;
		}
		if (!mcp.getMessageToggle()){
			mcp.getPlayer().sendMessage(LocalizedMessage.MESSAGE_TOGGLED_PM.toString());
			return true;
		}
		if (mcp.getIgnores().contains(player.getUUID())){
			mcp.getPlayer().sendMessage(LocalizedMessage.IGNORE_MESSAGE_TO_IGNORED_PLAYER.toString());
			return true;
		}
		if (player.getIgnores().contains(mcp.getUUID())) {
			mcp.getPlayer().sendMessage(LocalizedMessage.IGNORING_MESSAGE.toString().replace("{player}", player.getName()));
			return true;
		}
		if (!player.getMessageToggle()) {
			mcp.getPlayer().sendMessage(LocalizedMessage.BLOCKING_MESSAGE.toString().replace("{player}", player.getName()));
			return true;
		}

		if (args.length >= 2) {
			String msg = "";
			String echo = "";
			String send = "";
			String spy = "";
			if (args[1].length() > 0) {
				for (int r = 1; r < args.length; r++) {
					msg += " " + args[r];
				}
				if (mcp.hasFilter()) {
					msg = Format.FilterChat(msg);
				}
				if (mcp.getPlayer().hasPermission("venturechat.color.legacy")) {
					msg = Format.FormatStringLegacyColor(msg);
				}
				if (mcp.getPlayer().hasPermission("venturechat.color")) {
					msg = Format.FormatStringColor(msg);
				}
				if (mcp.getPlayer().hasPermission("venturechat.format")) {
					msg = Format.FormatString(msg);
				}

				send = Format.FormatStringAll(PlaceholderAPI.setBracketPlaceholders(mcp.getPlayer(), plugin.getConfig().getString("tellformatfrom").replaceAll("sender_", "")));
				echo = Format.FormatStringAll(PlaceholderAPI.setBracketPlaceholders(mcp.getPlayer(), plugin.getConfig().getString("tellformatto").replaceAll("sender_", "")));
				spy = Format.FormatStringAll(PlaceholderAPI.setBracketPlaceholders(mcp.getPlayer(), plugin.getConfig().getString("tellformatspy").replaceAll("sender_", "")));

				send = Format.FormatStringAll(PlaceholderAPI.setBracketPlaceholders(player.getPlayer(), send.replaceAll("receiver_", ""))) + msg;
				echo = Format.FormatStringAll(PlaceholderAPI.setBracketPlaceholders(player.getPlayer(), echo.replaceAll("receiver_", ""))) + msg;
				spy = Format.FormatStringAll(PlaceholderAPI.setBracketPlaceholders(player.getPlayer(), spy.replaceAll("receiver_", ""))) + msg;

				player.setReplyPlayer(mcp.getUUID());
				mcp.setReplyPlayer(player.getUUID());
				player.getPlayer().sendMessage(send);
				mcp.getPlayer().sendMessage(echo);
				if (player.hasNotifications()) {
					Format.playMessageSound(player);
				}
				if (!mcp.getPlayer().hasPermission("venturechat.spy.override")) {
					for (MineverseChatPlayer sp : MineverseChatAPI.getOnlineMineverseChatPlayers()) {
						if (sp.getName().equals(mcp.getName()) || sp.getName().equals(player.getName())) {
							continue;
						}
						if (sp.isSpy()) {
							sp.getPlayer().sendMessage(spy);
						}
					}
				}
			}
		}
		if (args.length == 1) {
			if (plugin.getConfig().getBoolean("private_conversation_toggle", true)) {
				if (!mcp.hasConversation() || (mcp.hasConversation() && !mcp.getConversation().toString().equals(player.getUUID().toString()))) {
					mcp.setConversation(player.getUUID());
					if (!mcp.getPlayer().hasPermission("venturechat.spy.override")) {
						for (MineverseChatPlayer sp : MineverseChatAPI.getOnlineMineverseChatPlayers()) {
							if (sp.getName().equals(mcp.getName())) {
								continue;
							}
							if (sp.isSpy()) {
								sp.getPlayer().sendMessage(LocalizedMessage.ENTER_PRIVATE_CONVERSATION_SPY.toString().replace("{player_sender}", mcp.getName())
										.replace("{player_receiver}", player.getName()));
							}
						}
					}
					mcp.getPlayer().sendMessage(LocalizedMessage.ENTER_PRIVATE_CONVERSATION.toString().replace("{player_receiver}", player.getName()));
				} else {
					mcp.setConversation(null);
					if (!mcp.getPlayer().hasPermission("venturechat.spy.override")) {
						for (MineverseChatPlayer sp : MineverseChatAPI.getOnlineMineverseChatPlayers()) {
							if (sp.getName().equals(mcp.getName())) {
								continue;
							}
							if (sp.isSpy()) {
								sp.getPlayer().sendMessage(LocalizedMessage.EXIT_PRIVATE_CONVERSATION_SPY.toString().replace("{player_sender}", mcp.getName())
										.replace("{player_receiver}", player.getName()));
							}
						}
					}
					mcp.getPlayer().sendMessage(LocalizedMessage.EXIT_PRIVATE_CONVERSATION.toString().replace("{player_receiver}", player.getName()));
				}
			} else {
				mcp.getPlayer().sendMessage(LocalizedMessage.COMMAND_INVALID_ARGUMENTS.toString().replace("{command}", "/msg").replace("{args}", "[player] [message]"));
			}
		}
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
