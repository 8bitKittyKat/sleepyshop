package com.jotrorox.sleepyshop.database;

import com.jotrorox.sleepyshop.model.Shop;
import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class DatabaseManager {

    private final JavaPlugin plugin;
    private Connection connection;
    private final ExecutorService executor;
    private static final String SHOP_TABLE_DDL =
        "CREATE TABLE IF NOT EXISTS shops (" +
        "sign_location TEXT PRIMARY KEY," +
        "chest_location TEXT NOT NULL," +
        "owner TEXT NOT NULL," +
        "sell_item TEXT," +
        "payment_item TEXT," +
        "take_amount INTEGER," +
        "output_amount INTEGER," +
        "shop_name TEXT," +
        "show_display BOOLEAN," +
        "show_itemdisplay BOOLEAN," +
        "show_stock_message BOOLEAN," +
        "display_entity_id TEXT," +
        "itemdisplay_entity_id TEXT" +
        ");";

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.executor = Executors.newSingleThreadExecutor();
        initDatabase();
    }

    public void upgradeDatabase() {
        try {
            // Check for "show_itemdisplay" column
            ResultSet rs = connection.getMetaData().getColumns(null, null, "SHOPS", "SHOW_ITEMDISPLAY");
            if (!rs.next()) {
                try (Statement stmt = connection.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE shops ADD COLUMN show_itemdisplay BOOLEAN");
                    plugin.getLogger().info("Added column 'show_itemdisplay' to 'shops' table.");
                }
            } else {
                plugin.getLogger().info("Column 'show_itemdisplay' already exists.");
            }
            rs.close();

            // Check for "itemdisplay_entity_id" column
            rs = connection.getMetaData().getColumns(null, null, "SHOPS", "ITEMDISPLAY_ENTITY_ID");
            if (!rs.next()) {
                try (Statement stmt = connection.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE shops ADD COLUMN itemdisplay_entity_id TEXT");
                    plugin.getLogger().info("Added column 'itemdisplay_entity_id' to 'shops' table.");
                }
            } else {
                plugin.getLogger().info("Column 'itemdisplay_entity_id' already exists.");
            }
            rs.close();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error during schema migration: " + e.getMessage());
        }
    }

    private void initDatabase() {
        try {
            try {
                Class.forName("org.h2.Driver");
            } catch (ClassNotFoundException e) {
                plugin.getLogger().severe("H2 driver not found on classpath!");
                return;
            }
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                var ignored = dataFolder.mkdirs();
            }
            File h2BaseFile = new File(dataFolder, "shops");
            File sqliteFile = new File(dataFolder, "shops.db");
            connection = DriverManager.getConnection(
                "jdbc:h2:file:" +
                h2BaseFile.getAbsolutePath() +
                ";AUTO_SERVER=FALSE;DATABASE_TO_UPPER=FALSE"
            );

            try (Statement statement = connection.createStatement()) {
                statement.execute(SHOP_TABLE_DDL);
            }

            upgradeDatabase();

            if (sqliteFile.exists()) {
                plugin.getLogger().severe("You are using the old db system please update using the 0.5 release or contact the authors!");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not initialize H2 database!");
            plugin.getLogger().severe("H2 init error: " + e.getMessage());
        }
    }

    public void closeConnection() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                plugin
                    .getLogger()
                    .warning("Timed out waiting for database tasks to finish.");
                executor.shutdownNow();
                executor.awaitTermination(5, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
            plugin
                .getLogger()
                .warning("Interrupted while waiting for database tasks to finish.");
        }

        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin
                .getLogger()
                .severe("Could not close H2 database!");
        }
    }

    private String serializeLocation(Location loc) {
        if (loc == null) return null;
        return (
            loc.getWorld().getName() +
            ":" +
            loc.getX() +
            ":" +
            loc.getY() +
            ":" +
            loc.getZ() +
            ":" +
            loc.getYaw() +
            ":" +
            loc.getPitch()
        );
    }

    private Location deserializeLocation(String s) {
        if (s == null) return null;
        String[] parts = s.split(":");
        if (parts.length < 6) return null;
        org.bukkit.World world = Bukkit.getWorld(parts[0]);
        if (world == null) return null;
        return new Location(
            world,
            Double.parseDouble(parts[1]),
            Double.parseDouble(parts[2]),
            Double.parseDouble(parts[3]),
            Float.parseFloat(parts[4]),
            Float.parseFloat(parts[5])
        );
    }

    private String serializeItemStack(ItemStack item) {
        if (item == null) return null;
        YamlConfiguration config = new YamlConfiguration();
        config.set("item", item);
        return config.saveToString();
    }

    private ItemStack deserializeItemStack(String s) {
        if (s == null) return null;
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(s);
            return config.getItemStack("item");
        } catch (Exception e) {
            return null;
        }
    }

    public CompletableFuture<Void> saveShop(Shop shop) {
        if (connection == null) return CompletableFuture.completedFuture(null);
        return CompletableFuture.runAsync(
            () -> {
                String sql =
                    "MERGE INTO shops (sign_location, chest_location, owner, sell_item, payment_item, " +
                    "take_amount, output_amount, shop_name, show_display, show_itemdisplay, show_stock_message, display_entity_id, itemdisplay_entity_id) " +
                    "KEY(sign_location) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (
                    PreparedStatement pstmt = connection.prepareStatement(sql)
                ) {
                    pstmt.setString(
                        1,
                        serializeLocation(shop.getSignLocation())
                    );
                    pstmt.setString(
                        2,
                        serializeLocation(shop.getChestLocation())
                    );
                    pstmt.setString(3, shop.getOwner().toString());
                    pstmt.setString(4, serializeItemStack(shop.getSellItem()));
                    pstmt.setString(
                        5,
                        serializeItemStack(shop.getPaymentItem())
                    );
                    pstmt.setInt(6, shop.getTakeAmount());
                    pstmt.setInt(7, shop.getOutputAmount());
                    pstmt.setString(8, shop.getShopName());
                    pstmt.setBoolean(9, shop.isShowDisplay());
                    pstmt.setBoolean(10, shop.isShowItemDisplay());
                    pstmt.setBoolean(11, shop.isShowStockMessage());
                    pstmt.setString(
                        12,
                        shop.getDisplayEntityId() != null
                            ? shop.getDisplayEntityId().toString()
                            : null
                    );
                    
                    pstmt.setString(
                        13,
                        shop.getItemDisplayEntityId() != null
                            ? shop.getItemDisplayEntityId().toString()
                            : null
                    );
                    pstmt.executeUpdate();
                } catch (SQLException e) {
                    plugin.getLogger().severe("Could not save shop to H2!");
                    plugin.getLogger().severe(e.getMessage());
                }
            },
            executor
        );
    }

    public void removeShop(Location signLoc) {
        if (connection == null) {
            CompletableFuture.completedFuture(null);
            return;
        }
        CompletableFuture.runAsync(
            () -> {
                String sql = "DELETE FROM shops WHERE sign_location = ?";
                try (
                    PreparedStatement pstmt = connection.prepareStatement(sql)
                ) {
                    pstmt.setString(1, serializeLocation(signLoc));
                    pstmt.executeUpdate();
                } catch (SQLException e) {
                    plugin
                        .getLogger()
                        .severe("Could not remove shop from H2!");
                }
            },
            executor
        );
    }

    public CompletableFuture<List<Shop>> loadShops() {
        if (connection == null) return CompletableFuture.completedFuture(
            new ArrayList<>()
        );
        return CompletableFuture.supplyAsync(
            () -> {
                List<Shop> shops = new ArrayList<>();
                String sql = "SELECT * FROM shops";
                try (
                    Statement stmt = connection.createStatement();
                    ResultSet rs = stmt.executeQuery(sql)
                ) {
                    while (rs.next()) {
                        Location signLoc = deserializeLocation(
                            rs.getString("sign_location")
                        );
                        Location chestLoc = deserializeLocation(
                            rs.getString("chest_location")
                        );
                        if (signLoc == null || chestLoc == null) continue;

                        UUID owner = UUID.fromString(rs.getString("owner"));

                        Shop shop = new Shop(signLoc, chestLoc, owner);
                        shop.setSellItem(
                            deserializeItemStack(rs.getString("sell_item"))
                        );
                        shop.setPaymentItem(
                            deserializeItemStack(rs.getString("payment_item"))
                        );
                        shop.setTakeAmount(rs.getInt("take_amount"));
                        shop.setOutputAmount(rs.getInt("output_amount"));
                        shop.setShopName(rs.getString("shop_name"));
                        shop.setShowDisplay(rs.getBoolean("show_display"));
                        shop.setShowItemDisplay(rs.getBoolean("show_itemdisplay"));
                        shop.setShowStockMessage(
                            rs.getBoolean("show_stock_message")
                        );
                        String displayId = rs.getString("display_entity_id");
                        if (displayId != null) {
                            shop.setDisplayEntityId(UUID.fromString(displayId));
                        }
                        String itemdisplayId = rs.getString("itemdisplay_entity_id");
                        if (itemdisplayId != null) {
                            shop.setItemDisplayEntityId(UUID.fromString(itemdisplayId));
                        }
                        
                        shops.add(shop);
                    }
                } catch (SQLException e) {
                    plugin
                        .getLogger()
                        .severe("Could not load shops from H2!");
            }
            return shops;
        },
        executor
        );
    }
}
