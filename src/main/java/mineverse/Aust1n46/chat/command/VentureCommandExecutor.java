package mineverse.Aust1n46.chat.command;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import mineverse.Aust1n46.chat.command.chat.*;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import mineverse.Aust1n46.chat.MineverseChat;
import mineverse.Aust1n46.chat.command.message.Ignore;
import mineverse.Aust1n46.chat.command.message.Message;
import mineverse.Aust1n46.chat.command.message.MessageToggle;
import mineverse.Aust1n46.chat.command.message.Notifications;
import mineverse.Aust1n46.chat.command.message.Reply;
import mineverse.Aust1n46.chat.command.message.Spy;
import mineverse.Aust1n46.chat.utilities.Format;

/**
 * Class that initializes and executes the plugin's commands.
 */
public class VentureCommandExecutor {
	private static final String VERSION = "3.3.0";
	private static final Map<String, Command> commands = new HashMap<>();
	private static final MineverseChat plugin = MineverseChat.getInstance();

	public static void initialize() {
		final Server server = plugin.getServer();
		final File commandsFile = new File(plugin.getDataFolder().getAbsolutePath(), "commands.yml");
		if (!commandsFile.isFile()) {
			plugin.saveResource("commands.yml", true);
		}
		FileConfiguration commandsFileConfiguration = YamlConfiguration.loadConfiguration(commandsFile);
		final String fileVersion = commandsFileConfiguration.getString("Version", "null");
		if (!fileVersion.equals(VERSION)) {
			server.getConsoleSender().sendMessage(Format.FormatStringAll("&8[&eVentureChat&8]&e - Version Change Detected!  Saving Old commands.yml and Generating Latest File"));
			commandsFile.renameTo(new File(plugin.getDataFolder().getAbsolutePath(), "commands_old_" + fileVersion + ".yml"));
			plugin.saveResource("commands.yml", true);
			commandsFileConfiguration = YamlConfiguration.loadConfiguration(commandsFile);
		}
		commands.put("broadcast", new Broadcast());
		commands.put("chatreload", new Chatreload());
		commands.put("clearchat", new Clearchat());
		commands.put("commandspy", new Commandspy());
		commands.put("filter", new Filter());
		commands.put("force", new Force());
		commands.put("forceall", new Forceall());
		commands.put("me", new Me());
		commands.put("notifications", new Notifications());
		commands.put("group", new Group());
		commands.put("rangedspy", new RangedSpy());
		commands.put("spy", new Spy());
		commands.put("messagetoggle", new MessageToggle());
		commands.put("reply", new Reply());
		commands.put("message", new Message());
		commands.put("ignore", new Ignore());
		final ConfigurationSection commandsSection = commandsFileConfiguration.getConfigurationSection("commands");
		for (final String commandName : commandsSection.getKeys(false)) {
			final ConfigurationSection commandSection = commandsSection.getConfigurationSection(commandName);
			final boolean isEnabled = commandSection.getBoolean("enabled", true);
			if (!isEnabled) {
				commands.remove(commandName);
			} else {
				final Command command = commands.get(commandName);
				if (command != null) {
					command.setAliases(commandSection.getStringList("aliases"));
				}
			}
		}
		final CommandMap commandMap = getCommandMap(server);
		registerAll(commandMap);
		server.getScheduler().runTaskLater(plugin, () -> registerAll(commandMap), 10);
	}

	private static void registerAll(final CommandMap commandMap) {
		for (final Entry<String, Command> commandEntry : commands.entrySet()) {
			commandMap.register("venturechat", commandEntry.getValue());
		}
	}

	private static CommandMap getCommandMap(final Server server) {
		try {
			return server.getCommandMap(); // Paper
		} catch (final NoSuchMethodError e) {
			try {
				final Field commandMapField = server.getClass().getDeclaredField("commandMap");
				commandMapField.setAccessible(true);
				return (CommandMap) commandMapField.get(server);
			} catch (final ReflectiveOperationException ex) {
				throw new RuntimeException("Unable to access CommandMap. Use Paper.", ex);
			}
		}
	}
}
