package com.jotrorox.sleepyshop.manager;

import com.jotrorox.sleepyshop.SleepyShop;
import com.jotrorox.sleepyshop.model.Shop;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class ShopManager {

    private final JavaPlugin plugin;
    private final Map<String, Shop> shops = new HashMap<>();

    public ShopManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadShops();
    }

    private void loadShops() {
        ((SleepyShop) plugin).getDatabaseManager()
            .loadShops()
            .thenAccept(loadedShops ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    for (Shop shop : loadedShops) {
                        shops.put(
                            locationToString(shop.getSignLocation()),
                            shop
                        );
                        updateDisplay(shop);
                    }
                })
            );
    }

    public void saveShop(Shop shop) {
        String id = locationToString(shop.getSignLocation());
        shops.put(id, shop);
        ((SleepyShop) plugin).getDatabaseManager()
            .saveShop(shop)
            .thenRun(() ->
                Bukkit.getScheduler().runTask(plugin, () -> updateDisplay(shop))
            );
    }

    public void removeShop(Location signLoc) {
        String id = locationToString(signLoc);
        Shop shop = shops.remove(id);
        if (shop != null && shop.getDisplayEntityId() != null) {
            Entity entity = Bukkit.getEntity(shop.getDisplayEntityId());
            if (entity != null) entity.remove();
        }
        ((SleepyShop) plugin).getDatabaseManager().removeShop(signLoc);
    }

    public void updateDisplay(Shop shop) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> updateDisplay(shop));
            return;
        }

        if (
            shop.getChestLocation() == null ||
            shop.getChestLocation().getWorld() == null
        ) return;

        Location loc = shop.getChestLocation().clone().add(0.5, 1.2, 0.5);
        if (!loc.isChunkLoaded()) return;

        if (!shop.isShowDisplay()) {
            if (shop.getDisplayEntityId() != null) {
                Entity entity = Bukkit.getEntity(shop.getDisplayEntityId());
                if (entity != null) entity.remove();
                shop.setDisplayEntityId(null);
                ((SleepyShop) plugin).getDatabaseManager().saveShop(shop);
            }
            return;
        }

        TextDisplay display = null;

        if (shop.getDisplayEntityId() != null) {
            Entity entity = Bukkit.getEntity(shop.getDisplayEntityId());
            if (entity instanceof TextDisplay td) {
                display = td;
            }
        }

        if (display == null) {
            // Search for an existing display at the location to avoid duplicates
            for (Entity nearby : loc
                .getWorld()
                .getNearbyEntities(loc, 0.1, 0.1, 0.1)) {
                if (nearby instanceof TextDisplay td) {
                    display = td;
                    shop.setDisplayEntityId(display.getUniqueId());
                    ((SleepyShop) plugin).getDatabaseManager().saveShop(shop);
                    break;
                }
            }
        }

        if (display == null) {
            display = loc.getWorld().spawn(loc, TextDisplay.class);
            shop.setDisplayEntityId(display.getUniqueId());
            display.setBillboard(Display.Billboard.CENTER);

            // Save the displayId immediately
            ((SleepyShop) plugin).getDatabaseManager().saveShop(shop);
        } else {
            display.teleport(loc);
        }

        String ownerName = Bukkit.getOfflinePlayer(shop.getOwner()).getName();
        if (ownerName == null) ownerName = "Unknown";

        String shopName = shop.getShopName();
        String sellItemName =
            shop.getSellItem() != null
                ? shop.getSellItem().getType().name()
                : "None";
        String payItemName =
            shop.getPaymentItem() != null
                ? shop.getPaymentItem().getType().name()
                : "None";

        // Stock check
        boolean outOfStock = false;
        Inventory shopInventory = getInventory(shop);
        if (
            shop.getSellItem() != null &&
            (shopInventory == null ||
                !shopInventory.containsAtLeast(
                    shop.getSellItem(),
                    shop.getOutputAmount()
                ))
        ) {
            outOfStock = true;
        }

        FileConfiguration pluginConfig = plugin.getConfig();
        MiniMessage mm = MiniMessage.miniMessage();

        String titleStr;
        if (shopName != null && !shopName.isEmpty()) {
            titleStr = pluginConfig
                .getString(
                    "shop-display.custom-title",
                    "<gold><b>{shopname}</b>"
                )
                .replace("{shopname}", shopName);
        } else {
            titleStr = pluginConfig
                .getString("shop-display.title", "<gold><b>{owner}'s Shop</b>")
                .replace("{owner}", ownerName);
        }

        Component title = mm.deserialize(titleStr);
        Component selling = mm.deserialize(
            pluginConfig
                .getString(
                    "shop-display.selling",
                    "<white>Selling: <green>{amount}x {item}"
                )
                .replace("{amount}", String.valueOf(shop.getOutputAmount()))
                .replace("{item}", sellItemName)
        );
        Component price = mm.deserialize(
            pluginConfig
                .getString(
                    "shop-display.price",
                    "<white>Price: <aqua>{price}x {payitem}"
                )
                .replace("{price}", String.valueOf(shop.getTakeAmount()))
                .replace("{payitem}", payItemName)
        );

        Component text = title
            .append(Component.newline())
            .append(selling)
            .append(Component.newline())
            .append(price);

        if (outOfStock && shop.isShowStockMessage()) {
            text = text
                .append(Component.newline())
                .append(
                    mm.deserialize(
                        pluginConfig.getString(
                            "shop-display.out-of-stock",
                            "<red><b>OUT OF STOCK</b>"
                        )
                    )
                );
        }

        display.text(text);
    }

    public Shop getShop(Location signLoc) {
        return shops.get(locationToString(signLoc));
    }

    public Map<String, Shop> getShops() {
        return shops;
    }

    public Inventory getInventory(Shop shop) {
        if (shop.getChestLocation() == null) {
            return null;
        }

        Block block = shop.getChestLocation().getBlock();
        if (block.getState() instanceof Container container) {
            return container.getInventory();
        }

        return null;
    }

    public int getAvailableTransactions(Shop shop) {
        if (shop.getSellItem() == null || shop.getOutputAmount() <= 0) {
            return 0;
        }

        Inventory inventory = getInventory(shop);
        if (inventory == null) {
            return 0;
        }

        int totalItems = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.isSimilar(shop.getSellItem())) {
                totalItems += item.getAmount();
            }
        }

        return totalItems / shop.getOutputAmount();
    }

    public boolean isShopSign(Location loc) {
        return shops.containsKey(locationToString(loc));
    }

    public Shop findShopByBlock(Block block) {
        if (isShopSign(block.getLocation())) {
            return getShop(block.getLocation());
        }

        if (!isSupportedContainer(block.getType())) {
            return null;
        }

        for (Shop shop : shops.values()) {
            if (
                shop.getChestLocation() != null &&
                isSameContainer(shop.getChestLocation(), block)
            ) {
                return shop;
            }
        }

        return null;
    }

    private String locationToString(Location loc) {
        if (loc == null || loc.getWorld() == null) return "null";
        return (
            loc.getWorld().getName() +
            "_" +
            loc.getBlockX() +
            "_" +
            loc.getBlockY() +
            "_" +
            loc.getBlockZ()
        );
    }

    public boolean isShopBlock(Block block) {
        return findShopByBlock(block) != null;
    }

    public void refreshDisplays() {
        for (Shop shop : shops.values()) {
            updateDisplay(shop);
        }
    }

    private boolean isSupportedContainer(Material type) {
        return (
            type == Material.CHEST ||
            type == Material.TRAPPED_CHEST ||
            type == Material.BARREL
        );
    }

    private boolean isSameContainer(Location shopChestLoc, Block targetBlock) {
        // Direct match
        if (shopChestLoc.equals(targetBlock.getLocation())) return true;

        // Double chest logic
        if (targetBlock.getState() instanceof Chest chest) {
            InventoryHolder holder = chest.getInventory().getHolder();
            if (holder instanceof DoubleChest doubleChest) {
                // Check both sides of the double chest against the saved shop location
                Location leftLoc = (
                    (Chest) Objects.requireNonNull(doubleChest.getLeftSide())
                ).getLocation();
                Location rightLoc = (
                    (Chest) Objects.requireNonNull(doubleChest.getRightSide())
                ).getLocation();

                return (
                    shopChestLoc.equals(leftLoc) ||
                    shopChestLoc.equals(rightLoc)
                );
            }
        }
        return false;
    }
}
