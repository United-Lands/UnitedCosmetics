package net.unitedlands.cosmetics.menus;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.unitedlands.cosmetics.UnitedCosmetics;
import net.unitedlands.cosmetics.managers.ClothingManager;
import net.unitedlands.cosmetics.managers.VaultManager;
import net.unitedlands.cosmetics.storage.CosmeticsProfile;
import net.unitedlands.cosmetics.storage.EquippedClothing;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.unitedlands.UnitedLib;
import org.unitedlands.annotations.UnitedMenu;
import org.unitedlands.menu.UnitedMenuBuilder;
import org.unitedlands.menu.UnitedMenuHandler;
import org.unitedlands.menu.UnitedMenuItem;
import org.unitedlands.menu.UnitedMenuSize;

import java.util.*;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;
import static org.bukkit.Bukkit.getScheduler;

@UnitedMenu(
        title = "<shift:-48><glyph:wardrobe>",
        size = UnitedMenuSize.SIX_ROWS,
        allowPlayerInventory = true
)

public class WardrobeMenu implements UnitedMenuHandler {

    private final UnitedCosmetics plugin;
    private final ClothingManager clothingManager;
    private final VaultManager vaultManager;
    private final NamespacedKey typeKey;

    private final Map<UUID, String> viewingCategory = new HashMap<>();
    private final Map<UUID, Integer> viewingPage = new HashMap<>();

    private static final int[] VAULT_SLOTS = {
            14, 15, 16, 17, 23, 24, 25, 26, 32, 33, 34, 35, 41, 42, 43, 44
    };

    private static final int PREV_SLOT = 50;
    private static final int CLEAR_SLOT = 51;
    private static final int BACK_SLOT = 52;
    private static final int NEXT_SLOT = 53;


    public WardrobeMenu(UnitedCosmetics plugin, ClothingManager clothingManager, VaultManager vaultManager) {
        this.plugin = plugin;
        this.clothingManager = clothingManager;
        this.vaultManager = vaultManager;
        this.typeKey = new NamespacedKey("unitedcosmetics", "clothing_type");
    }

    @Override
    public void build(UnitedMenuBuilder menu) {
        Player player = menu.getPlayer();
        UUID uuid = player.getUniqueId();
        CosmeticsProfile profile = plugin.loadProfile(player);

        // Build left side (equipped items).
        buildSlot(menu, profile, "hat", 10, Material.AIR);
        buildSlot(menu, profile, "chest", 19, Material.AIR);
        buildSlot(menu, profile, "legs", 28, Material.AIR);
        buildSlot(menu, profile, "boots", 37, Material.AIR);

        for (int slot : VAULT_SLOTS) {
            menu.set(slot, UnitedMenuItem.of(Material.AIR));
        }
        menu.set(PREV_SLOT, UnitedMenuItem.of(Material.AIR));
        menu.set(NEXT_SLOT, UnitedMenuItem.of(Material.AIR));
        menu.set(CLEAR_SLOT, UnitedMenuItem.of(Material.AIR));
        menu.set(BACK_SLOT, UnitedMenuItem.of(Material.AIR));

        // Clear button,
        ItemStack clearIcon = createButton(
                "wardrobe.buttons.clear",
                "messages.wardrobe-button-name-clear"
        );
        menu.set(CLEAR_SLOT, UnitedMenuItem.of(clearIcon).onClick(_ -> {
            for (String type : List.of("hat", "chest", "legs", "boots")) {
                if (profile.hasEquippedClothing(type)) {
                    EquippedClothing equipped = profile.getEquippedClothing(type);
                    if (!equipped.isDigital()) {
                        player.getInventory().addItem(UnitedLib.getInstance().getItemFactory().getItemStack(equipped.nexoId(), 1));
                    }
                    profile.removeEquippedClothing(type);
                }
            }
            clothingManager.clearSpoof(player);
            clothingManager.removeBodyCosmetic(player);
            player.updateInventory();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            build(menu);
        }));

        // Back button.
        boolean isInsideCategory = viewingCategory.containsKey(uuid);
        ItemStack backIcon = createButton("wardrobe.buttons.back", "messages.wardrobe-button-name-back");
        menu.set(BACK_SLOT, UnitedMenuItem.of(backIcon).onClick(_ -> {
            if (isInsideCategory) {
                viewingCategory.remove(uuid);
                viewingPage.put(uuid, 0);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                build(menu);
            } else {
                String rootCommand = plugin.getConfig().getString("wardrobe.back-command");
                player.closeInventory();
                if (rootCommand != null && !rootCommand.isEmpty()) {
                    getScheduler().runTask(plugin, () -> player.performCommand(rootCommand));
                }
            }
        }));

        // Group cached items into categories automatically.
        Map<String, List<ItemStack>> categories = new HashMap<>();
        for (ItemStack item : vaultManager.getCachedCosmetics()) {
            if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(typeKey, PersistentDataType.STRING)) {
                String type = item.getItemMeta().getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
                categories.computeIfAbsent(type, _ -> new ArrayList<>()).add(item);
            }
        }

