package net.unitedlands.cosmetics.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.node.NodeType;
import net.unitedlands.cosmetics.UnitedCosmetics;
import net.unitedlands.cosmetics.managers.VaultManager;
import net.unitedlands.cosmetics.storage.CosmeticsProfile;
import net.unitedlands.cosmetics.utils.FeedbackProvider;
import net.unitedlands.cosmetics.utils.MessageProvider;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.unitedlands.utils.Messenger;

import java.util.Collection;
import java.util.Map;

public class AdminCommands implements BasicCommand {

    private final UnitedCosmetics plugin;
    private final MessageProvider messageProvider;
    private final TabCompleter tabCompleter;
    private final VaultManager vaultManager;
    private final PrefixCommands prefixCommands;
    private final ChatColourCommands chatColourCommands;

    public AdminCommands(UnitedCosmetics plugin, MessageProvider messageProvider, TabCompleter tabCompleter, VaultManager vaultManager) {
        this.plugin = plugin;
        this.messageProvider = messageProvider;
        this.tabCompleter = tabCompleter;
        this.vaultManager = vaultManager;
        this.prefixCommands = new PrefixCommands(plugin, messageProvider);
        this.chatColourCommands = new ChatColourCommands(plugin, messageProvider);
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String @NotNull [] args) {

        CommandSender sender = stack.getSender();
        String prefix = messageProvider.get("messages.prefix");

        if (args.length == 0)
            return;

        if (!sender.hasPermission("unitedcosmetics.admin")) {
            Messenger.sendMessage(sender, messageProvider.get("messages.no-permission"), null, prefix);
            return;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            messageProvider.reload(plugin.getConfig());
            vaultManager.reloadVault();
            Messenger.sendMessage(sender, messageProvider.get("messages.reload"), null, prefix);
            return;
        }

        // Delegate to PrefixCommands.
        if (args[0].equalsIgnoreCase("prefix")) {
            prefixCommands.execute(sender, args);
            return;
        }

        // Delegate to ChatColourCommands.
        if (args[0].equalsIgnoreCase("chatcolour")) {
            chatColourCommands.execute(sender, args);
            return;
        }

        if (args[0].equalsIgnoreCase("profile")) {
            if (args.length < 3) {
                FeedbackProvider.sendFeedback(sender, null, messageProvider.get("messages.admin-profile-usage"), null, prefix);
                return;
            }

            String action = args[1].toLowerCase();

            // Use LuckPerms as source of truth for UUID.
            java.util.UUID realUuid = LuckPermsProvider.get().getUserManager().lookupUniqueId(args[2]).join();

            if (realUuid == null) {
                FeedbackProvider.sendFeedback(sender, null, messageProvider.get("messages.unknown-player"), Map.of("player", args[2]), prefix);
                return;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(realUuid);
            String targetName = target.getName() != null ? target.getName() : args[2];
            CosmeticsProfile profile = new CosmeticsProfile(target);
            Map<String, String> placeholders = Map.of("player", targetName);

            switch (action) {
                case "create":
                    if (profile.hasFile()) {
                        FeedbackProvider.sendFeedback(sender, target, messageProvider.get("messages.profile-exists"), placeholders, prefix);
                        return;
                    }
                    profile.createFile();
                    FeedbackProvider.sendFeedback(sender, target, messageProvider.get("messages.profile-created"), placeholders, prefix);
                    break;

                case "delete":
                    if (!profile.hasFile()) {
                        FeedbackProvider.sendFeedback(sender, target, messageProvider.get("messages.profile-not-found"), placeholders, prefix);
                        return;
                    }
                    profile.deleteFile();

                    // Clear active prefix on profile delete.
                    LuckPermsProvider.get().getUserManager().modifyUser(target.getUniqueId(), user ->
                            user.data().clear(NodeType.PREFIX.predicate(node -> node.getPriority() == 100))
                    );

                    FeedbackProvider.sendFeedback(sender, target, messageProvider.get("messages.profile-deleted"), placeholders, prefix);
                    break;

                case "reset":
                    if (!profile.hasFile()) {
                        FeedbackProvider.sendFeedback(sender, target, messageProvider.get("messages.profile-not-found"), placeholders, prefix);
                        return;
                    }
                    profile.reset();

                    // Clear active prefix on profile reset
                    LuckPermsProvider.get().getUserManager().modifyUser(target.getUniqueId(), user ->
                            user.data().clear(NodeType.PREFIX.predicate(node -> node.getPriority() == 100))
                    );

                    FeedbackProvider.sendFeedback(sender, target, messageProvider.get("messages.profile-reset"), placeholders, prefix);
                    break;

                default:
                    FeedbackProvider.sendFeedback(sender, null, messageProvider.get("messages.admin-profile-usage"), null, prefix);
                    break;
            }
        }
    }

    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack stack, @NotNull String @NotNull [] args) {
        return tabCompleter.getSuggestions(stack.getSender(), args);
    }
}