package com.jotrorox.sleepyshop.listener;

import com.jotrorox.sleepyshop.gui.ShopGuiProvider;
import com.jotrorox.sleepyshop.gui.ShopInventoryHolder;
import com.jotrorox.sleepyshop.manager.ShopManager;
import com.jotrorox.sleepyshop.model.Shop;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class ShopListener implements Listener {

    private final ShopManager manager;
    private final ShopGuiProvider guiProvider;
    private final Map<UUID, Shop> namingSession = new HashMap<>();
    private static final Component PREFIX = Component.text(
        "[SleepyShop] ",
        NamedTextColor.BLUE
    );

    public ShopListener(ShopManager manager) {
        this.manager = manager;
        this.guiProvider = new ShopGuiProvider(manager);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        manager
            .getShops()
            .values()
            .stream()
            .filter(shop ->
                shop.getChestLocation().getWorld().equals(event.getWorld())
            )
            .filter(
                shop ->
                    shop.getChestLocation().getBlockX() >> 4 ==
                        event.getChunk().getX() &&
                    shop.getChestLocation().getBlockZ() >> 4 ==
                    event.getChunk().getZ()
            )
            .forEach(manager::updateDisplay);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Location loc = block.getLocation();

        if (manager.isShopSign(loc)) {
            Shop shop = manager.getShop(loc);
            if (
                !event.getPlayer().getUniqueId().equals(shop.getOwner()) &&
                !event.getPlayer().hasPermission("sleepyshop.admin")
            ) {
                event.setCancelled(true);
                event
                    .getPlayer()
                    .sendMessage(
                        PREFIX.append(
                            Component.text(
                                "You cannot break this shop!",
                                NamedTextColor.RED
                            )
                        )
                    );
            } else {
                manager.removeShop(loc);
                event
                    .getPlayer()
                    .sendMessage(
                        PREFIX.append(
                            Component.text(
                                "SleepyShop removed.",
                                NamedTextColor.YELLOW
                            )
                        )
                    );
            }
            return;
        }

        if (manager.isShopBlock(block)) {
            Shop associatedShop = manager.findShopByBlock(block);

            if (associatedShop != null) {
                if (
                    !event
                        .getPlayer()
                        .getUniqueId()
                        .equals(associatedShop.getOwner()) &&
                    !event.getPlayer().hasPermission("sleepyshop.admin")
                ) {
                    event.setCancelled(true);
                    event
                        .getPlayer()
                        .sendMessage(
                            PREFIX.append(
                                Component.text(
                                    "This chest belongs to a shop!",
                                    NamedTextColor.RED
                                )
                            )
                        );
                } else {
                    // Drop the sign as an item before removing the shop
                    Block signBlock = associatedShop
                        .getSignLocation()
                        .getBlock();
                    if (signBlock.getState() instanceof Sign) {
                        Material signType = signBlock.getType();
                        ItemStack signItem = getSignDropItem(signType);
                        signBlock
                            .getWorld()
                            .dropItemNaturally(
                                    signBlock.getLocation(),
                                    signItem
                            );
                        signBlock.setType(Material.AIR);
                    }
                    manager.removeShop(associatedShop.getSignLocation());
                    event
                        .getPlayer()
                        .sendMessage(
                            PREFIX.append(
                                Component.text(
                                    "SleepyShop removed.",
                                    NamedTextColor.YELLOW
                                )
                            )
                        );
                }
            } else {
                event.setCancelled(true);
                event
                    .getPlayer()
                    .sendMessage(
                        PREFIX.append(
                            Component.text(
                                "This block is protected by a shop!",
                                NamedTextColor.RED
                            )
                        )
                    );
            }
        }
    }

    @EventHandler
    public void onChestAccess(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || !isChest(block)) return;

        if (manager.isShopBlock(block)) {
            Shop associatedShop = manager.findShopByBlock(block);

            if (associatedShop != null) {
                if (
                    !event
                        .getPlayer()
                        .getUniqueId()
                        .equals(associatedShop.getOwner()) &&
                    !event.getPlayer().hasPermission("sleepyshop.admin")
                ) {
                    event.setCancelled(true);
                    event
                        .getPlayer()
                        .sendMessage(
                            PREFIX.append(
                                Component.text(
                                    "This chest belongs to a shop!",
                                    NamedTextColor.RED
                                )
                            )
                        );
                }
            } else {
                if (event.getPlayer().hasPermission("sleepyshop.admin")) {
                    event.setCancelled(false);
                    return;
                }
                event.setCancelled(true);
                event
                    .getPlayer()
                    .sendMessage(
                        PREFIX.append(
                            Component.text(
                                "This block is protected by a shop!",
                                NamedTextColor.RED
                            )
                        )
                    );
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inv = event.getInventory();
        if (inv.getHolder() instanceof org.bukkit.block.Container container) {
            Block block = container.getBlock();
            if (isChest(block)) {
                Shop shop = manager.findShopByBlock(block);
                if (shop != null) {
                    manager.updateDisplay(shop);
                }
            }
        }
    }

    private boolean isChest(Block block) {
        return (
            block.getType() == Material.CHEST ||
            block.getType() == Material.TRAPPED_CHEST ||
            block.getType() == Material.BARREL
        );
    }

    @EventHandler
    public void onSignClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Sign)) return;

        Shop shop = manager.getShop(block.getLocation());
        if (shop != null) {
            event.setCancelled(true);
            if (event.getPlayer().getUniqueId().equals(shop.getOwner())) {
                guiProvider.openOwnerGui(event.getPlayer(), shop);
            } else {
                guiProvider.openBuyerGui(event.getPlayer(), shop);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (
            !(event.getInventory().getHolder() instanceof
                    ShopInventoryHolder holder)
        ) return;

        Shop shop = holder.shop();
        String title = holder.title();
        Player player = (Player) event.getWhoClicked();

        if (event.getClickedInventory() == event.getView().getTopInventory()) {
            event.setCancelled(true);
            if (title.equals(ShopGuiProvider.BUYER_GUI_TITLE)) {
                handleBuyerClick(event, player, shop, holder);
            } else {
                handleOwnerClick(event, player, shop, title);
            }
        } else {
            // Bottom inventory click
            if (title.equals(ShopGuiProvider.ITEMS_GUI_TITLE)) {
                // Allow moving items in own inventory but prevent shift-clicking into shop GUI
                if (
                    event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                ) {
                    event.setCancelled(true);
                }
            } else {
                event.setCancelled(true);
            }
        }
    }

    private void handleOwnerClick(
        InventoryClickEvent event,
        Player player,
        Shop shop,
        String title
    ) {
        int slot = event.getRawSlot();

        switch (title) {
            case ShopGuiProvider.OWNER_GUI_TITLE -> {
                if (slot == 11) guiProvider.openPriceGui(player, shop);
                else if (slot == 13) guiProvider.openItemsGui(player, shop);
                else if (slot == 15) guiProvider.openOtherGui(player, shop);
                else if (slot == 26) {
                    // Disband
                    manager.removeShop(shop.getSignLocation());
                    var signBlock = shop.getSignLocation().getBlock();
                    if (signBlock.getState() instanceof Sign) {
                        Material signType = signBlock.getType();
                        ItemStack signItem = getSignDropItem(signType);
                        signBlock.getWorld()
                                .dropItemNaturally(
                                        signBlock.getLocation(),
                                        signItem
                                );
                        signBlock.setType(Material.AIR);
                    }
                    player.closeInventory();
                    player.sendMessage(
                        PREFIX.append(
                            Component.text(
                                "SleepyShop disbanded.",
                                NamedTextColor.YELLOW
                            )
                        )
                    );
                }
            }
            case ShopGuiProvider.PRICE_GUI_TITLE -> {
                if (slot == 31) guiProvider.openOwnerGui(player, shop);
                // Price (Take Amount)
                else if (slot == 10) adjustTakeAmount(shop, -10, player);
                else if (slot == 11) adjustTakeAmount(shop, -1, player);
                else if (slot == 15) adjustTakeAmount(shop, 1, player);
                else if (slot == 16) adjustTakeAmount(shop, 10, player);
                // Amount (Output Amount)
                else if (slot == 19) adjustOutputAmount(shop, -10, player);
                else if (slot == 20) adjustOutputAmount(shop, -1, player);
                else if (slot == 24) adjustOutputAmount(shop, 1, player);
                else if (slot == 25) adjustOutputAmount(shop, 10, player);
            }
            case ShopGuiProvider.ITEMS_GUI_TITLE -> {
                if (slot == 18) guiProvider.openOwnerGui(player, shop);
                else if (slot == 11) {
                    // Sell Item
                    ItemStack cursor = event.getCursor();
                    if (cursor.getType() != Material.AIR) {
                        ItemStack shopItem = cursor.clone();
                        shopItem.setAmount(1);
                        shop.setSellItem(shopItem);
                        manager.saveShop(shop);
                        player.sendMessage(
                            PREFIX.append(
                                Component.text(
                                    "Sell item set to " +
                                        shopItem.getType().name(),
                                    NamedTextColor.GREEN
                                )
                            )
                        );
                        guiProvider.openItemsGui(player, shop);
                    }
                } else if (slot == 15) {
                    // Payment Item
                    ItemStack cursor = event.getCursor();
                    if (cursor.getType() != Material.AIR) {
                        ItemStack payItem = cursor.clone();
                        payItem.setAmount(1);
                        shop.setPaymentItem(payItem);
                        manager.saveShop(shop);
                        player.sendMessage(
                            PREFIX.append(
                                Component.text(
                                    "Payment item set to " +
                                        payItem.getType().name(),
                                    NamedTextColor.GREEN
                                )
                            )
                        );
                        guiProvider.openItemsGui(player, shop);
                    }
                }
            }
            case ShopGuiProvider.OTHER_GUI_TITLE -> {
                if (slot == 18) guiProvider.openOwnerGui(player, shop);
                else if (slot == 13) {
                    namingSession.put(player.getUniqueId(), shop);
                    player.closeInventory();
                    player.sendMessage(
                        PREFIX.append(
                            Component.text(
                                "Please enter the shop name in chat (type 'cancel' to stop or 'reset' for default):",
                                NamedTextColor.YELLOW
                            )
                        )
                    );
                } else if (slot == 11) {
                    // Toggle Hologram
                    shop.setShowDisplay(!shop.isShowDisplay());
                    manager.saveShop(shop);
                    guiProvider.openOtherGui(player, shop);
                } else if (slot == 15) {
                    // Toggle Stock Warning
                    shop.setShowStockMessage(!shop.isShowStockMessage());
                    manager.saveShop(shop);
                    guiProvider.openOtherGui(player, shop);
                }
            }
        }
    }

    private void adjustTakeAmount(Shop shop, int delta, Player player) {
        shop.setTakeAmount(Math.max(0, shop.getTakeAmount() + delta));
        manager.saveShop(shop);
        guiProvider.openPriceGui(player, shop);
    }

    private void adjustOutputAmount(Shop shop, int delta, Player player) {
        shop.setOutputAmount(
            Math.max(1, Math.min(64, shop.getOutputAmount() + delta))
        );
        manager.saveShop(shop);
        guiProvider.openPriceGui(player, shop);
    }

    private void handleBuyerClick(
        InventoryClickEvent event,
        Player player,
        Shop shop,
        ShopInventoryHolder holder
    ) {
        int slot = event.getRawSlot();

        switch (slot) {
            case 2 -> adjustTransactionCount(
                event.getInventory(),
                shop,
                holder,
                -10
            );
            case 3 -> adjustTransactionCount(
                event.getInventory(),
                shop,
                holder,
                -1
            );
            case 5 -> adjustTransactionCount(
                event.getInventory(),
                shop,
                holder,
                1
            );
            case 6 -> adjustTransactionCount(
                event.getInventory(),
                shop,
                holder,
                10
            );
            case 18 -> player.closeInventory(); // Cancel button
            case 22 -> performTransaction(
                player,
                shop,
                holder.getTransactionCount()
            );
        }
    }

    private void adjustTransactionCount(
        Inventory inv,
        Shop shop,
        ShopInventoryHolder holder,
        int delta
    ) {
        int maxTransactions = calculateMaxTransactions(shop);
        int newCount = Math.max(
            1,
            Math.min(maxTransactions, holder.getTransactionCount() + delta)
        );
        holder.setTransactionCount(newCount);
        guiProvider.updateBuyerGui(inv, shop, newCount, maxTransactions);
    }

    private int calculateMaxTransactions(Shop shop) {
        return manager.getAvailableTransactions(shop);
    }

    private void performTransaction(
        Player buyer,
        Shop shop,
        int transactionCount
    ) {
        if (shop.getSellItem() == null) return;

        Inventory chestInv = manager.getInventory(shop);
        if (chestInv == null) {
            buyer.sendMessage(
                PREFIX.append(
                    Component.text(
                        "Error: Shop container not found!",
                        NamedTextColor.RED
                    )
                )
            );
            return;
        }

        // Calculate total amounts needed
        int totalOutputItems = shop.getOutputAmount() * transactionCount;
        int totalPaymentAmount = shop.getTakeAmount() * transactionCount;

        // Check if chest has enough stock
        if (!chestInv.containsAtLeast(shop.getSellItem(), totalOutputItems)) {
            buyer.sendMessage(
                PREFIX.append(
                    Component.text(
                        "Shop doesn't have enough stock!",
                        NamedTextColor.RED
                    )
                )
            );
            return;
        }

        // Check buyer's payment
        ItemStack payment = shop.getPaymentItem().clone();
        payment.setAmount(totalPaymentAmount);
        if (
            totalPaymentAmount > 0 &&
            !buyer
                .getInventory()
                .containsAtLeast(shop.getPaymentItem(), totalPaymentAmount)
        ) {
            buyer.sendMessage(
                PREFIX.append(
                    Component.text(
                        "You don't have enough " +
                            payment.getType().name() +
                            "!",
                        NamedTextColor.RED
                    )
                )
            );
            return;
        }

        // Check if chest has enough space for payment items
        if (totalPaymentAmount > 0 && !hasSpaceForItems(chestInv, payment)) {
            buyer.sendMessage(
                PREFIX.append(
                    Component.text(
                        "Shop chest is full! Cannot accept payment.",
                        NamedTextColor.RED
                    )
                )
            );
            return;
        }

        // Transaction - take payment from buyer and add to chest
        if (totalPaymentAmount > 0) {
            buyer.getInventory().removeItem(payment);
            chestInv.addItem(payment);
        }

        // Remove items from chest and give to buyer
        ItemStack toGive = shop.getSellItem().clone();
        toGive.setAmount(totalOutputItems);
        chestInv.removeItem(toGive);
        buyer
            .getInventory()
            .addItem(toGive)
            .values()
            .forEach(item ->
                buyer.getWorld().dropItem(buyer.getLocation(), item)
            );

        String message =
            transactionCount > 1
                ? "Purchase successful! (" + transactionCount + " transactions)"
                : "Purchase successful!";
        buyer.sendMessage(
            PREFIX.append(Component.text(message, NamedTextColor.GREEN))
        );
        buyer.closeInventory();
        manager.updateDisplay(shop);
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        Shop shop = namingSession.remove(player.getUniqueId());
        if (shop == null) return;

        event.setCancelled(true);
        String name = PlainTextComponentSerializer.plainText().serialize(
            event.message()
        );

        if (name.equalsIgnoreCase("cancel")) {
            player.sendMessage(
                PREFIX.append(
                    Component.text("Shop naming cancelled.", NamedTextColor.RED)
                )
            );
            return;
        }

        if (name.equalsIgnoreCase("reset")) {
            shop.setShopName(null);
            manager.saveShop(shop);
            player.sendMessage(
                PREFIX.append(
                    Component.text(
                        "Shop name reset to default.",
                        NamedTextColor.GREEN
                    )
                )
            );
            return;
        }

        shop.setShopName(name);
        manager.saveShop(shop);
        player.sendMessage(
            PREFIX.append(
                Component.text(
                    "Shop name set to: " + name,
                    NamedTextColor.GREEN
                )
            )
        );
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        handleExplosion(event.blockList().iterator());
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        handleExplosion(event.blockList().iterator());
    }

    private void handleExplosion(Iterator<Block> iterator) {
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (manager.isShopBlock(block)) {
                iterator.remove();
            }
        }
    }

    @EventHandler
    public void onHopperTransfer(InventoryMoveItemEvent event) {
        if (isShopInventory(event.getSource())) {
            event.setCancelled(true);
            return;
        }

        if (isShopInventory(event.getDestination())) {
            event.setCancelled(true);
        }
    }

    private boolean isShopInventory(Inventory inventory) {
        InventoryHolder holder = inventory.getHolder();

        if (holder instanceof org.bukkit.block.Container container) {
            return manager.isShopBlock(container.getBlock());
        }

        if (holder instanceof org.bukkit.block.DoubleChest doubleChest) {
            if (
                doubleChest.getLeftSide() instanceof
                    org.bukkit.block.Container left
            ) {
                if (manager.isShopBlock(left.getBlock())) return true;
            }
            if (
                doubleChest.getRightSide() instanceof
                    org.bukkit.block.Container right
            ) {
                return manager.isShopBlock(right.getBlock());
            }
        }

        return false;
    }

    /**
     * Checks if an inventory has enough space to accept the given items.
     */
    private boolean hasSpaceForItems(Inventory inventory, ItemStack items) {
        int remaining = items.getAmount();
        int maxStackSize = items.getMaxStackSize();

        for (ItemStack slot : inventory.getContents()) {
            if (slot == null || slot.getType() == Material.AIR) {
                // Empty slot can hold up to maxStackSize
                remaining -= maxStackSize;
            } else if (slot.isSimilar(items)) {
                // Partial stack can hold more of the same item
                remaining -= (maxStackSize - slot.getAmount());
            }
            if (remaining <= 0) {
                return true;
            }
        }
        return remaining <= 0;
    }

    /**
     * Gets the appropriate sign item to drop based on the sign block type.
     */
    private ItemStack getSignDropItem(Material signType) {
        String name = signType.name();

        // Normalize wall variants to the base item
        if (name.endsWith("_WALL_HANGING_SIGN")) {
            name = name.replace("_WALL_HANGING_SIGN", "_HANGING_SIGN");
        } else if (name.endsWith("_WALL_SIGN")) {
            name = name.replace("_WALL_SIGN", "_SIGN");
        }

        Material base = Material.matchMaterial(name);
        if (
            base != null &&
            (base.name().endsWith("_SIGN") ||
                base.name().endsWith("_HANGING_SIGN"))
        ) {
            return new ItemStack(base);
        }

        return new ItemStack(Material.OAK_SIGN); // Fallback
    }
}