        String currentCategory = viewingCategory.get(uuid);
        int page = viewingPage.getOrDefault(uuid, 0);

        if (currentCategory == null) {
            buildCategoryView(menu, player, uuid, categories, page);
        } else {
            buildItemView(menu, player, uuid, profile, categories.getOrDefault(currentCategory, new ArrayList<>()), page);
        }
    }

    private void buildCategoryView(UnitedMenuBuilder menu, Player player, UUID uuid, Map<String, List<ItemStack>> categories, int page) {
        List<String> categoryNames = new ArrayList<>(categories.keySet());
        categoryNames.sort(String::compareTo);

        int maxPages = (int) Math.ceil((double) categoryNames.size() / VAULT_SLOTS.length);
        if (page >= maxPages && maxPages > 0) page = maxPages - 1;

        int startIndex = page * VAULT_SLOTS.length;

        for (int i = 0; i < VAULT_SLOTS.length; i++) {
            int dataIndex = startIndex + i;
            if (dataIndex < categoryNames.size()) {
                String catName = categoryNames.get(dataIndex);
                List<ItemStack> catItems = categories.get(catName);

                ItemStack icon = catItems.getFirst().clone();
                var meta = icon.getItemMeta();

                String formattedName = catName.substring(0, 1).toUpperCase() + catName.substring(1);
                String nameTemplate = plugin.getConfig().getString("messages.wardrobe-category-name");
                meta.displayName(miniMessage().deserialize(Objects.requireNonNull(nameTemplate).replace("{category}", formattedName)).decoration(TextDecoration.ITALIC, false));

                long unlocked = catItems.stream()
                        .map(item -> UnitedLib.getInstance().getItemFactory().getId(item))
                        .filter(id -> player.hasPermission("unitedcosmetics.cosmetic." + id))
                        .count();

                List<Component> lore = new ArrayList<>();
                for (String line : plugin.getConfig().getStringList("messages.wardrobe-category-description")) {
                    String processed = line.replace("{unlocked}", String.valueOf(unlocked))
                            .replace("{total}", String.valueOf(catItems.size()));
                    lore.add(miniMessage().deserialize(processed).decoration(TextDecoration.ITALIC, false));
                }
                meta.lore(lore);
                icon.setItemMeta(meta);

                menu.set(VAULT_SLOTS[i], UnitedMenuItem.of(icon).onClick(_ -> {
                    viewingCategory.put(uuid, catName);
                    viewingPage.put(uuid, 0);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    build(menu);
                }));
            }
        }

        buildPagination(menu, player, uuid, page, maxPages);
    }

    private void buildItemView(UnitedMenuBuilder menu, Player player, UUID uuid, CosmeticsProfile profile, List<ItemStack> catItems, int page) {
        int maxPages = (int) Math.ceil((double) catItems.size() / VAULT_SLOTS.length);
        if (page >= maxPages && maxPages > 0) page = maxPages - 1;

        int startIndex = page * VAULT_SLOTS.length;

        for (int i = 0; i < VAULT_SLOTS.length; i++) {
            int dataIndex = startIndex + i;
            if (dataIndex < catItems.size()) {
                ItemStack vaultItem = catItems.get(dataIndex).clone();
                menu.set(VAULT_SLOTS[i], createCosmeticEntry(player, profile, vaultItem, menu));
            }
        }

        buildPagination(menu, player, uuid, page, maxPages);
    }

    private void buildPagination(UnitedMenuBuilder menu, Player player, UUID uuid, int page, int maxPages) {
        if (page > 0) {
            ItemStack prevIcon = createButton(
                    "wardrobe.buttons.previous",
                    "messages.wardrobe-button-name-previous"
            );

            menu.set(PREV_SLOT, UnitedMenuItem.of(prevIcon).onClick(_ -> {
                viewingPage.put(uuid, page - 1);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                build(menu);
            }));
        }

        if (page < maxPages - 1) {
            ItemStack nextIcon = createButton(
                    "wardrobe.buttons.next",
                    "messages.wardrobe-button-name-next"
            );

            menu.set(NEXT_SLOT, UnitedMenuItem.of(nextIcon).onClick(_ -> {
                viewingPage.put(uuid, page + 1);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                build(menu);
            }));
        }
    }

    private UnitedMenuItem createCosmeticEntry(Player player, CosmeticsProfile profile, ItemStack vaultItem, UnitedMenuBuilder menu) {
        String nexoId = UnitedLib.getInstance().getItemFactory().getId(vaultItem);
        boolean isUnlocked = player.hasPermission("unitedcosmetics.cosmetic." + nexoId);

        var meta = vaultItem.getItemMeta();
        List<Component> existingLore = meta.lore();
        List<Component> lore = existingLore != null ? new java.util.ArrayList<>(existingLore) : new java.util.ArrayList<>();

        if (!isUnlocked) {
            String suffix = plugin.getConfig().getString("messages.wardrobe-locked-item-suffix");
            if (suffix != null && !suffix.isEmpty()) {
                Component suffixComponent = miniMessage().deserialize(suffix).decoration(TextDecoration.ITALIC, false);
                meta.displayName(meta.itemName().append(suffixComponent).decoration(TextDecoration.ITALIC, false));
            }
            for (String line : plugin.getConfig().getStringList("messages.wardrobe-locked-item-description")) {
                lore.add(miniMessage().deserialize(line).decoration(TextDecoration.ITALIC, false));
            }
        } else {
            for (String line : plugin.getConfig().getStringList("messages.wardrobe-unlocked-item-description")) {
                lore.add(miniMessage().deserialize(line).decoration(TextDecoration.ITALIC, false));
            }
        }

        meta.lore(lore);
        vaultItem.setItemMeta(meta);

        return UnitedMenuItem.of(vaultItem).onClick(_ -> {
            if (!isUnlocked) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1.0f, 1.0f);
                return;
            }

            if (vaultItem.hasItemMeta() && vaultItem.getItemMeta().getPersistentDataContainer().has(typeKey, PersistentDataType.STRING)) {
                String cosmeticType = vaultItem.getItemMeta().getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);

                profile.setEquippedClothing(cosmeticType, nexoId, true);

                if ("hat".equals(cosmeticType)) {
                    clothingManager.refreshSpoof(player);
                } else if ("chest".equals(cosmeticType)) {
                    clothingManager.spawnBodyCosmetic(player, nexoId);
                }

                player.updateInventory();
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1.0f, 1.0f);
                build(menu);
            }
        });
    }

    private void buildSlot(UnitedMenuBuilder menu, CosmeticsProfile profile, String type, int slot, Material emptyIcon) {
        Player player = menu.getPlayer();

        // Equipped slots.
        if (profile.hasEquippedClothing(type)) {
            EquippedClothing equipped = profile.getEquippedClothing(type);
            ItemStack rawCosmeticItem = UnitedLib.getInstance().getItemFactory().getItemStack(equipped.nexoId(), 1);

            if (rawCosmeticItem != null) {
                ItemStack displayItem = rawCosmeticItem.clone();
                var meta = displayItem.getItemMeta();

                List<Component> existingLore = meta.lore();
                List<Component> lore = existingLore != null
                        ? new java.util.ArrayList<>(existingLore)
                        : new java.util.ArrayList<>();

                String typeText = equipped.isDigital()
                        ? "<white>ᴅɪɢɪᴛᴀʟ ᴄᴏꜱᴍᴇᴛɪᴄ</white>"
                        : "<white>ᴘʜʏꜱɪᴄᴀʟ ᴄᴏꜱᴍᴇᴛɪᴄ</white>";

                for (String line : plugin.getConfig().getStringList("messages.wardrobe-equipped-item-description")) {
                    String processedLine = line.replace("{cosmetic-type}", typeText);
                    lore.add(miniMessage().deserialize(processedLine).decoration(TextDecoration.ITALIC, false));
                }

                meta.lore(lore);
                displayItem.setItemMeta(meta);

                UnitedMenuItem menuItem = UnitedMenuItem.of(displayItem)
                        .onClick(event -> {
                            if (event.getCursor().getType().isAir()) {

                                if (!equipped.isDigital()) {
                                    event.getView().setCursor(rawCosmeticItem);
                                }

                                profile.removeEquippedClothing(type);

                                if (type.equals("hat")) {
                                    clothingManager.refreshSpoof(player);
                                } else if (type.equals("chest")) {
                                    clothingManager.removeBodyCosmetic(player);
                                }

                                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1.0f, 0.8f);
                                buildSlot(menu, profile, type, slot, emptyIcon);
                            }
                        });

                menu.set(slot, menuItem);
                return;
            }
        }

        // Empty placeholder slots.
        UnitedMenuItem placeholder = UnitedMenuItem.of(emptyIcon);
        placeholder.onClick(event -> {
            ItemStack cursorItem = event.getCursor();

                    if (!cursorItem.getType().isAir() && cursorItem.hasItemMeta() && cursorItem.getItemMeta().getPersistentDataContainer().has(typeKey, PersistentDataType.STRING)) {
                        String cosmeticType = cursorItem.getItemMeta().getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);

                        if (type.equalsIgnoreCase(cosmeticType)) {
                            String id = UnitedLib.getInstance().getItemFactory().getId(cursorItem);
                            profile.setEquippedClothing(type, id, false);

                            // Handles multi-item stacks to not delete them.
                            if (cursorItem.getAmount() > 1) {
                                cursorItem.setAmount(cursorItem.getAmount() - 1);
                                event.getView().setCursor(cursorItem);
                            } else {
                                event.getView().setCursor(null);
                            }

                            if (type.equals("hat")) {
                                clothingManager.refreshSpoof(player);
                            } else if (type.equals("chest")) {
                                clothingManager.spawnBodyCosmetic(player, id);
                            }

                            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1.0f, 1.0f);
                            buildSlot(menu, profile, type, slot, emptyIcon);
                        }
                    }
                });
        menu.set(slot, placeholder);
    }

    private ItemStack createButton(String configPath, String namePath) {
        String matString = plugin.getConfig().getString(configPath);
        ItemStack item;

        if (matString != null && matString.toLowerCase().startsWith("[nexo]")) {
            String nexoId = matString.substring(6).trim();
            item = UnitedLib.getInstance().getItemFactory().getItemStack(nexoId, 1);
        } else {
            Material mat = Material.matchMaterial(Objects.requireNonNull(matString));
            item = new ItemStack(Objects.requireNonNull(mat));
        }

        var meta = item.getItemMeta();
        if (meta != null) {
            String name = plugin.getConfig().getString(namePath);
            meta.displayName(miniMessage().deserialize(Objects.requireNonNull(name)).decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }
}