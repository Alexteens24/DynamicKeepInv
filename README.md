````markdown
# DynamicKeepInv

![Java](https://img.shields.io/badge/Java-17%2B-orange) ![Server](https://img.shields.io/badge/Server-Paper%20%7C%20Spigot%20%7C%20Folia-blue) ![License](https://img.shields.io/badge/License-Apache%202.0-green)

**DynamicKeepInv** is a sophisticated plugin that revolutionizes how `keepInventory` works on Minecraft servers. Instead of a simple global toggle, it dynamically controls inventory loss based on in-game time, death causes, territory protection, and economy.

It is designed for modern servers (1.20.4+) and features full native support for **Folia** region threading.

---

## 🌟 Why use DynamicKeepInv?

Vanilla Minecraft forces you to choose between "Too Easy" (`keepInventory true`) or "Too Hard" (`keepInventory false`).

**DynamicKeepInv** bridges this gap:
* **🌞 Day/Night Cycle:** Players feel safe during the day but fear the night.
* **⚔️ PvP vs PvE:** Make PvP high-stakes (drop items) while keeping PvE casual (keep items), or vice-versa.
* **💰 Economy Sink:** Allow players to pay a fee to save their items upon death via an interactive GUI.
* **🛡️ Land Integration:** Respects claims from Lands and GriefPrevention (e.g., keep items in your own base).

---

## ✨ Key Features

### 🕒 Time-Based Rules
* Automatically enables `keepInventory` during the day.
* Disables it at night (configurable start/end ticks).
* Plays sounds and sends titles when the status changes.

### 💀 Advanced Death Rules
* **PvP vs PvE:** Configure separate rules for Item Loss and XP Loss based on the killer.
* **By World:** Set different rules for the Overworld, Nether, and End.
* **Permission Bypass:** VIPs with `dynamickeepinv.bypass` never lose items.

### 💸 Economy & Death GUI
* **Death Confirmation GUI:** When a player dies, a GUI appears asking them to **PAY** to keep items or **DROP** them.
* **Auto-Pay:** Players can toggle `/dki autopay` to automatically deduct the fee and skip the GUI.
* **Persistence:** If a player disconnects while dead, their pending death is saved to a database and the GUI reopens upon rejoin.
* *(Requires Vault)*

### 📊 Player Statistics
* Tracks death history for every player.
* **In-Game GUI:** Run `/dki stats` to see:
    * Total Deaths / Saved Deaths / Lost Deaths.
    * Money spent on keeping inventory.
    * Death cause breakdown (Day, Night, PvP, etc.).
* **PlaceholderAPI:** Display these stats on scoreboards or chat.

### 🧩 Plugin Integrations
* **Lands:** Configure rules for "Own Land", "Enemy Land", and "Wilderness".
* **GriefPrevention:** Similar support for claims.
* **GravesX / AxGraves:** If a player chooses to drop items (or can't afford to pay), they are placed into a Grave instead of scattering on the ground.
* **Folia:** 100% thread-safe region scheduling.

---

## 🧠 Logic Hierarchy

When a player dies, the plugin decides whether to keep or drop items based on this priority order (highest to lowest):

1.  **Bypass Permission** (`dynamickeepinv.bypass`)
    * *If player has this, they always keep items.*
2.  **Claim Protection** (Lands / GriefPrevention)
    * *Is the player in their own claim? Use claim settings.*
3.  **Death Cause** (PvP / PvE)
    * *Did another player kill them? Use PvP settings.*
4.  **Wilderness Settings**
    * *If enabled, use specific settings for unclaimed land.*
5.  **Time-Based** (Day / Night)
    * *Fallback: Is it currently Day or Night?*

---

## 📥 Installation

1.  Download the latest JAR from the [Releases](https://github.com/Alexteens24/DynamicKeepInv/releases) page.
2.  Drop it into your server's `plugins/` folder.
3.  **(Optional)** Install [Vault](https://www.spigotmc.org/resources/vault.34315/) and an Economy plugin for paid features.
4.  Restart the server.

---

## 🎮 Commands & Permissions

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/dki status` | `dynamickeepinv.admin` | View current status (Time, World, Enabled). |
| `/dki reload` | `dynamickeepinv.admin` | Reload config and messages. |
| `/dki toggle` | `dynamickeepinv.admin` | Toggle the plugin on/off globally. |
| `/dki stats [player]` | `dynamickeepinv.stats` | View death statistics GUI. |
| `/dki confirm` | `dynamickeepinv.use` | Re-open the Death Confirmation GUI (if pending). |
| `/dki autopay` | `dynamickeepinv.use` | Toggle auto-payment mode. |

**Other Permissions:**
* `dynamickeepinv.bypass`: User always keeps inventory.
* `dynamickeepinv.stats.others`: View other players' stats.

---

## 📂 Configuration

A snippet of `config.yml` demonstrating the advanced economy mode:

```yaml
advanced:
  enabled: true
  
  # Economy Settings
  economy:
    enabled: true
    cost: 100.0
    mode: "gui" # Options: charge-to-keep, charge-to-bypass, gui
    gui:
      timeout: 30 # Seconds to decide
      expire-time: 300 # Seconds to save data on disconnect

  # Death Cause Rules
  death-cause:
    enabled: true
    pvp:
      keep-items: false
      keep-xp: false
    pve:
      keep-items: true
      keep-xp: true
````

-----

## 🧩 Placeholders

Requires **PlaceholderAPI**.

  * `%dynamickeepinv_isday%` - Returns `true` or `false`.
  * `%dynamickeepinv_period%` - Returns "Day" or "Night".
  * `%dynamickeepinv_stats_save_rate%` - Player's save percentage.
  * `%dynamickeepinv_economy_cost%` - Current death cost.

-----

## 📄 License

This project is licensed under the [Apache 2.0 License](https://www.google.com/search?q=LICENSE).

```

### Changes included in this version:
1.  **Visual Badges**: Added at the top to make it look professional.
2.  **Logic Hierarchy**: Explicitly explains *how* the plugin decides (based on `wiki/Advanced-Configuration.md`).
3.  **Detailed Features**: Added specific mentions of **GravesX**, **Folia**, and the **Pending Death Database** mechanism which ensures safety on disconnects.
4.  **Commands Table**: Clearer presentation of commands found in `wiki/Commands.md`.
```
