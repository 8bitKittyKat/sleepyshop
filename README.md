# 💤 SleepyShop

A simple but effective ChestShop system that doesn't depend on big economy systems but uses items for trading. Perfect for survival servers that want a more "vanilla-plus" trading experience.

[![sleepyshop](https://img.shields.io/hangar/dt/sleepyshop?link=https%3A%2F%2Fhangar.papermc.io%2FJotrorox%2Fsleepyshop&style=for-the-badge)](https://hangar.papermc.io/Jotrorox/sleepyshop)
[![sleepyshop](https://img.shields.io/hangar/stars/sleepyshop?link=https%3A%2F%2Fhangar.papermc.io%2FJotrorox%2Fsleepyshop&style=for-the-badge)](https://hangar.papermc.io/Jotrorox/sleepyshop)
[![sleepyshop](https://img.shields.io/hangar/views/sleepyshop?link=https%3A%2F%2Fhangar.papermc.io%2FJotrorox%2Fsleepyshop&style=for-the-badge)](https://hangar.papermc.io/Jotrorox/sleepyshop)

---

## Features

*   **Item-Based Economy:** No complex money plugins needed. Trade items for items (e.g., Diamonds for Wood).
*   **Intuitive GUI:** Manage your shop through a clean, easy-to-use interface.
*   **Floating Displays:** Real-time information displays above your chests showing stock and prices.
*   **Customization:** Set custom shop names and toggle stock warnings.
*   **Lightweight & Fast:** Built with H2 (single-file) and asynchronous database operations to keep your server lag-free.

## Getting Started

### Creating a Shop
1.  Place a **Chest**, **Trapped Chest**, or **Barrel**.
2.  Place a **Sign** on or next to it.
3.  Write `[Shop]` or `[SleepyShop]` on the first line.
4.  Right-click the sign to open the configuration menu!

### Configuring Your Shop
*   **Price Settings:** Adjust how many items a buyer needs to pay.
*   **Item Settings:** Simply click with an item in your cursor to set what you are selling or what you accept as payment.
*   **Other Settings:** Toggle the floating display or change your shop's name.

## Configuration

SleepyShop is highly customizable via the `config.yml`. You can change how the floating text looks using MiniMessage formatting:
```yaml
shop-display: 
  title: " {owner}'s Shop" 
  selling: " Selling: {amount}x {item}" 
  price: " Price: {price}x {payitem}" 
  out-of-stock: "OUT OF STOCK"
```

Placement radius (distance from world spawn) can be configured to restrict where shops are created:
```yaml
placement-radius: -1 # -1 = no restriction, >0 = max distance from world spawn
```

## Admin Commands

`/sleepyshop reload`
Reloads `config.yml` and refreshes all floating shop displays.

`/sleepyshop list [owner]`
Lists tracked shops, optionally filtered by owner name.

`/sleepyshop inspect`
Shows owner, trade, stock, and location details for the shop sign or container you are looking at.

`/sleepyshop remove`
Removes the shop sign or container you are looking at without needing to break blocks manually.

## Requirements

*   **Java 21** or higher.
*   **Paper** or a Paper-compatible server (1.21+).

---

*Made by Johannes ([Jotrorox](https://jotrorox.com)) Müller*
