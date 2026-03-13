package com.jotrorox.sleepyshop.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class ShopBlocks {

    private ShopBlocks() {}

    public static ItemStack getSignDropItem(Material signType) {
        String name = signType.name();

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

        return new ItemStack(Material.OAK_SIGN);
    }
}
