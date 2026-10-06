package net.unitedlands.cosmetics.storage;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.unitedlands.utils.Logger;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.bukkit.Bukkit.getPluginManager;

public class CosmeticsProfile {

    private final OfflinePlayer player;
    private final File file;
    private FileConfiguration config;
    private final Map<String, EquippedClothing> equippedClothing = new HashMap<>();

    public CosmeticsProfile(OfflinePlayer player) {
        JavaPlugin plugin = (JavaPlugin) getPluginManager().getPlugin("UnitedCosmetics");
        this.player = player;
        this.file = new File(Objects.requireNonNull(plugin).getDataFolder(), "players" + File.separator + player.getUniqueId() + ".yml");
        this.config = loadConfig();
    }

    public boolean hasFile() {
        return file.exists();
    }

    public void createFile() {
        if (hasFile()) return;

        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            if (!parent.mkdirs()) {
                Logger.logError("Failed to create parent directory for " + file.getAbsolutePath(), "UnitedCosmetics");
                return;
            }
        }

        try {
            if (file.createNewFile()) {

                config = new YamlConfiguration();
                config.set("name", player.getName());

                // Initialise custom prefix slots.
                config.set("saved-prefixes.slot_1", null);
                config.set("saved-prefixes.slot_2", null);
                config.set("saved-prefixes.slot_3", null);

                // Initialise chat colour slots.
                config.set("saved-chat-colours.slot_1", null);
                config.set("saved-chat-colours.slot_2", null);
                config.set("saved-chat-colours.slot_3", null);

                // Initialise equipped statuses.
                config.set("equipped.prefix", null);
                config.set("equipped.chat-colour", null);

                saveConfig();
                Logger.log("Created cosmetic profile for " + player.getName(), "UnitedCosmetics");
            }
        }
        catch (IOException e) {
            Logger.logError("IOException while creating player data file: " + file.getAbsolutePath(), "UnitedCosmetics");
        }
    }

    public void deleteFile() {
        if (hasFile()) {
            if (file.delete()) {
                Logger.log("Deleted cosmetic profile for " + player.getName(), "UnitedCosmetics");
            } else {
                Logger.logError("Failed to delete cosmetic profile for " + player.getName(), "UnitedCosmetics");
            }
        }
    }

    public void reset() {
        if (!hasFile()) return;

        config.set("saved-prefixes.slot_1", null);
        config.set("saved-prefixes.slot_2", null);
        config.set("saved-prefixes.slot_3", null);

        config.set("saved-chat-colours.slot_1", null);
        config.set("saved-chat-colours.slot_2", null);
        config.set("saved-chat-colours.slot_3", null);

        saveConfig();

        Logger.log("Reset cosmetic profile for " + player.getName(), "UnitedCosmetics");
    }

    private FileConfiguration loadConfig() {
        if (!hasFile()) return new YamlConfiguration();

        FileConfiguration fileConfiguration = new YamlConfiguration();

        try {
            fileConfiguration.load(file);
            equippedClothing.clear();
            ConfigurationSection section = fileConfiguration.getConfigurationSection("equipped.clothing");

            if (section != null) {
                for (String type : section.getKeys(false)) {
                    String nexoId = section.getString(type + ".id");
                    boolean isDigital = section.getBoolean(type + ".digital");
                    equippedClothing.put(type, new EquippedClothing(nexoId, isDigital));
                }
            }

            return fileConfiguration;
        } catch (IOException | InvalidConfigurationException e) {
            Logger.logError("Failed to load player data file: " + file.getAbsolutePath(), "UnitedCosmetics");
            return new YamlConfiguration();
        }
    }

    private void saveConfig() {
        try {

            config.set("equipped.clothing", null);
            equippedClothing.forEach((type, item) -> {
                config.set("equipped.clothing." + type + ".id", item.nexoId());
                config.set("equipped.clothing." + type + ".digital", item.isDigital());
            });

            config.save(file);
        } catch (IOException e) {
            Logger.logError("Failed to save player data file: " + file.getAbsolutePath(), "UnitedCosmetics");
        }
    }

// +----------------------------------------+ #
// |           Data Access Methods          | #
// +----------------------------------------+ #

    // Custom Prefixes
    public String getSavedPrefix(int slot) {
        return config.getString("saved-prefixes.slot_" + slot);
    }

    public void savePrefix(int slot, String prefixRaw) {
        if (!hasFile()) createFile();
        config.set("saved-prefixes.slot_" + slot, prefixRaw);
        saveConfig();
    }

    public void setEquippedPrefix(String slotKey) {
        if (!hasFile()) createFile();
        config.set("equipped.prefix", slotKey);
        saveConfig();
    }

    public String getEquippedPrefix() {
        return config.getString("equipped.prefix");
    }

    // Chat Colour
    public String getSavedChatColour(int slot) {
        return config.getString("saved-chat-colours.slot_" + slot);
    }

    public void saveChatColour(int slot, String colourTag) {
        if (!hasFile()) createFile();
        config.set("saved-chat-colours.slot_" + slot, colourTag);
        saveConfig();
    }

    public void setEquippedChatColour(String slotKey) {
        if (!hasFile()) createFile();
        config.set("equipped.chat-colour", slotKey);
        saveConfig();
    }

    public String getEquippedChatColour() {
        return config.getString("equipped.chat-colour");
    }

    // Clothing
    public EquippedClothing getEquippedClothing(String type) {
        return equippedClothing.get(type);
    }

    public void setEquippedClothing(String type, String nexoId, boolean isDigital) {
        if (!hasFile()) createFile();
        equippedClothing.put(type, new EquippedClothing(nexoId, isDigital));
        saveConfig();
    }

    public void removeEquippedClothing(String type) {
        if (!hasFile()) return;
        equippedClothing.remove(type);
        saveConfig();
    }

    public boolean hasEquippedClothing(String type) {
        return equippedClothing.containsKey(type);
    }
}