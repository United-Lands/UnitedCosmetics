package net.unitedlands.cosmetics.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.unitedlands.cosmetics.UnitedCosmetics;
import net.unitedlands.cosmetics.managers.ClothingManager;
import net.unitedlands.cosmetics.managers.VaultManager;
import net.unitedlands.cosmetics.menus.WardrobeMenu;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class WardrobeCommands implements BasicCommand {

    private final UnitedCosmetics plugin;
    private final ClothingManager clothingManager;
    private final VaultManager vaultManager;

    public WardrobeCommands(UnitedCosmetics plugin, ClothingManager clothingManager, VaultManager vaultManager) {
        this.plugin = plugin;
        this.clothingManager = clothingManager;
        this.vaultManager = vaultManager;
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String @NotNull [] args) {
        if (stack.getSender() instanceof Player player) {
            new WardrobeMenu(plugin, clothingManager, vaultManager).open(player);
        } else {
            stack.getSender().sendMessage("Only players can open the wardrobe.");
        }
    }
}