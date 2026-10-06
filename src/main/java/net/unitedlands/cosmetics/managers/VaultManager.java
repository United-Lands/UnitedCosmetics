package net.unitedlands.cosmetics.managers;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.unitedlands.UnitedLib;

import java.util.ArrayList;
import java.util.List;

public class VaultManager {

    private final List<ItemStack> cachedCosmetics = new ArrayList<>();
    private final NamespacedKey typeKey;

    public VaultManager(JavaPlugin plugin) {
        this.typeKey = new NamespacedKey(plugin, "clothing_type");
        loadVaultItems();
    }

    public void loadVaultItems() {
        cachedCosmetics.clear();

        var itemFactory = UnitedLib.getInstance().getItemFactory();

        for (String id : itemFactory.getItemList()) {
            ItemStack item = itemFactory.getItemStack(id, 1);
            if (item == null || !item.hasItemMeta()) continue;

            var pdc = item.getItemMeta().getPersistentDataContainer();
            if (pdc.has(typeKey, PersistentDataType.STRING)) {
                cachedCosmetics.add(item);
            }
        }
    }

    public List<ItemStack> getCachedCosmetics() {
        // Lazy load the vault in case Nexo wasn't ready before the vault was generated.
        if (cachedCosmetics.isEmpty()) {
            loadVaultItems();
        }
        return cachedCosmetics;
    }

    public void reloadVault() {
        loadVaultItems();
    }
}