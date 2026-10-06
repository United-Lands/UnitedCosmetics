package net.unitedlands.cosmetics.utils;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.unitedlands.cosmetics.storage.CosmeticsProfile;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class CosmeticsPlaceholders extends PlaceholderExpansion {

    @Override
    public @NotNull String getIdentifier() {
        return "unitedcosmetics";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Litning11";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        CosmeticsProfile profile = new CosmeticsProfile(player);

        // Custom prefixes.
        if (params.startsWith("prefix_")) {
            int slot = parseSlot(params.replace("prefix_", ""));
            if (slot != -1) {
                String prefix = profile.getSavedPrefix(slot);
                return prefix != null ? prefix : "None";
            }
        }

        if (params.startsWith("has_prefix_")) {
            int slot = parseSlot(params.replace("has_prefix_", ""));
            if (slot != -1) {
                String prefix = profile.getSavedPrefix(slot);
                return (prefix != null && !prefix.isEmpty()) ? "Yes" : "No";
            }
        }

        // Chat colours.
        if (params.startsWith("chatcolour_")) {
            int slot = parseSlot(params.replace("chatcolour_", ""));
            if (slot != -1) {
                String color = profile.getSavedChatColour(slot);
                return color != null ? color : "None";
            }
        }

        if (params.startsWith("has_chatcolour_")) {
            int slot = parseSlot(params.replace("has_chatcolour_", ""));
            if (slot != -1) {
                String color = profile.getSavedChatColour(slot);
                return (color != null && !color.isEmpty()) ? "Yes" : "No";
            }
        }

        // Live preview generator (for menu editors).
        if (params.equals("preview")) {
            // Grab the max colours dynamically from the config
            org.bukkit.plugin.java.JavaPlugin plugin = org.bukkit.plugin.java.JavaPlugin.getPlugin(net.unitedlands.cosmetics.UnitedCosmetics.class);
            int maxColours = plugin.getConfig().getInt("chatcolour.colours", 3);

            StringBuilder colours = new StringBuilder();
            int count = 0;

            // Loop through however many inputs the config allows.
            for (int i = 1; i <= maxColours; i++) {
                String sessionVar = "%commandpanels_session_chatcolour_input_" + i + "%";
                String input = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, sessionVar);

                if (input.matches("^#([A-Fa-f0-9]{6})$")) {
                    if (count > 0) colours.append(":");
                    colours.append(input);
                    count++;
                }
            }
            // If they haven't typed a valid hex code yet.
            if (count == 0) {
                return "<gray>Waiting for valid hex code...</gray>";
            }

            // Build the tag and apply it to a sample text.
            String tag = count == 1 ? "<" + colours + ">" : "<gradient:" + colours + ">";
            return tag + "Sample Chat Colour Text<reset>";
        }

        return null;
    }

    private int parseSlot(String arg) {
        try {
            return Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
