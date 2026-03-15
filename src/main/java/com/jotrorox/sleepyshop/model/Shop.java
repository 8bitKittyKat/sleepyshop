package com.jotrorox.sleepyshop.model;

import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class Shop {

    private final Location signLocation;
    private final Location chestLocation;
    private final UUID owner;
    private ItemStack sellItem;
    private ItemStack paymentItem;
    private int takeAmount;
    private int outputAmount;
    private UUID displayEntityId;
    private UUID itemdisplayEntityId;
    private String shopName;
    private boolean showDisplay = true;
    private boolean showItemDisplay = false;
    private boolean showStockMessage = true;

    public Shop(Location signLocation, Location chestLocation, UUID owner) {
        this.signLocation = signLocation;
        this.chestLocation = chestLocation;
        this.owner = owner;
        this.takeAmount = 1;
        this.outputAmount = 1;
        this.paymentItem = new ItemStack(Material.DIAMOND);
    }

    public Location getSignLocation() {
        return signLocation;
    }

    public Location getChestLocation() {
        return chestLocation;
    }

    public UUID getOwner() {
        return owner;
    }

    public ItemStack getSellItem() {
        return sellItem;
    }

    public void setSellItem(ItemStack item) {
        this.sellItem = item;
    }

    public ItemStack getPaymentItem() {
        return paymentItem;
    }

    public void setPaymentItem(ItemStack paymentItem) {
        this.paymentItem = paymentItem;
    }

    public int getTakeAmount() {
        return takeAmount;
    }

    public void setTakeAmount(int takeAmount) {
        this.takeAmount = takeAmount;
    }

    public int getOutputAmount() {
        return outputAmount;
    }

    public void setOutputAmount(int amount) {
        this.outputAmount = amount;
    }

    public UUID getDisplayEntityId() {
        return displayEntityId;
    }

    public void setDisplayEntityId(UUID displayEntityId) {
        this.displayEntityId = displayEntityId;
    }

    public UUID getItemDisplayEntityId() {
        return itemdisplayEntityId;
    }

    public void setItemDisplayEntityId(UUID itemdisplayEntityId) {
        this.itemdisplayEntityId = itemdisplayEntityId;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public boolean isShowDisplay() {
        return showDisplay;
    }

    public boolean isShowItemDisplay() {
        return showItemDisplay;
    }

    public void setShowDisplay(boolean showDisplay) {
        this.showDisplay = showDisplay;
    }

    public void setShowItemDisplay(boolean showItemDisplay) {
        this.showItemDisplay = showItemDisplay;
    }

    public boolean isShowStockMessage() {
        return showStockMessage;
    }

    public void setShowStockMessage(boolean showStockMessage) {
        this.showStockMessage = showStockMessage;
    }
}
