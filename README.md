# CloudStorage

Infinite virtual item storage plugin for Paper 1.21.x, inspired by Satisfactory's dimensional storage.

## Features

- `/cloud` — paginated GUI showing all stored items with quantities
- **Cloud Block** — craftable barrel (chest + diamond) that opens cloud storage on right-click, with ambient particles and sounds
- **Shared Cloud** — toggle between personal and shared storage in the GUI
- **Auto-Cloud** — when inventory is full, items auto-deposit to your cloud (toggleable per-player)
- **Auto-Pickup** — mined block drops go directly to inventory (or cloud if full)
- **Pickup Log** — action bar shows picked up items (green for inventory, aqua for cloud overflow)
- **Shared Cloud Notifications** — chat log when someone deposits/withdraws from shared storage
- **Click-to-Deposit** — click items in your inventory while the cloud GUI is open to deposit them

## GUI Layout (54 slots)

| Slots | Content |
|-------|---------|
| 0-44 | Stored items (paginated, click to withdraw) |
| 45 | Previous page |
| 46 | Personal/Shared toggle |
| 47 | Auto-Cloud ON/OFF toggle |
| 48 | Deposit inventory |
| 49 | Deposit held item |
| 50 | Page info |
| 53 | Next page |

**Withdraw**: Left-click = 1 stack, Shift-click = all of that item

## Cloud Block

Craft: 1 chest + 1 diamond (shapeless recipe)

- Spawns ambient END_ROD particles
- Ender chest open sound on interact
- Beacon sounds on place/break
- Drops as cloud block item when broken

## Storage

SQLite database at `plugins/CloudStorage/storage.db`. Amounts stored as `BIGINT` (effectively infinite).

## Permissions

| Permission | Default | Description |
|-----------|---------|-------------|
| `cloudstorage.use` | true | Use /cloud command |
| `cloudstorage.craft` | true | Craft cloud blocks |

## Build

```sh
./gradlew build
# Output: build/libs/cloud-storage-mc-1.0.0.jar
```

Requires Java 21+.
