package net.unitedlands.cosmetics;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.unitedlands.cosmetics.commands.AdminCommands;
import net.unitedlands.cosmetics.commands.TabCompleter;
import net.unitedlands.cosmetics.commands.WardrobeCommands;
import net.unitedlands.cosmetics.listeners.ClothingListener;
import net.unitedlands.cosmetics.managers.ClothingManager;
import net.unitedlands.cosmetics.managers.VaultManager;
import net.unitedlands.cosmetics.storage.CosmeticsProfile;
import net.unitedlands.cosmetics.utils.CosmeticItemReader;
import net.unitedlands.cosmetics.utils.CosmeticsPlaceholders;
import net.unitedlands.cosmetics.utils.MessageProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.unitedlands.utils.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UnitedCosmetics extends JavaPlugin {

    private static MessageProvider messageProvider;
    private final Map<UUID, CosmeticsProfile> activeProfiles = new HashMap<>();
    private ClothingManager clothingManager;
    private VaultManager vaultManager;
    private CosmeticItemReader itemReader;

    public ClothingManager getClothingManager() {
        return clothingManager;
    }

    public VaultManager getVaultManager() {
        return vaultManager;
    }

    public CosmeticItemReader getItemReader() {
        return itemReader;
    }

    @Override
    public void onEnable() {
        // Plugin startup logic.
        saveDefaultConfig();
        messageProvider = new MessageProvider(getConfig());

        this.clothingManager = new ClothingManager(this);
        this.vaultManager = new VaultManager(this);
        this.itemReader = new CosmeticItemReader(this);

        vaultManager.loadVaultItems();

        registerCommands();
        registerPlaceholders();
        getServer().getPluginManager().registerEvents(new ClothingListener(this, clothingManager), this);

        Logger.log("UnitedCosmetics has been enabled.", "UnitedCosmetics");
    }

    private void registerCommands() {
        TabCompleter tabCompleter = new TabCompleter(this);
        AdminCommands adminCommands = new AdminCommands(this, messageProvider, tabCompleter, vaultManager);
        WardrobeCommands wardrobeCommand = new WardrobeCommands(this, clothingManager, vaultManager);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands registrar = event.registrar();
            registrar.register(
                    "unitedcosmetics",
                    "Main command argument for UnitedCosmetics",
                    List.of("cosmetics"),
                    adminCommands
            );

        registrar.register(
                "wardrobe",
                "Open your cosmetics wardrobe",
                wardrobeCommand
        );
    });

    }

    private void registerPlaceholders() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new CosmeticsPlaceholders().register();
            Logger.log("PlaceholderAPI hooked successfully.", "UnitedCosmetics");
        } else {
            Logger.logWarning("PlaceholderAPI not found! Placeholders will not work.", "UnitedCosmetics");
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic.
        if (clothingManager != null) {
            clothingManager.cleanupAll();
        }
        Logger.log("UnitedCosmetics has been disabled.", "UnitedCosmetics");
    }

    public CosmeticsProfile loadProfile(Player player) {
        return activeProfiles.computeIfAbsent(player.getUniqueId(), uuid -> new CosmeticsProfile(player));
    }

    public void unloadProfile(Player player) {
        activeProfiles.remove(player.getUniqueId());
    }
}