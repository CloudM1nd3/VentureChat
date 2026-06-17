package mineverse.Aust1n46.chat.listeners;

import java.util.Set;

import net.essentialsx.api.v2.services.discord.DiscordService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.PluginManager;

import com.comphenix.protocol.events.PacketContainer;
import com.massivecraft.factions.entity.MPlayer;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;

import me.clip.placeholderapi.PlaceholderAPI;
import mineverse.Aust1n46.chat.MineverseChat;
import mineverse.Aust1n46.chat.api.MineverseChatAPI;
import mineverse.Aust1n46.chat.api.MineverseChatPlayer;
import mineverse.Aust1n46.chat.api.events.VentureChatEvent;
import mineverse.Aust1n46.chat.channel.ChatChannel;
import mineverse.Aust1n46.chat.database.Database;
import mineverse.Aust1n46.chat.localization.LocalizedMessage;
import mineverse.Aust1n46.chat.utilities.Format;

//This class listens to chat through the chat event and handles the bulk of the chat channels and formatting.
public class ChatListener implements Listener {
	private final boolean essentialsDiscordHook = Bukkit.getPluginManager().isPluginEnabled("EssentialsDiscord");
	private MineverseChat plugin = MineverseChat.getInstance();

	// this event isn't always asynchronous even though the event's name starts with "Async"
    // blame md_5 for that one
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onAsyncPlayerChatEvent(AsyncPlayerChatEvent event) {
		event.setCancelled(true);
		Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				handleTrueAsyncPlayerChatEvent(event);
			}
		});
	}
	
	public void handleTrueAsyncPlayerChatEvent(AsyncPlayerChatEvent event) {
		String chat = event.getMessage();
		String format;
		Set<Player> recipients = event.getRecipients();
		int recipientCount = recipients.size(); // Don't count vanished players
		MineverseChatPlayer mcp = MineverseChatAPI.getOnlineMineverseChatPlayer(event.getPlayer());
		ChatChannel eventChannel = ChatChannel.getDefaultChannel();
		for (ChatChannel channel : ChatChannel.getChatChannels()){
			if(!channel.getQuickSymbol().equals("None")){
				if(chat.startsWith(channel.getQuickSymbol())){
					eventChannel = channel;
					chat = chat.substring(eventChannel.getQuickSymbol().length());
					break;
				}
			}
		}
		chat = " " + chat.stripLeading();

		if(mcp.isEditing()) {
			mcp.getPlayer().sendMessage(Format.FormatStringAll(chat));
			mcp.setEditing(false);
			return;
		}

		if(mcp.hasConversation()) {
			MineverseChatPlayer tp = MineverseChatAPI.getMineverseChatPlayer(mcp.getConversation());
			if(!tp.isOnline()) {
				mcp.getPlayer().sendMessage(ChatColor.RED + tp.getName() + " is not available.");
				if(!mcp.getPlayer().hasPermission("venturechat.spy.override")) {
					for(MineverseChatPlayer p : MineverseChatAPI.getOnlineMineverseChatPlayers()) {
						if(p.getName().equals(mcp.getName())) {
							continue;
						}
						if(p.isSpy()) {
							p.getPlayer().sendMessage(LocalizedMessage.EXIT_PRIVATE_CONVERSATION_SPY.toString()
									.replace("{player_sender}", mcp.getName())
									.replace("{player_receiver}", tp.getName()));
						}
					}
				}
				mcp.setConversation(null);
			}
			else {
				if(tp.getIgnores().contains(mcp.getUUID())) {
					mcp.getPlayer().sendMessage(LocalizedMessage.IGNORING_MESSAGE.toString()
							.replace("{player}", tp.getName()));
					event.setCancelled(true);
					return;
				}
				if(!tp.getMessageToggle()) {
					mcp.getPlayer().sendMessage(LocalizedMessage.BLOCKING_MESSAGE.toString()
							.replace("{player}", tp.getName()));
					event.setCancelled(true);
					return;
				}
				String filtered = chat;
				String echo = "";
				String send = "";
				String spy = "";
				if(mcp.hasFilter()) {
					filtered = Format.FilterChat(filtered);
				}
				if(mcp.getPlayer().hasPermission("venturechat.color.legacy")) {
					filtered = Format.FormatStringLegacyColor(filtered);
				}
				if(mcp.getPlayer().hasPermission("venturechat.color")) {
					filtered = Format.FormatStringColor(filtered);
				}
				if(mcp.getPlayer().hasPermission("venturechat.format")) {
					filtered = Format.FormatString(filtered);
				}
				filtered = " " + filtered;
				
				send = Format.FormatStringAll(PlaceholderAPI.setBracketPlaceholders(mcp.getPlayer(), plugin.getConfig().getString("tellformatfrom").replaceAll("sender_", "")));
				echo = Format.FormatStringAll(PlaceholderAPI.setBracketPlaceholders(mcp.getPlayer(), plugin.getConfig().getString("tellformatto").replaceAll("sender_", "")));
				spy = Format.FormatStringAll(PlaceholderAPI.setBracketPlaceholders(mcp.getPlayer(), plugin.getConfig().getString("tellformatspy").replaceAll("sender_", "")));
				
				send = Format.FormatStringAll(PlaceholderAPI.setBracketPlaceholders(tp.getPlayer(), send.replaceAll("receiver_", ""))) + filtered;
				echo = Format.FormatStringAll(PlaceholderAPI.setBracketPlaceholders(tp.getPlayer(), echo.replaceAll("receiver_", ""))) + filtered;
				spy = Format.FormatStringAll(PlaceholderAPI.setBracketPlaceholders(tp.getPlayer(), spy.replaceAll("receiver_", ""))) + filtered;
				
				if(!mcp.getPlayer().hasPermission("venturechat.spy.override")) {
					for(MineverseChatPlayer p : MineverseChatAPI.getOnlineMineverseChatPlayers()) {
						if(p.getName().equals(mcp.getName()) || p.getName().equals(tp.getName())) {
							continue;
						}
						if(p.isSpy()) {
							p.getPlayer().sendMessage(spy);
						}
					}
				}
				tp.getPlayer().sendMessage(send);
				mcp.getPlayer().sendMessage(echo);
				if(tp.hasNotifications()) {
					Format.playMessageSound(tp);
				}
				mcp.setReplyPlayer(tp.getUUID());
				tp.setReplyPlayer(mcp.getUUID());
				if(Database.isEnabled()) {
					Database.writeVentureChat(mcp.getUUID().toString(), mcp.getName(), "Local", "Messaging_Component", chat.replace("'", "''"), "Chat");
				}
			}
			return;
		}

		if(mcp.isPartyChat()) {
			if(mcp.hasParty()) {
				String partyformat = "";
				for(MineverseChatPlayer p : MineverseChatAPI.getOnlineMineverseChatPlayers()) {
					if((p.hasParty() && p.getParty().toString().equals(mcp.getParty().toString()) || p.isSpy())) {
						String filtered = chat;
						if(mcp.hasFilter()) {
							filtered = Format.FilterChat(filtered);
						}
						if(mcp.getPlayer().hasPermission("venturechat.color.legacy")) {
							filtered = Format.FormatStringLegacyColor(filtered);
						}
						if(mcp.getPlayer().hasPermission("venturechat.color")) {
							filtered = Format.FormatStringColor(filtered);
						}
						if(mcp.getPlayer().hasPermission("venturechat.format")) {
							filtered = Format.FormatString(filtered);
						}
						filtered = " " + filtered;
						if(plugin.getConfig().getString("partyformat").equalsIgnoreCase("Default")) {
							partyformat = ChatColor.GREEN + "[" + MineverseChatAPI.getMineverseChatPlayer(mcp.getParty()).getName() + "'s Party] " + mcp.getName() + ":" + filtered;
						}
						else {
							partyformat = Format.FormatStringAll(plugin.getConfig().getString("partyformat").replace("{host}", MineverseChatAPI.getMineverseChatPlayer(mcp.getParty()).getName()).replace("{player}", mcp.getName())) + filtered;
						}
						p.getPlayer().sendMessage(partyformat);
					}
				}
				Bukkit.getConsoleSender().sendMessage(partyformat);
				if(Database.isEnabled()) {
					Database.writeVentureChat(mcp.getUUID().toString(), mcp.getName(), "Local", "Party_Component", chat.replace("'", "''"), "Chat");
				}
				return;
			}
			mcp.getPlayer().sendMessage(ChatColor.RED + "You are not in a party.");
			return;
		}
		
		Location locreceip;
		Location locsender = mcp.getPlayer().getLocation();
		Location diff;
		Boolean filterthis = true;
		Double chDistance = (double) 0;
		if(eventChannel.hasSpeakPermission() && !mcp.getPlayer().hasPermission(eventChannel.getSpeakPermission())) {
			mcp.getPlayer().sendMessage(LocalizedMessage.CHANNEL_NO_SPEAK_PERMISSIONS.toString());
			return;
		}
		
		long dateTimeSeconds = System.currentTimeMillis() / Format.MILLISECONDS_PER_SECOND;
		
		int chCooldown = 0;
		if(eventChannel.hasCooldown()) {
			chCooldown = eventChannel.getCooldown();
		}
		try {
			if (mcp.hasCooldown(eventChannel)) {
				long cooldownTime = mcp.getCooldowns().get(eventChannel).longValue();
				if (dateTimeSeconds < cooldownTime) {
					long remainingCooldownTime = cooldownTime - dateTimeSeconds;
					String cooldownString = Format.parseTimeStringFromMillis(remainingCooldownTime * Format.MILLISECONDS_PER_SECOND);
					mcp.getPlayer().sendMessage(LocalizedMessage.CHANNEL_COOLDOWN.toString()
							.replace("{cooldown}", cooldownString));
					return;
				}
			}
			if (eventChannel.hasCooldown()) {
				if (!mcp.getPlayer().hasPermission("venturechat.cooldown.bypass")) {
					mcp.addCooldown(eventChannel, dateTimeSeconds + chCooldown);
				}
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
		
		if (mcp.hasSpam(eventChannel) && plugin.getConfig().getConfigurationSection("antispam").getBoolean("enabled")
				&& !mcp.getPlayer().hasPermission("venturechat.spam.bypass")) {
			long spamcount = mcp.getSpam().get(eventChannel).get(0);
			long spamtime = mcp.getSpam().get(eventChannel).get(1);
			long spamtimeconfig = plugin.getConfig().getConfigurationSection("antispam").getLong("spamnumber");
			long dateTime = System.currentTimeMillis();
			if (dateTimeSeconds < spamtime
					+ plugin.getConfig().getConfigurationSection("antispam").getLong("spamtime")) {
				if (spamcount + 1 >= spamtimeconfig) {
					mcp.getSpam().get(eventChannel).set(0, 0L);
					return;
				} else {
					if (spamtimeconfig % 2 != 0) {
						spamtimeconfig++;
					}
					if (spamcount + 1 == spamtimeconfig / 2) {
						mcp.getPlayer().sendMessage(LocalizedMessage.SPAM_WARNING.toString());
					}
					mcp.getSpam().get(eventChannel).set(0, spamcount + 1);
				}
			} else {
				mcp.getSpam().get(eventChannel).set(0, 1L);
				mcp.getSpam().get(eventChannel).set(1, dateTimeSeconds);
			}
		} else {
			mcp.addSpam(eventChannel);
			mcp.getSpam().get(eventChannel).add(0, 1L);
			mcp.getSpam().get(eventChannel).add(1, dateTimeSeconds);
		}
		
		if(eventChannel.hasDistance()) {
			chDistance = eventChannel.getDistance();
		}
		
		format = Format.FormatStringAll(eventChannel.getFormat());
		
		filterthis = eventChannel.isFiltered();
		if(filterthis) {
			if(mcp.hasFilter()) {
				chat = Format.FilterChat(chat);
			}
		}
		PluginManager pluginManager = plugin.getServer().getPluginManager();
		for(MineverseChatPlayer p : MineverseChatAPI.getOnlineMineverseChatPlayers()) {
			if(p.getPlayer() != mcp.getPlayer()) {
				if(plugin.getConfig().getBoolean("ignorechat", false) && p.getIgnores().contains(mcp.getUUID())) {
					recipients.remove(p.getPlayer());
					recipientCount--;
					continue;
				}
				if(plugin.getConfig().getBoolean("enable_towny_channel") && pluginManager.isPluginEnabled("Towny")) {
					try {
						TownyUniverse towny = TownyUniverse.getInstance();
						if(eventChannel.getName().equalsIgnoreCase("Town")) {
							Resident r = towny.getResident(p.getName());
							Resident pp = towny.getResident(mcp.getName());
							if(!pp.hasTown()) {
								recipients.remove(p.getPlayer());
								recipientCount--;
								continue;
							}
							else if(!r.hasTown()) {
								recipients.remove(p.getPlayer());
								recipientCount--;
								continue;
							}
							else if(!(r.getTown().getName().equals(pp.getTown().getName()))) {
								recipients.remove(p.getPlayer());
								recipientCount--;
								continue;
							}
						}
						if(eventChannel.getName().equalsIgnoreCase("Nation")) {
							Resident r = towny.getResident(p.getName());
							Resident pp = towny.getResident(mcp.getName());
							if(!pp.hasNation()) {
								recipients.remove(p.getPlayer());
								recipientCount--;
								continue;
							}
							else if(!r.hasNation()) {
								recipients.remove(p.getPlayer());
								recipientCount--;
								continue;
							}
							else if(!(r.getTown().getNation().getName().equals(pp.getTown().getNation().getName()))) {
								recipients.remove(p.getPlayer());
								recipientCount--;
								continue;
							}
						}
					}
					catch(Exception ex) {
						ex.printStackTrace();
					}
				}

				if(plugin.getConfig().getBoolean("enable_factions_channel") && pluginManager.isPluginEnabled("Factions")) {
					try {
						if(eventChannel.getName().equalsIgnoreCase("Faction")) {
							MPlayer mplayer = MPlayer.get(mcp.getPlayer());
							MPlayer mplayerp = MPlayer.get(p.getPlayer());
							if(!mplayer.hasFaction()) {
								recipients.remove(p.getPlayer());
								recipientCount--;
							}
							else if(!mplayerp.hasFaction()) {
								recipients.remove(p.getPlayer());
								recipientCount--;
							}
							else if(!(mplayer.getFactionName().equals(mplayerp.getFactionName()))) {
								recipients.remove(p.getPlayer());
								recipientCount--;
							}
						}
					}
					catch(Exception ex) {
						ex.printStackTrace();
					}
				}

				if(chDistance > (double) 0 && !p.getRangedSpy()) {
					locreceip = p.getPlayer().getLocation();
					if(locreceip.getWorld() == mcp.getPlayer().getWorld()) {
						diff = locreceip.subtract(locsender);
						if(Math.abs(diff.getX()) > chDistance || Math.abs(diff.getZ()) > chDistance || Math.abs(diff.getY()) > chDistance) {
							recipients.remove(p.getPlayer());
							recipientCount--;
							continue;
						}
						if(!mcp.getPlayer().canSee(p.getPlayer())) {
							recipientCount--;
							continue;
						}
					}
					else {
						recipients.remove(p.getPlayer());
						recipientCount--;
						continue;
					}
				}
				if(!mcp.getPlayer().canSee(p.getPlayer())) {
					recipientCount--;
				}
			}
		}
		
		if(mcp.getPlayer().hasPermission("venturechat.color.legacy")) {
			chat = Format.FormatStringLegacyColor(chat);
		}
		if(mcp.getPlayer().hasPermission("venturechat.color")) {
			chat = Format.FormatStringColor(chat);
		}
		if(mcp.getPlayer().hasPermission("venturechat.format")) {
			chat = Format.FormatString(chat);
		}
		// Format the placeholders and their color codes to determine the last color code to use for the chat message color
		chat = Format.getLastCode(Format.FormatStringAll(PlaceholderAPI.setBracketPlaceholders(mcp.getPlayer(), format))) + chat;

		String globalJSON = Format.convertToJson(mcp, format, chat);
		format = Format.FormatStringAll(PlaceholderAPI.setBracketPlaceholders(mcp.getPlayer(), Format.FormatStringAll(format)));
		String message = Format.stripColor(format + chat); // UTF-8 encoding issues.
		int hash = message.hashCode();
		
		//Create VentureChatEvent
		VentureChatEvent ventureChatEvent = new VentureChatEvent(mcp, mcp.getName(), mcp.getNickname(), MineverseChat.getVaultPermission().getPrimaryGroup(mcp.getPlayer()), eventChannel, recipients, recipientCount, format, chat, globalJSON, hash);
		//Fire event and wait for other plugin listeners to act on it
		Bukkit.getServer().getPluginManager().callEvent(ventureChatEvent);
		//Call method to send the processed chat
		handleVentureChatEvent(ventureChatEvent);
	}
	
	public void handleVentureChatEvent(VentureChatEvent event) {
		MineverseChatPlayer mcp = event.getMineverseChatPlayer();
		ChatChannel channel = event.getChannel();
		Set<Player> recipients = event.getRecipients();
		int recipientCount = event.getRecipientCount();
		String chat = event.getChat();
		String consoleChat = event.getConsoleChat();
		String globalJSON = event.getGlobalJSON();;

		if (essentialsDiscordHook) {
			Bukkit.getServicesManager().load(DiscordService.class).sendChatMessage(mcp.getPlayer(), chat);
		}

		if(Database.isEnabled()) {
			Database.writeVentureChat(mcp.getUUID().toString(), mcp.getName(), "Local", channel.getName(), chat.replace("'", "''"), "Chat");
		}

		if(recipientCount == 1) {
			if(!plugin.getConfig().getString("emptychannelalert", "&6No one is listening to you.").equals("")) {
				mcp.getPlayer().sendMessage(Format.FormatStringAll(plugin.getConfig().getString("emptychannelalert", "&6No one is listening to you.")));
			}
		}
		for(Player p : recipients) {
			PacketContainer packet = Format.createPacketPlayOutChat(globalJSON);
			Format.sendPacketPlayOutChat(p, packet);
		}
		Bukkit.getConsoleSender().sendMessage(consoleChat);

	}
}
