package com.jotrorox.sleepyshop.command;

import com.jotrorox.sleepyshop.SleepyShop;
import com.jotrorox.sleepyshop.manager.ShopManager;
import com.jotrorox.sleepyshop.model.Shop;
import com.jotrorox.sleepyshop.util.ShopBlocks;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SleepyShopCommand implements TabExecutor {

    private static final Component PREFIX = Component.text(
        "[SleepyShop] ",
        NamedTextColor.BLUE
    );
    private static final int TARGET_DISTANCE = 6;

    private final SleepyShop plugin;
    private final ShopManager shopManager;

    public SleepyShopCommand(SleepyShop plugin, ShopManager shopManager) {
        this.plugin = plugin;
        this.shopManager = shopManager;
    }

    @Override
    public boolean onCommand(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String label,
        @NotNull String[] args
    ) {
        if (!sender.hasPermission("sleepyshop.admin")) {
            sender.sendMessage(
                PREFIX.append(
                    Component.text(
                        "You do not have permission to use this command.",
                        NamedTextColor.RED
                    )
                )
            );
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> handleReload(sender);
            case "list" -> handleList(sender, args);
            case "inspect" -> handleInspect(sender);
            case "remove" -> handleRemove(sender);
            default -> {
                sendHelp(sender, label);
                yield true;
            }
        };
    }

    @Override
    public @NotNull List<String> onTabComplete(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String alias,
        @NotNull String[] args
    ) {
        if (!sender.hasPermission("sleepyshop.admin")) {
            return List.of();
        }

        if (args.length == 1) {
            return filterMatches(
                args[0],
                List.of("inspect", "list", "reload", "remove")
            );
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("list")) {
            List<String> owners = shopManager
                .getShops()
                .values()
                .stream()
                .map(this::getOwnerName)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
            return filterMatches(args[1], owners);
        }

        return List.of();
    }

    private boolean handleReload(CommandSender sender) {
        plugin.reloadConfig();
        shopManager.refreshDisplays();
        sender.sendMessage(
            PREFIX.append(
                Component.text(
                    "Configuration reloaded and shop displays refreshed.",
                    NamedTextColor.GREEN
                )
            )
        );
        return true;
    }

    private boolean handleList(CommandSender sender, String[] args) {
        String ownerFilter = args.length > 1 ? args[1] : null;
        List<Shop> shops = new ArrayList<>(shopManager.getShops().values());
        shops.sort(
            Comparator
                .comparing(this::getOwnerName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(shop ->
                    formatLocation(shop.getSignLocation()).toLowerCase(
                        Locale.ROOT
                    )
                )
        );

        if (ownerFilter != null && !ownerFilter.isBlank()) {
            shops =
                shops
                    .stream()
                    .filter(shop ->
                        getOwnerName(shop).equalsIgnoreCase(ownerFilter)
                    )
                    .toList();
        }

        if (shops.isEmpty()) {
            String message = ownerFilter == null || ownerFilter.isBlank()
                ? "No shops found."
                : "No shops found for owner '" + ownerFilter + "'.";
            sender.sendMessage(
                PREFIX.append(Component.text(message, NamedTextColor.YELLOW))
            );
            return true;
        }

        String header = ownerFilter == null || ownerFilter.isBlank()
            ? "Tracked shops: " + shops.size()
            : "Tracked shops for " + ownerFilter + ": " + shops.size();
        sender.sendMessage(
            PREFIX.append(Component.text(header, NamedTextColor.GREEN))
        );

        int previewCount = Math.min(10, shops.size());
        for (int i = 0; i < previewCount; i++) {
            Shop shop = shops.get(i);
            sender.sendMessage(
                Component.text("- ", NamedTextColor.DARK_GRAY)
                    .append(
                        Component.text(getOwnerName(shop), NamedTextColor.AQUA)
                    )
                    .append(
                        Component.text(" | ", NamedTextColor.DARK_GRAY)
                    )
                    .append(
                        Component.text(formatTrade(shop), NamedTextColor.WHITE)
                    )
                    .append(
                        Component.text(" | ", NamedTextColor.DARK_GRAY)
                    )
                    .append(
                        Component.text(
                            formatLocation(shop.getSignLocation()),
                            NamedTextColor.GRAY
                        )
                    )
            );
        }

        if (shops.size() > previewCount) {
            sender.sendMessage(
                PREFIX.append(
                    Component.text(
                        "Showing first " +
                            previewCount +
                            " shops. Refine with /sleepyshop list <owner>.",
                        NamedTextColor.YELLOW
                    )
                )
            );
        }

        return true;
    }

    private boolean handleInspect(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(
                PREFIX.append(
                    Component.text(
                        "Only players can inspect a targeted shop.",
                        NamedTextColor.RED
                    )
                )
            );
            return true;
        }

        Shop shop = findTargetedShop(player);
        if (shop == null) {
            return true;
        }

        sender.sendMessage(
            PREFIX.append(
                Component.text("Shop inspection", NamedTextColor.GREEN)
            )
        );
        sender.sendMessage(
            Component.text("Owner: ", NamedTextColor.GRAY)
                .append(Component.text(getOwnerName(shop), NamedTextColor.AQUA))
        );
        sender.sendMessage(
            Component.text("Trade: ", NamedTextColor.GRAY)
                .append(Component.text(formatTrade(shop), NamedTextColor.WHITE))
        );
        sender.sendMessage(
            Component.text("Available transactions: ", NamedTextColor.GRAY)
                .append(
                    Component.text(
                        String.valueOf(shopManager.getAvailableTransactions(shop)),
                        NamedTextColor.GOLD
                    )
                )
        );
        sender.sendMessage(
            Component.text("Display: ", NamedTextColor.GRAY)
                .append(
                    Component.text(
                        shop.isShowDisplay() ? "enabled" : "disabled",
                        shop.isShowDisplay()
                            ? NamedTextColor.GREEN
                            : NamedTextColor.RED
                    )
                )
        );
        sender.sendMessage(
            Component.text("Sign: ", NamedTextColor.GRAY)
                .append(
                    Component.text(
                        formatLocation(shop.getSignLocation()),
                        NamedTextColor.WHITE
                    )
                )
        );
        sender.sendMessage(
            Component.text("Container: ", NamedTextColor.GRAY)
                .append(
                    Component.text(
                        formatLocation(shop.getChestLocation()),
                        NamedTextColor.WHITE
                    )
                )
        );
        return true;
    }

    private boolean handleRemove(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(
                PREFIX.append(
                    Component.text(
                        "Only players can remove a targeted shop.",
                        NamedTextColor.RED
                    )
                )
            );
            return true;
        }

        Shop shop = findTargetedShop(player);
        if (shop == null) {
            return true;
        }

        Block signBlock = shop.getSignLocation().getBlock();
        if (signBlock.getState() instanceof Sign) {
            signBlock
                .getWorld()
                .dropItemNaturally(
                    signBlock.getLocation(),
                    ShopBlocks.getSignDropItem(signBlock.getType())
                );
            signBlock.setType(Material.AIR);
        }

        shopManager.removeShop(shop.getSignLocation());
        sender.sendMessage(
            PREFIX.append(
                Component.text(
                    "Removed shop owned by " + getOwnerName(shop) + ".",
                    NamedTextColor.YELLOW
                )
            )
        );
        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(
            PREFIX.append(
                Component.text("Available admin commands:", NamedTextColor.GREEN)
            )
        );
        sender.sendMessage(
            Component.text("/" + label + " reload", NamedTextColor.AQUA)
                .append(
                    Component.text(
                        " - reload config and refresh displays",
                        NamedTextColor.GRAY
                    )
                )
        );
        sender.sendMessage(
            Component.text("/" + label + " list [owner]", NamedTextColor.AQUA)
                .append(
                    Component.text(
                        " - list tracked shops",
                        NamedTextColor.GRAY
                    )
                )
        );
        sender.sendMessage(
            Component.text("/" + label + " inspect", NamedTextColor.AQUA)
                .append(
                    Component.text(
                        " - inspect the shop you are looking at",
                        NamedTextColor.GRAY
                    )
                )
        );
        sender.sendMessage(
            Component.text("/" + label + " remove", NamedTextColor.AQUA)
                .append(
                    Component.text(
                        " - remove the shop you are looking at",
                        NamedTextColor.GRAY
                    )
                )
        );
    }

    private @Nullable Shop findTargetedShop(Player player) {
        Block targetBlock = player.getTargetBlockExact(TARGET_DISTANCE);
        if (targetBlock == null) {
            player.sendMessage(
                PREFIX.append(
                    Component.text(
                        "Look at a shop sign or container first.",
                        NamedTextColor.RED
                    )
                )
            );
            return null;
        }

        Shop shop = shopManager.findShopByBlock(targetBlock);
        if (shop == null) {
            player.sendMessage(
                PREFIX.append(
                    Component.text(
                        "That block is not part of a shop.",
                        NamedTextColor.RED
                    )
                )
            );
        }

        return shop;
    }

    private String getOwnerName(Shop shop) {
        OfflinePlayer owner = Bukkit.getOfflinePlayer(shop.getOwner());
        return owner.getName() != null ? owner.getName() : "Unknown";
    }

    private String formatTrade(Shop shop) {
        return (
            shop.getOutputAmount() +
            "x " +
            formatItem(shop.getSellItem()) +
            " for " +
            shop.getTakeAmount() +
            "x " +
            formatItem(shop.getPaymentItem())
        );
    }

    private String formatItem(@Nullable ItemStack item) {
        return item != null ? item.getType().name() : "UNSET";
    }

    private String formatLocation(@Nullable Location location) {
        if (location == null || location.getWorld() == null) {
            return "unknown";
        }

        return (
            location.getWorld().getName() +
            " " +
            location.getBlockX() +
            ", " +
            location.getBlockY() +
            ", " +
            location.getBlockZ()
        );
    }

    private List<String> filterMatches(String input, List<String> values) {
        String normalizedInput = input.toLowerCase(Locale.ROOT);
        return values
            .stream()
            .filter(value ->
                value.toLowerCase(Locale.ROOT).startsWith(normalizedInput)
            )
            .collect(Collectors.toList());
    }
}
