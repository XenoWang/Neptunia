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
- [Adding a New Goddess 添加新女神（3 步）](#adding-a-new-goddess-添加新女神3-步)
- [GUI / UI File Guide (for UI developers) GUI 文件速查](#gui--ui-file-guide-for-ui-developers-gui-文件速查)
- [Network Packets 网络包说明](#network-packets-网络包说明)
- [Capability System 能力系统](#capability-system-能力系统)
- [Build & Run 构建与运行](#build--run-构建与运行)

---

## Project Structure / 项目结构

```
src/main/java/com/MinerDimensionNeptunia/NeptuniaMod/
├── capability/                  # Goddess capability system 女神化能力系统
│   ├── GoddessCapability.java               # Capability interface 能力接口
│   ├── GoddessCapabilityImplementation.java # Implementation (data storage / NBT) 实现（数据存储/读写）
│   └── GoddessCapabilityProvider.java       # Provider (attach & serialize) 提供者（附加到玩家、序列化）
├── client/
│   ├── gui/                                 # ★ UI related, see GUI guide below ★ UI 相关，见下文速查表
│   │   ├── GoddessHudRenderer.java          # In-game HUD (transformation bar) 游戏内女神化条渲染
│   │   ├── GoddessSelectionScreen.java      # Goddess selection screen 女神选择界面
│   │   └── KeyBindings.java                 # Key bindings 按键绑定注册
│   └── config/
│       ├── GoddessColorConfig.java          # Reads goddess_colors.json 读取颜色配置
│       └── GoddessConfig.java               # General client config 客户端常规配置
├── goddess/                                 # ★ Goddess data (standalone module) ★ 女神化数据（已独立拆分）
│   ├── Goddess.java                         # Data class for a single goddess 单个女神数据类
│   └── GoddessRegistry.java                 # Goddess registry 女神注册表
├── item/                                    # Items (e.g. goddess disks) 物品
├── network/                                 # Network sync packets 网络同步包
│   ├── GoddessAbilitySyncPacket.java        # Sync abilities to client 同步能力到客户端
│   ├── GoddessTypeSelectPacket.java         # Select goddess type 选择女神类型
│   └── TransformRequestPacket.java          # Request transformation 请求变身
└── util/
    ├── GoddessType.java                     # ★ Goddess type enum ★ 女神类别枚举
    └── Neptunia.java

src/main/resources/assets/miner_dimension_neptunia/
├── config/
│   └── goddess_colors.json                  # ★ Transformation bar colors ★ 女神化条颜色配置
├── lang/
│   └── en_us.json                           # Language file 语言文件
├── models/item/
│   └── goddess_disk.json                    # Goddess disk model 女神碟片模型
pack.mcmeta
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

## Adding a New Goddess / 添加新女神（3 步）

Adding a new goddess only takes **3 files**, in order:
新增一位女神只需按顺序修改 **3 个文件**：

### Step 1 / 第 1 步：Add the enum entry — `util/GoddessType.java`

```java
public enum GoddessType {
    NONE,
    PROTOTYPE;
    // 这里添加新女神名称，后续步骤名称需要保持统一
    // Add new goddess names here. Keep the name consistent in the following steps!
    // ...
}
```

> ⚠️ The enum constant name (e.g. `PROTOTYPE`) is serialized to the config key by converting to
> lowercase (e.g. `prototype`). This key must match **Step 2's GoddessType** and **Step 3's JSON key**.
> 枚举名（如 `PROTOTYPE`）会被转换为小写作为配置 key（如 `prototype`），必须与第 2 步的 GoddessType、
> 第 3 步的 JSON key 完全对应。

### Step 2 / 第 2 步：Register the data — `goddess/GoddessRegistry.java`

Add the new goddess inside `registerDefaultGoddesses()`, following the existing pattern:
在 `registerDefaultGoddesses()` 方法中仿照现有写法添加：

```java
private void registerDefaultGoddesses() {
    // 原型女神（平衡型）
    Goddess prototype = new Goddess(GoddessType.PROTOTYPE, "Prototype Goddess")
            .addAttributeBoost(Attributes.ATTACK_DAMAGE, 2.0)
            .addAttributeBoost(Attributes.MOVEMENT_SPEED, 2.0)
            .addAttributeBoost(Attributes.ARMOR, 2.0)
            .addAttributeBoost(Attributes.ARMOR_TOUGHNESS, 2.0);
    registerGoddess(prototype);

    // ============================================================
    // 🆕 在这里添加更多女神，例如：
    // ============================================================
    // Goddess PurpleHeart = new Goddess(GoddessType.STRENGTH, "紫色之心")
    //         .addAttributeBoost(Attributes.ATTACK_DAMAGE, 3.0)
    //         .addAttributeBoost(Attributes.ARMOR, 1.5);
    // registerGoddess(PurpleHeart);
    // ============================================================
}
```

- `new Goddess(GoddessType, "Display Name")` creates the goddess; chain `.addAttributeBoost(...)`
  to attach attribute modifiers （攻击、移速、护甲、韧性 etc.).
  用 `new Goddess(GoddessType, "显示名")` 创建女神，链式调用 `.addAttributeBoost(...)` 添加属性加成。
- Always end with `registerGoddess(goddess)` — an unregistered goddess will not work.
  最后必须调用 `registerGoddess(goddess)` 注册，否则不生效。

### Step 3 / 第 3 步：Bar colors — `resources/assets/miner_dimension_neptunia/config/goddess_colors.json`

Add a color config block for the new goddess. The JSON key is the enum name in **lowercase**:
为新女神添加颜色配置块，JSON key 为枚举名的**小写形式**：

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
  该文件由 `GoddessColorConfig.java` 启动时读取，供 HUD 渲染使用。
- All values are HEX colors (`#RRGGBB`). 所有颜色为十六进制（`#RRGGBB`）。
- After editing, reload resources with **F3+T** (no game restart needed).
  修改后按 **F3+T** 热重载资源即可生效，无需重启游戏。

### Extra / 补充：Language file 语言文件（可选但推荐）

Add the display name in `resources/assets/miner_dimension_neptunia/lang/en_us.json`:
在语言文件中添加显示名：

```json
{
  "goddess.miner_dimension_neptunia.prototype": "Prototype Goddess"
}
```

---

## GUI / UI File Guide (for UI developers) / GUI 文件速查（给做 UI 的人）

All UI code lives in `client/gui/`, with configs in `client/config/`:
所有 UI 代码在 `client/gui/`，配套配置在 `client/config/`：

| File 文件 | Responsibility 职责 |
| --- | --- |
| `client/gui/GoddessHudRenderer.java` | In-game HUD: transformation bar position, size, animation, text 女神化条的位置、尺寸、动画、文字渲染 |
| `client/gui/GoddessSelectionScreen.java` | Goddess selection screen: layout, buttons, selection state 女神选择界面布局、按钮、选中态 |
| `client/gui/KeyBindings.java` | Key bindings (open screen / transform) 按键注册（打开界面、变身） |
| `client/config/GoddessColorConfig.java` | Single entry point for bar colors 读取 goddess_colors.json，UI 取颜色的唯一入口 |
| `client/config/GoddessConfig.java` | General client settings (HUD toggle, offsets…) 客户端常规配置 |

### Quick lookup / 常见需求速查

| What you want to change 你想改什么 | Where to go 目标文件 |
| --- | --- |
| Bar colors 女神化条颜色 | `config/goddess_colors.json` (no code change 不用改代码) |
| Bar position / size / style 位置 / 大小 / 样式 | `client/gui/GoddessHudRenderer.java` |
| Selection screen layout 选择界面布局 | `client/gui/GoddessSelectionScreen.java` |
| Add / change keys 新增 / 修改按键 | `client/gui/KeyBindings.java` + `lang/en_us.json` |
| HUD toggle / client settings 显隐 / 设置项 | `client/config/GoddessConfig.java` |

> **Convention 约定**: Never hardcode colors in UI code — always read from `GoddessColorConfig`.
> Never hardcode display text — always use lang files.
> UI 代码中**不要**硬编码颜色（一律走 `GoddessColorConfig`）和显示文字（一律走 lang 文件）。

---

## Network Packets / 网络包说明

| Packet 包 | Direction 方向 | Purpose 作用 |
| --- | --- | --- |
| `TransformRequestPacket` | C → S | Request transform / revert 请求变身 / 解除 |
| `GoddessTypeSelectPacket` | C → S | Select goddess in the UI 界面选定女神 |
| `GoddessAbilitySyncPacket` | S → C | Sync abilities to client (for HUD) 同步能力供 HUD 显示 |

Adding a goddess requires **no new packets** — `GoddessType` syncs through the existing ones.
新增女神**不需要**新增网络包，GoddessType 会随现有包同步。

---

## Capability System / 能力系统

`capability/` attaches transformation data to the player:

- `GoddessCapability` — interface: get/set current goddess type 接口：当前女神类型的读写
- `GoddessCapabilityImplementation` — data storage & NBT read/write 数据存储与 NBT 读写
- `GoddessCapabilityProvider` — attaches capability to `Player`, handles (de)serialization 附加到玩家、序列化

The **server holds the authoritative state**; the client refreshes via sync packets.
**服务端数据为准**，客户端靠同步包刷新。

---

## Build & Run / 构建与运行

```bash
# Build 构建
./gradlew build          # Linux / macOS
gradlew.bat build        # Windows

# Dev client with hot reload 开发环境运行客户端
./gradlew runClient
```

Tips 提示：
- **F3+T** reloads resources (lang, JSON configs, models) without restarting.
  按 **F3+T** 可热重载语言文件、JSON 配置和模型，无需重启。
