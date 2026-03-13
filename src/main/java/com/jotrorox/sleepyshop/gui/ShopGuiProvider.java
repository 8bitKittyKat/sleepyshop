package com.jotrorox.sleepyshop.gui;

import com.jotrorox.sleepyshop.manager.ShopManager;
import com.jotrorox.sleepyshop.model.Shop;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ShopGuiProvider {

    public static final String OWNER_GUI_TITLE = "SleepyShop Configuration";
    public static final String PRICE_GUI_TITLE = "Shop Price Settings";
    public static final String ITEMS_GUI_TITLE = "Shop Item Settings";
    public static final String OTHER_GUI_TITLE = "Other Settings";
    public static final String BUYER_GUI_TITLE = "Purchase Item";

    private static final Component PREFIX = Component.text(
        "[SleepyShop] ",
        NamedTextColor.BLUE
    );
    private final ShopManager manager;

    public ShopGuiProvider(ShopManager manager) {
        this.manager = manager;
    }

    public void openOwnerGui(Player player, Shop shop) {
        Inventory inv = createBaseGui(shop, 27, OWNER_GUI_TITLE);

        inv.setItem(
            11,
            createGuiSpacer(
                Material.GOLD_INGOT,
                "Price Settings",
                NamedTextColor.GOLD,
                "Set output and take amounts"
            )
        );
        inv.setItem(
            13,
            createGuiSpacer(
                Material.CHEST,
                "Item Settings",
                NamedTextColor.AQUA,
                "Set sell and payment items"
            )
        );
        inv.setItem(
            15,
            createGuiSpacer(
                Material.REPEATER,
                "Other Settings",
                NamedTextColor.GRAY,
                "Under construction..."
            )
        );

        inv.setItem(
            26,
            createGuiSpacer(
                Material.BARRIER,
                "Disband Shop",
                NamedTextColor.RED
            )
        );

        player.openInventory(inv);
    }

    public void openPriceGui(Player player, Shop shop) {
        Inventory inv = createBaseGui(shop, 36, PRICE_GUI_TITLE);

        // Take Amount (Price) - Row 1
        inv.setItem(
            10,
            createGuiSpacer(
                Material.RED_STAINED_GLASS_PANE,
                "-10 Price",
                NamedTextColor.RED
            )
        );
        inv.setItem(
            11,
            createGuiSpacer(
                Material.PINK_STAINED_GLASS_PANE,
                "-1 Price",
                NamedTextColor.LIGHT_PURPLE
            )
        );

        ItemStack priceStatus = createGuiSpacer(
            Material.GOLD_INGOT,
            "Current Price: " + shop.getTakeAmount(),
            NamedTextColor.GOLD
        );
        ItemMeta priceMeta = priceStatus.getItemMeta();
        priceMeta.lore(
            List.of(
                Component.text(
                    "This is what buyers pay per purchase",
                    NamedTextColor.GRAY
                )
            )
        );
        priceStatus.setItemMeta(priceMeta);
        inv.setItem(13, priceStatus);

        inv.setItem(
            15,
            createGuiSpacer(
                Material.LIME_STAINED_GLASS_PANE,
                "+1 Price",
                NamedTextColor.GREEN
            )
        );
        inv.setItem(
            16,
            createGuiSpacer(
                Material.GREEN_STAINED_GLASS_PANE,
                "+10 Price",
                NamedTextColor.DARK_GREEN
            )
        );

        // Output Amount - Row 2
        inv.setItem(
            19,
            createGuiSpacer(
                Material.RED_STAINED_GLASS_PANE,
                "-10 Amount",
                NamedTextColor.RED
            )
        );
        inv.setItem(
            20,
            createGuiSpacer(
                Material.PINK_STAINED_GLASS_PANE,
                "-1 Amount",
                NamedTextColor.LIGHT_PURPLE
            )
        );

        ItemStack amountStatus = createGuiSpacer(
            Material.CHEST,
            "Current Amount: " + shop.getOutputAmount(),
            NamedTextColor.AQUA
        );
        ItemMeta amountMeta = amountStatus.getItemMeta();
        amountMeta.lore(
            List.of(
                Component.text(
                    "This is what buyers receive per purchase",
                    NamedTextColor.GRAY
                )
            )
        );
        amountStatus.setItemMeta(amountMeta);
        inv.setItem(22, amountStatus);

        inv.setItem(
            24,
            createGuiSpacer(
                Material.LIME_STAINED_GLASS_PANE,
                "+1 Amount",
                NamedTextColor.GREEN
            )
        );
        inv.setItem(
            25,
            createGuiSpacer(
                Material.GREEN_STAINED_GLASS_PANE,
                "+10 Amount",
                NamedTextColor.DARK_GREEN
            )
        );

        inv.setItem(31, createBackItem());
        player.openInventory(inv);
    }

    public void openItemsGui(Player player, Shop shop) {
        Inventory inv = createBaseGui(shop, 27, ITEMS_GUI_TITLE);

        // Sell Item
        ItemStack sellItemSlot =
            shop.getSellItem() != null
                ? shop.getSellItem().clone()
                : new ItemStack(Material.BARRIER);
        ItemMeta sellMeta = sellItemSlot.getItemMeta();
        sellMeta.displayName(
            Component.text("Item to Sell", NamedTextColor.AQUA).decoration(
                TextDecoration.ITALIC,
                false
            )
        );
        List<Component> sellLore = new ArrayList<>();
        if (shop.getSellItem() == null) {
            sellLore.add(Component.text("No item set!", NamedTextColor.RED));
        } else {
            sellLore.add(
                Component.text(
                    "Currently: " + shop.getSellItem().getType().name(),
                    NamedTextColor.GRAY
                )
            );
        }
        sellLore.add(
            Component.text(
                "Click with an item in cursor to set",
                NamedTextColor.DARK_GRAY
            )
        );
        sellMeta.lore(sellLore);
        sellItemSlot.setItemMeta(sellMeta);
        inv.setItem(11, sellItemSlot);

        // Payment Item
        ItemStack payItemSlot =
            shop.getPaymentItem() != null
                ? shop.getPaymentItem().clone()
                : new ItemStack(Material.BARRIER);
        ItemMeta payMeta = payItemSlot.getItemMeta();
        payMeta.displayName(
            Component.text(
                "Item to Accept as Payment",
                NamedTextColor.GOLD
            ).decoration(TextDecoration.ITALIC, false)
        );
        List<Component> payLore = new ArrayList<>();
        if (shop.getPaymentItem() == null) {
            payLore.add(Component.text("No item set!", NamedTextColor.RED));
        } else {
            payLore.add(
                Component.text(
                    "Currently: " + shop.getPaymentItem().getType().name(),
                    NamedTextColor.GRAY
                )
            );
        }
        payLore.add(
            Component.text(
                "Click with an item in cursor to set",
                NamedTextColor.DARK_GRAY
            )
        );
        payMeta.lore(payLore);
        payItemSlot.setItemMeta(payMeta);
        inv.setItem(15, payItemSlot);

        inv.setItem(18, createBackItem());
        player.openInventory(inv);
    }

    public void openOtherGui(Player player, Shop shop) {
        Inventory inv = createBaseGui(shop, 27, OTHER_GUI_TITLE);

        // Shop Name
        String currentName =
            shop.getShopName() != null ? shop.getShopName() : "None (Default)";
        inv.setItem(
            13,
            createGuiSpacer(
                Material.NAME_TAG,
                "Set Shop Name",
                NamedTextColor.GOLD,
                "Current: " + currentName,
                "Click to change name in chat"
            )
        );

        // Toggle Hologram
        Material holoMaterial = shop.isShowDisplay()
            ? Material.LIME_DYE
            : Material.GRAY_DYE;
        String holoStatus = shop.isShowDisplay() ? "Enabled" : "Disabled";
        inv.setItem(
            11,
            createGuiSpacer(
                holoMaterial,
                "Floating Text",
                NamedTextColor.YELLOW,
                "Status: " + holoStatus,
                "Click to toggle shop display"
            )
        );

        // Toggle Stock Warning
        Material stockMaterial = shop.isShowStockMessage()
            ? Material.PAPER
            : Material.BARRIER;
        String stockStatus = shop.isShowStockMessage() ? "Enabled" : "Disabled";
        inv.setItem(
            15,
            createGuiSpacer(
                stockMaterial,
                "Out of Stock Warning",
                NamedTextColor.YELLOW,
                "Status: " + stockStatus,
                "Click to toggle 'OUT OF STOCK' message"
            )
        );

        inv.setItem(18, createBackItem());
        player.openInventory(inv);
    }

    public void openBuyerGui(Player player, Shop shop) {
        if (shop.getSellItem() == null) {
            player.sendMessage(
                PREFIX.append(
                    Component.text(
                        "This shop is not configured yet!",
                        NamedTextColor.RED
                    )
                )
            );
            return;
        }

        Inventory inv = createBaseGui(shop, 27, BUYER_GUI_TITLE);
        ShopInventoryHolder holder = (ShopInventoryHolder) inv.getHolder();

        // Calculate max transactions based on chest stock
        int maxTransactions = manager.getAvailableTransactions(shop);
        if (maxTransactions == 0) {
            player.sendMessage(
                PREFIX.append(
                    Component.text(
                        "This shop is out of stock!",
                        NamedTextColor.RED
                    )
                )
            );
            return;
        }

        assert holder != null;
        holder.setTransactionCount(1);

        // Set up the GUI items
        setupBuyerGuiItems(inv, shop, 1, maxTransactions);

        player.openInventory(inv);
    }

    public void updateBuyerGui(
        Inventory inv,
        Shop shop,
        int transactionCount,
        int maxTransactions
    ) {
        setupBuyerGuiItems(inv, shop, transactionCount, maxTransactions);
    }

    private void setupBuyerGuiItems(
        Inventory inv,
        Shop shop,
        int transactionCount,
        int maxTransactions
    ) {
        // Fill with dark glass for clean look
        ItemStack filler = createGuiSpacer(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }

        // Transaction controls - centered in top row
        inv.setItem(
            2,
            createGuiSpacer(
                Material.RED_STAINED_GLASS_PANE,
                "-10",
                NamedTextColor.RED,
                "Decrease by 10"
            )
        );
        inv.setItem(
            3,
            createGuiSpacer(
                Material.ORANGE_STAINED_GLASS_PANE,
                "-1",
                NamedTextColor.GOLD,
                "Decrease by 1"
            )
        );

        // Transaction count display (paper with stack amount showing count)
        ItemStack countDisplay = new ItemStack(
            Material.PAPER,
            Math.min(64, transactionCount)
        );
        ItemMeta countMeta = countDisplay.getItemMeta();
        countMeta.displayName(
            Component.text(
                "× " + transactionCount + " Transactions",
                NamedTextColor.YELLOW
            )
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true)
        );
        List<Component> countLore = new ArrayList<>();
        countLore.add(Component.text(""));
        countLore.add(
            Component.text(
                "You will receive:",
                NamedTextColor.WHITE
            ).decoration(TextDecoration.ITALIC, false)
        );
        countLore.add(
            Component.text(
                "  " +
                    (shop.getOutputAmount() * transactionCount) +
                    "× " +
                    shop.getSellItem().getType().name(),
                NamedTextColor.AQUA
            ).decoration(TextDecoration.ITALIC, false)
        );
        countLore.add(Component.text(""));
        countLore.add(
            Component.text("You will pay:", NamedTextColor.WHITE).decoration(
                TextDecoration.ITALIC,
                false
            )
        );
        countLore.add(
            Component.text(
                "  " +
                    (shop.getTakeAmount() * transactionCount) +
                    "× " +
                    shop.getPaymentItem().getType().name(),
                NamedTextColor.GOLD
            ).decoration(TextDecoration.ITALIC, false)
        );
        countLore.add(Component.text(""));
        countLore.add(
            Component.text(
                "Stock: " + maxTransactions + " available",
                NamedTextColor.GRAY
            ).decoration(TextDecoration.ITALIC, false)
        );
        countMeta.lore(countLore);
        countDisplay.setItemMeta(countMeta);
        inv.setItem(4, countDisplay);

        inv.setItem(
            5,
            createGuiSpacer(
                Material.LIME_STAINED_GLASS_PANE,
                "+1",
                NamedTextColor.GREEN,
                "Increase by 1"
            )
        );
        inv.setItem(
            6,
            createGuiSpacer(
                Material.GREEN_STAINED_GLASS_PANE,
                "+10",
                NamedTextColor.DARK_GREEN,
                "Increase by 10"
            )
        );

        // Item display - middle row, centered
        ItemStack displayItem = shop.getSellItem().clone();
        displayItem.setAmount(shop.getOutputAmount());
        ItemMeta meta = displayItem.getItemMeta();
        List<Component> lore = meta.hasLore()
            ? new ArrayList<>(Objects.requireNonNull(meta.lore()))
            : new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(
            Component.text(
                "▸ You get " + shop.getOutputAmount() + " per transaction",
                NamedTextColor.AQUA
            ).decoration(TextDecoration.ITALIC, false)
        );
        lore.add(
            Component.text(
                "▸ Price: " +
                    shop.getTakeAmount() +
                    "× " +
                    shop.getPaymentItem().getType().name(),
                NamedTextColor.GOLD
            ).decoration(TextDecoration.ITALIC, false)
        );
        meta.lore(lore);
        displayItem.setItemMeta(meta);
        inv.setItem(13, displayItem);

        // Confirm button - bottom row, centered
        ItemStack confirmButton = createGuiSpacer(
            Material.EMERALD_BLOCK,
            "✔ Confirm Purchase",
            NamedTextColor.GREEN,
            "",
            "Click to buy " +
                transactionCount +
                " transaction" +
                (transactionCount > 1 ? "s" : "")
        );
        inv.setItem(22, confirmButton);

        // Cancel button
        inv.setItem(
            18,
            createGuiSpacer(Material.BARRIER, "✖ Cancel", NamedTextColor.RED)
        );
    }
    private Inventory createBaseGui(Shop shop, int size, String title) {
        Inventory inv = Bukkit.createInventory(
            new ShopInventoryHolder(shop, title),
            size,
            Component.text(title)
        );
        ItemStack filler = createGuiSpacer(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < size; i++) {
            inv.setItem(i, filler);
        }
        return inv;
    }

    private ItemStack createBackItem() {
        return createGuiSpacer(Material.ARROW, "Back", NamedTextColor.RED);
    }

    private ItemStack createGuiSpacer(Material material) {
        return createGuiSpacer(material, " ", NamedTextColor.WHITE);
    }

    private ItemStack createGuiSpacer(
        Material material,
        String name,
        NamedTextColor color,
        String... loreLines
    ) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(
            Component.text(name, color).decoration(TextDecoration.ITALIC, false)
        );
        if (loreLines.length > 0) {
            List<Component> lore = new ArrayList<>();
            for (String line : loreLines) {
                lore.add(
                    Component.text(line, NamedTextColor.GRAY).decoration(
                        TextDecoration.ITALIC,
                        false
                    )
                );
            }
            meta.lore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }
}
