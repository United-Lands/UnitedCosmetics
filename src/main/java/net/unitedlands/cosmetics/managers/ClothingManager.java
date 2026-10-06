package net.unitedlands.cosmetics.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.unitedlands.cosmetics.UnitedCosmetics;
import net.unitedlands.cosmetics.storage.CosmeticsProfile;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.EulerAngle;
import org.unitedlands.UnitedLib;

import java.util.*;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;

public class ClothingManager {

    private final UnitedCosmetics plugin;
    private final Map<UUID, ArmorStand> bodyCosmetics = new HashMap<>();

    public ClothingManager(UnitedCosmetics plugin) {
        this.plugin = plugin;
        startRotationSyncTask();
    }

    // Head cosmetics (helmet spoof).
    public void refreshSpoof(Player wearer) {
        CosmeticsProfile profile = plugin.loadProfile(wearer);
        if (!profile.hasEquippedClothing("hat")) return;

        String itemId = profile.getEquippedClothing("hat").nexoId();
        ItemStack cosmeticHat = UnitedLib.getInstance().getItemFactory().getItemStack(itemId, 1);

        if (cosmeticHat == null || cosmeticHat.isEmpty()) return;

        ItemStack realHelmet = wearer.getInventory().getHelmet();
        ItemStack wearerItem = cosmeticHat.clone();
        ItemMeta wearerMeta = wearerItem.getItemMeta();

        if (wearerMeta != null) {
            wearerMeta.setEnchantmentGlintOverride(false);
            if (!realHelmet.isEmpty()) {
                ItemMeta realMeta = realHelmet.getItemMeta();

                // Super-duper accurate helmet spoofing.
                // If player is wearing an actual helmet underneath, display as much original item info as possible.
                if (realMeta != null) {
                    // Item name.
                    if (realMeta.hasDisplayName()) {
                        wearerMeta.displayName(realMeta.displayName());
                    } else if (realMeta.hasItemName()) {
                        wearerMeta.itemName(realMeta.itemName());
                    } else {
                        String rawName = UnitedLib.getInstance().getItemFactory().getDisplayName(realHelmet);
                        wearerMeta.displayName(Component.text(rawName, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
                    }

                    // Item lore + cosmetic marker.
                    List<Component> lore = realMeta.hasLore() && realMeta.lore() != null
                            ? new ArrayList<>(Objects.requireNonNull(realMeta.lore()))
                            : new ArrayList<>();

                    lore.add(Component.empty());
                    lore.add(Component.text("Visual Cosmetic Active", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                    wearerMeta.lore(lore);

                    // Enchants
                    if (realMeta.hasEnchants()) {
                        realMeta.getEnchants().forEach((ench, level) -> wearerMeta.addEnchant(ench, level, true));
                    }

                    // Item flags.
                    wearerMeta.addItemFlags(realMeta.getItemFlags().toArray(new ItemFlag[0]));
                    wearerMeta.setUnbreakable(realMeta.isUnbreakable());

                    // Durability.
                    if (realMeta instanceof org.bukkit.inventory.meta.Damageable realDamage && wearerMeta instanceof Damageable wearerDamage) {
                        wearerDamage.setDamage(realDamage.getDamage());
                        if (realDamage.hasMaxDamage()) {
                            wearerDamage.setMaxDamage(realDamage.getMaxDamage());
                        }
                    }

                    // Armour stats
                    if (realMeta.hasAttributeModifiers()) {
                        wearerMeta.setAttributeModifiers(realMeta.getAttributeModifiers());
                    } else {
                        wearerMeta.setAttributeModifiers(realHelmet.getType().getDefaultAttributeModifiers(EquipmentSlot.HEAD));
                    }
                }

            } else {
                List<Component> existingLore = wearerMeta.lore();
                List<Component> lore = existingLore != null
                        ? new java.util.ArrayList<>(existingLore)
                        : new java.util.ArrayList<>();

                lore.add(miniMessage().deserialize("<gray>Cosmetic Hat</gray>").decoration(TextDecoration.ITALIC, false));
                lore.add(miniMessage().deserialize("<gray>Manage in the Wardrobe</gray>").decoration(TextDecoration.ITALIC, false));

                NamespacedKey dummyKey = new NamespacedKey("unitedcosmetics", "dummy_cosmetic");
                wearerMeta.getPersistentDataContainer().set(dummyKey, PersistentDataType.BYTE, (byte) 1);

                wearerMeta.lore(lore);
            }

            wearerItem.setItemMeta(wearerMeta);
        }

        for (Player viewer : plugin.getServer().getOnlinePlayers()) {
            if (viewer.equals(wearer)) {
                viewer.sendEquipmentChange(wearer, EquipmentSlot.HEAD, wearerItem);
            } else {
                viewer.sendEquipmentChange(wearer, EquipmentSlot.HEAD, cosmeticHat);
            }
        }
    }

    public void clearSpoof(Player wearer) {
        ItemStack realHelmet = wearer.getInventory().getHelmet();
        for (Player viewer : plugin.getServer().getOnlinePlayers()) {
            viewer.sendEquipmentChange(wearer, EquipmentSlot.HEAD, realHelmet);
        }
    }

    // Body-worn cosmetics (armour-stand based).
    public void spawnBodyCosmetic(Player player, String itemId) {
        removeBodyCosmetic(player);

        ItemStack cosmeticItem = null;
        if (itemId != null && !itemId.isEmpty()) {
            cosmeticItem = UnitedLib.getInstance().getItemFactory().getItemStack(itemId, 1);
        }

        ItemStack finalCosmeticItem = cosmeticItem;
        ArmorStand display = player.getWorld().spawn(player.getLocation(), ArmorStand.class, ent -> {
            ent.setInvisible(true);
            ent.setInvulnerable(true);
            ent.setPersistent(false);
            ent.setGravity(false);
            ent.setBasePlate(false);
            ent.setSmall(true);
            ent.setMarker(true);
            ent.setCollidable(false);
            ent.setCanTick(false);
            ent.getEquipment().setHelmet(finalCosmeticItem);
        });

        bodyCosmetics.put(player.getUniqueId(), display);

        // Delay mount slightly to avoid client packet race conditions on spawn.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && !display.isDead()) {
                display.teleport(player.getLocation());
                player.addPassenger(display);
                display.setRotation(player.getLocation().getYaw(), 0f);
            }
        }, 5L);
    }

    public boolean isBodyCosmetic(Player player, Entity entity) {
        Entity tracked = bodyCosmetics.get(player.getUniqueId());
        return tracked != null && tracked.equals(entity);
    }

    public void removeBodyCosmetic(Player player) {
        Entity display = bodyCosmetics.remove(player.getUniqueId());
        if (display != null && !display.isDead()) {
            display.remove();
        }
    }

    public void cleanupAll() {
        for (ArmorStand stand : bodyCosmetics.values()) {
            if (stand != null && !stand.isDead()) {
                stand.remove();
            }
        }
        bodyCosmetics.clear();
    }

    // Sync body worn cosmetics with player yaw routinely.
    private void startRotationSyncTask() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Map.Entry<UUID, ArmorStand> entry : bodyCosmetics.entrySet()) {
                Player player = plugin.getServer().getPlayer(entry.getKey());
                ArmorStand stand = entry.getValue();

                if (player != null && player.isOnline() && !stand.isDead()) {
                    float playerYaw = player.getYaw();

                    // First actually check the player turned.
                    if (Math.abs(stand.getLocation().getYaw() - playerYaw) > 1.0f) {
                        stand.setRotation(playerYaw, 0f);
                    }

                    FileConfiguration config = plugin.getConfig();
                    double headPitch = config.getDouble("wardrobe.positioning.chest.sneak-angle");

                    if (player.isSneaking()) {
                        stand.setHeadPose(new EulerAngle(Math.toRadians(headPitch), 0, 0));
                    } else {
                        stand.setHeadPose(EulerAngle.ZERO);
                    }

                }
            }
        }, 1L, 1L);
    }
}