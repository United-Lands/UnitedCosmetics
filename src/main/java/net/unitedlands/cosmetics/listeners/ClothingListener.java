package net.unitedlands.cosmetics.listeners;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import net.unitedlands.cosmetics.UnitedCosmetics;
import net.unitedlands.cosmetics.managers.ClothingManager;
import net.unitedlands.cosmetics.storage.CosmeticsProfile;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class ClothingListener implements Listener {

    private final UnitedCosmetics plugin;
    private final ClothingManager clothingManager;

    public ClothingListener(UnitedCosmetics plugin, ClothingManager clothingManager) {
        this.plugin = plugin;
        this.clothingManager = clothingManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (event.getSlotType() == InventoryType.SlotType.ARMOR && event.getSlot() == 39) {
            CosmeticsProfile profile = plugin.loadProfile(player);
            ItemStack realHelmet = player.getInventory().getHelmet();

            if (profile != null && profile.hasEquippedClothing("hat") && realHelmet.isEmpty()) {

                ItemStack cursor = event.getCursor();
                boolean isCursorEmpty = cursor.isEmpty();
                if (isCursorEmpty || event.getClick().isKeyboardClick()) {

                    event.setCancelled(true);

                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        player.updateInventory();
                        clothingManager.refreshSpoof(player);
                    });
                }
            }
        }
    }

    @EventHandler
    public void onArmourChange(PlayerArmorChangeEvent event) {
        if (event.getSlot() == EquipmentSlot.HEAD) {
            Player player = event.getPlayer();
            if (plugin.loadProfile(player).hasEquippedClothing("hat")) {
                clothingManager.refreshSpoof(player);
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        CosmeticsProfile profile = plugin.loadProfile(player);
        if (profile == null) return;

        // Spawn hat cosmetic.
        if (profile.hasEquippedClothing("hat")) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> clothingManager.refreshSpoof(player), 5L);
        }
        // Spawn chest cosmetic.
        if (profile.hasEquippedClothing("chest")) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                    clothingManager.spawnBodyCosmetic(player, profile.getEquippedClothing("chest").nexoId()), 5L);
        }

        // Spoofer to show other players the equipped hat.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                if (!online.equals(player) && plugin.loadProfile(online) != null && plugin.loadProfile(online).hasEquippedClothing("hat")) {
                    clothingManager.refreshSpoof(online);
                }
            }
        }, 5L);
    }

    // Prevent clothing from dismounting (like when jumping in water).
    @EventHandler
    public void onDismount(EntityDismountEvent event) {
        if (event.getEntity() instanceof ArmorStand stand
                && event.getDismounted() instanceof Player player) {
            if (clothingManager.isBodyCosmetic(player, stand)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        CosmeticsProfile profile = plugin.loadProfile(player);

        if (profile != null) {
            if (profile.hasEquippedClothing("hat")) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        clothingManager.refreshSpoof(player);
                    }
                }, 5L);
            }

            // Despawn and recreate the passenger safely.
            if (profile.hasEquippedClothing("chest")) {
                clothingManager.removeBodyCosmetic(player);
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        clothingManager.spawnBodyCosmetic(player, profile.getEquippedClothing("chest").nexoId());
                    }
                }, 5L);
            }
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            CosmeticsProfile profile = plugin.loadProfile(player);

            // Respoof the hat just in case the client cleared the packet on death.
            if (profile.hasEquippedClothing("hat")) {
                clothingManager.refreshSpoof(player);
            }

            // Respawn the body cosmetics.
            if (profile.hasEquippedClothing("chest")) {
                clothingManager.spawnBodyCosmetic(player, profile.getEquippedClothing("chest").nexoId());
            }
        }, 5L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clothingManager.removeBodyCosmetic(event.getPlayer());
        plugin.unloadProfile(event.getPlayer());
    }
}