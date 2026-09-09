# Miner Dimension Neptunia / 矿工次元海王星模组

⚠️ DEVELOPER README — MOD USERS: DOWNLOAD THE .jar FROM RELEASES  
❌DO NOT DOWNLOAD THE ZIP SOURCE CODE❌  
⚠️ 此处为开发者 README.md —— 游玩本模组请直接前往 Release 下载 .jar 文件  
❌不要下载 ZIP 源代码❌

A Minecraft mod based on *Hyperdimension Neptunia* （超次元游戏海王星）, featuring the **Goddess Transformation （女神化）** system — transform into different goddesses and gain their unique powers.  
基于《超次元游戏海王星》的 Minecraft 模组，核心玩法为**女神化系统**：变身成不同女神形态，获得对应能力。

| Item 项目 | Version 版本 |
| --- | --- |
| Minecraft | 1.20.1 |
| Forge | 47.3.0 |
| JDK | 17 |

---

## Table of Contents / 目录

- [Project Structure 项目结构](#project-structure-项目结构)
- [Core Systems 核心系统概览](#core-systems-核心系统概览)
- [Commands 命令](#commands-命令)
- [Adding a New Goddess 添加新女神（3 步）](#adding-a-new-goddess-添加新女神3-步)
- [GUI / UI File Guide (for UI developers) GUI 文件速查](#gui--ui-file-guide-for-ui-developers-gui-文件速查)
- [Configuration System 配置系统](#configuration-system-配置系统)
- [Network Packets 网络包说明](#network-packets-网络包说明)
- [Capability System 能力系统](#capability-system-能力系统)
- [Build & Run 构建与运行](#build--run-构建与运行)

---

## Project Structure / 项目结构

```
src/main/java/com/MinerDimensionNeptunia/NeptuniaMod/
├── Neptunia.java                           # Main mod class 主类
│
├── capability/                             # Goddess capability system 女神化能力系统
│   ├── GoddessCapability.java              # Capability interface 能力接口
│   ├── GoddessCapabilityImplementation.java # Implementation (data storage / NBT) 实现（数据存储/读写）
│   └── GoddessCapabilityProvider.java      # Provider (attach & serialize) 提供者（附加到玩家、序列化）
│
├── client/
│   ├── ClientEvents.java                   # Client event handler 客户端事件处理
│   ├── KeyBindings.java                    # Key bindings 按键绑定注册
│   ├── gui/
│   │   ├── GoddessHudRenderer.java         # ★ In-game HUD (transformation bar) ★ 游戏内女神化条渲染
│   │   ├── GoddessSelectionScreen.java     # Goddess selection screen 女神选择界面
│   │   ├── ModConfigScreen.java            # ★ Main config screen (module list) ★ 配置主界面（模块列表）
│   │   └── config/                         # ★ Config modules ★ 配置模块
│   │       ├── ConfigModule.java           # Module interface 模块接口
│   │       ├── HudConfigModule.java        # HUD config sub-screen HUD 配置子界面
│   │       └── Modules.java                # ★ Module registry (add new modules here) ★ 模块注册中心
│   └── config/                             # Config loader (non-GUI) 配置加载器（非界面）
│       ├── GoddessColorConfig.java         # Reads goddess_colors.json 读取颜色配置
│       └── GoddessConfig.java              # ForgeConfigSpec definition 配置定义
│
├── command/                                # ★ Commands (standalone) ★ 命令（已独立拆分）
│   └── GoddessCommand.java                 # /neptunia goddess commands 女神化命令
│
├── goddess/                                # ★ Goddess data (standalone module) ★ 女神化数据（已独立拆分）
│   ├── Goddess.java                        # Data class for a single goddess 单个女神数据类
│   └── GoddessRegistry.java                # Goddess registry 女神注册表
│
├── item/                                   # Items (e.g. goddess disks) 物品
│   └── GoddessDiskItem.java
│
├── network/                                # Network sync packets 网络同步包
│   ├── GoddessAbilitySyncPacket.java       # Sync abilities to client 同步能力到客户端
│   ├── GoddessTypeSelectPacket.java        # Select goddess type 选择女神类型
│   └── TransformRequestPacket.java         # Request transformation 请求变身
│
└── util/
    └── GoddessType.java                    # ★ Goddess type enum ★ 女神类别枚举

src/main/resources/assets/miner_dimension_neptunia/
├── config/
│   └── goddess_colors.json                 # ★ Transformation bar colors ★ 女神化条颜色配置
├── lang/
│   └── en_us.json                          # Language file 语言文件
├── models/item/
│   └── goddess_disk.json                   # Goddess disk model 女神碟片模型
└── pack.mcmeta
```

---

## Core Systems / 核心系统概览

Data flow of the transformation system / 女神化完整数据流：

```
Key press 玩家按键 (KeyBindings)
  → TransformRequestPacket (C → S 请求变身)
  → Server-side Capability switches GoddessType / applies Goddess data 服务端切换女神类型
  → GoddessAbilitySyncPacket (S → C 同步能力)
  → Client GoddessHudRenderer draws the bar using GoddessColorConfig 客户端渲染女神化条
```

> **Note 注意**: The goddess system is now a standalone module. Adding a new goddess requires  
> **zero changes** to capability / network / gui code — just follow the 3 steps below.  
> 女神化系统已独立拆分，新增女神**不需要**改动 capability / network / gui 代码，只需按下面 3 步操作。

---

## Commands / 命令

All commands require **OP level 2** (`/op <player>`).

| Command 命令 | Description 说明 |
| --- | --- |
| `/neptunia goddess clear` | Clear your own goddess ability 清除自己的女神化能力 |
| `/neptunia goddess clear <player>` | Clear a target player's goddess ability 清除指定玩家的女神化能力 |
| `/neptunia goddess add <player> <type>` | Add goddess ability to a target player (type: `prototype`, etc.) 为指定玩家添加女神化能力 |

> **Auto-completion**: Tab works for player names and goddess types (`prototype`, etc.).  
> **自动补全**：按 Tab 自动补全玩家名和女神类型（如 `prototype`）。

---

## Adding a New Goddess / 添加新女神（3 步）

Adding a new goddess only takes **3 files**, in order:  
新增一位女神只需按顺序修改 **3 个文件**：

### Step 1 / 第 1 步：Add the enum entry — `util/GoddessType.java`

```java
public enum GoddessType {
    NONE,
    PROTOTYPE;   // 已有的
    // 这里添加新女神名称，后续步骤名称需要保持统一
    // Add new goddess names here. Keep the name consistent in the following steps!
}
```

### Step 2 / 第 2 步：Register the data — `goddess/GoddessRegistry.java`

Add the new goddess inside `registerDefaultGoddesses()`:

```java
private void registerDefaultGoddesses() {
    Goddess prototype = new Goddess(GoddessType.PROTOTYPE, "Prototype Goddess")
            .addAttributeBoost(Attributes.ATTACK_DAMAGE, 2.0)
            .addAttributeBoost(Attributes.MOVEMENT_SPEED, 2.0)
            .addAttributeBoost(Attributes.ARMOR, 2.0)
            .addAttributeBoost(Attributes.ARMOR_TOUGHNESS, 2.0);
    registerGoddess(prototype);

    // ============================================================
    // 🆕 在这里添加更多女神
    // 🆕 Add more goddesses here
    // ============================================================
    // Goddess purpleHeart = new Goddess(GoddessType.PURPLE_HEART, "紫色之心")
    //         .addAttributeBoost(Attributes.ATTACK_DAMAGE, 3.0)
    //         .addAttributeBoost(Attributes.ARMOR, 1.5);
    // registerGoddess(purpleHeart);
    // ============================================================
}
```

- `new Goddess(GoddessType, "Display Name")` creates the goddess; chain `.addAttributeBoost(...)` to attach attribute modifiers (attack, speed, armor, toughness, etc.).
- Always end with `registerGoddess(goddess)` — an unregistered goddess will not work.

### Step 3 / 第 3 步：Bar colors — `resources/assets/miner_dimension_neptunia/config/goddess_colors.json`

Add a color config block for the new goddess. The JSON key is the enum name in **lowercase**:

```json
{
    "prototype": {
        "main": "#8B5CF6",
        "secondary": "#A78BFA",
        "border": "#6D28D9",
        "text": "#FFFFFF",
        "background": "#1E1B4B"
    }
    // 🆕 在这里添加更多女神的颜色配置
    // 🆕 Add more goddess color configs here
}
```

| Field 字段 | Used for 用途 |
| --- | --- |
| `main` | Primary bar color 女神化条主色 |
| `secondary` | Secondary / fill gradient 副色 / 渐变填充 |
| `border` | Bar border 边框 |
| `text` | HUD text 文字颜色 |
| `background` | Bar background 背景色 |

- Loaded at startup by `client/config/GoddessColorConfig.java`, consumed by `GoddessHudRenderer`.
- After editing, reload resources with **F3+T** (no game restart needed).

---

## GUI / UI File Guide (for UI developers) / GUI 文件速查（给做 UI 的人）

All UI code lives in `client/gui/`:

| File 文件 | Responsibility 职责 |
| --- | --- |
| `client/gui/GoddessHudRenderer.java` | In-game HUD: transformation bar position, size, animation, text 女神化条的位置、尺寸、动画、文字渲染 |
| `client/gui/GoddessSelectionScreen.java` | Goddess selection screen: layout, buttons, selection state 女神选择界面布局、按钮、选中态 |
| `client/gui/ModConfigScreen.java` | ★ Main config screen: displays a list of config modules 配置主界面：显示配置模块列表 |
| `client/gui/config/ConfigModule.java` | ★ Interface for config modules 配置模块接口 |
| `client/gui/config/HudConfigModule.java` | ★ HUD config sub-screen (sliders for position & scale) HUD 配置子界面（位置/缩放滑块） |
| `client/gui/config/Modules.java` | ★ Module registry (add new modules here) 模块注册中心 |

### Quick lookup / 常见需求速查

| What you want to change 你想改什么 | Where to go 目标文件 |
| --- | --- |
| Bar colors 女神化条颜色 | `resources/.../config/goddess_colors.json` (no code change) |
| Bar position / size / style 位置 / 大小 / 样式 | `client/gui/GoddessHudRenderer.java` |
| Selection screen layout 选择界面布局 | `client/gui/GoddessSelectionScreen.java` |
| Add / change keys 新增 / 修改按键 | `client/KeyBindings.java` + `lang/en_us.json` |
| HUD offset / scale config 偏移 / 缩放配置 | `client/gui/config/HudConfigModule.java` + `GoddessConfig.java` |
| Add a new config module 新增配置模块 | ① Create module class → ② Register in `Modules.java` |

> **Convention 约定**: Never hardcode colors in UI code — always read from `GoddessColorConfig`.  
> Never hardcode display text — always use lang files.

---

## Configuration System / 配置系统

The configuration system is **modular**:

| Module 模块 | File 文件 | Purpose 作用 |
| --- | --- | --- |
| **HUD Module** | `client/gui/config/HudConfigModule.java` | X/Y offset, scale 位置/缩放 |
| **Future Modules** | Add to `Modules.java` | Combat, audio, graphics, etc. 战斗、音效、画面等 |

### Config entries (currently available) / 当前可用的配置项

| Entry 配置项 | Range 范围 | Default 默认 | Description 说明 |
| --- | --- | --- | --- |
| `hud.offsetX` | 0 ~ 500 | 20 | Horizontal offset from right edge 从右侧边缘的水平偏移 |
| `hud.offsetY` | 0 ~ 500 | 70 | Vertical offset from bottom edge 从底部边缘的垂直偏移 |
| `hud.scale` | 0.5 ~ 2.0 | 1.0 | Scale factor for HUD bar HUD 条缩放比例 |

---

## Network Packets / 网络包说明

| Packet 包 | Direction 方向 | Purpose 作用 |
| --- | --- | --- |
| `TransformRequestPacket` | C → S | Request transform / revert 请求变身 / 解除 |
| `GoddessTypeSelectPacket` | C → S | Select goddess in the UI 界面选定女神 |
| `GoddessAbilitySyncPacket` | S → C | Sync abilities to client (for HUD) 同步能力供 HUD 显示 |

Adding a goddess requires **no new packets** — `GoddessType` syncs through the existing ones.

---

## Capability System / 能力系统

`capability/` attaches transformation data to the player:

- `GoddessCapability` — interface: get/set current goddess type
- `GoddessCapabilityImplementation` — data storage & NBT read/write
- `GoddessCapabilityProvider` — attaches capability to `Player`, handles (de)serialization

**Data is synced with client** after each state change to keep HUD up-to-date.

---

## Build & Run / 构建与运行

```bash
# Build 构建
./gradlew build          # Linux / macOS
gradlew.bat build        # Windows

# Dev client with hot reload 开发环境运行客户端
./gradlew runClient
```

**Tips 提示**:
- **F3+T** reloads resources (lang, JSON configs, models) without restarting.
- Configuration changes are saved via the in-game config screen and persisted to `.toml` files in `run/config/`.

---

## License / 许可证

All Rights Reserved © MReimu