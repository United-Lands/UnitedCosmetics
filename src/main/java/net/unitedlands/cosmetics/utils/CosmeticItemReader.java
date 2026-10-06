package net.unitedlands.cosmetics.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class CosmeticItemReader {
    private final NamespacedKey clothingKey;

    public CosmeticItemReader(JavaPlugin plugin) {
        this.clothingKey = new NamespacedKey(plugin, "clothing_type");
    }

    public String getClothingType(ItemStack item) {
        if (item == null) return null;
        if (!item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(clothingKey, PersistentDataType.STRING);
    }
}