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
- [Goddesses 内置女神一览](#goddesses-内置女神一览)
- [Goddess Disk 女神磁盘](#goddess-disk-女神磁盘)
- [Goddess Selection Screen 女神选择界面](#goddess-selection-screen-女神选择界面)
- [JEI Integration JEI 集成](#jei-integration-jei-集成)
- [Commands 命令](#commands-命令)
- [Adding a New Goddess 添加新女神](#adding-a-new-goddess-添加新女神)
- [GUI / UI File Guide (for UI developers) GUI 文件速查](#gui--ui-file-guide-for-ui-developers-gui-文件速查)
- [Configuration System 配置系统](#configuration-system-配置系统)
- [Network Packets 网络包说明](#network-packets-网络包说明)
- [Capability System 能力系统](#capability-system-能力系统)
- [Tools 工具脚本](#tools-工具脚本)
- [Build & Run 构建与运行](#build--run-构建与运行)

---

## Project Structure / 项目结构

> 以下为**文件夹层面**的框架总览（主类 `Neptunia.java` 位于 Java 包根目录，负责所有注册入口）。

```
src/main/java/com/MinerDimensionNeptunia/NeptuniaMod/
├── capability/    # 女神化能力系统（附加到玩家、NBT 读写、序列化）
├── client/        # 客户端代码（事件、按键、HUD 渲染、女神选择界面、配置界面）
├── command/       # 命令（/neptunia goddess ...）
├── compat/        # 第三方模组兼容（JEI 插件：隐藏配方 + 获取提示）
├── config/        # 配置定义（ForgeConfigSpec）与颜色配置读取
├── goddess/       # ★ 女神数据模型与注册中心（新增女神核心入口）
├── item/          # 物品（女神磁盘）与创造模式标签页
├── loot/          # 战利品修改器注册（末地城宝箱注入女神磁盘）
├── network/       # 网络同步包（变身请求 / 类型选择 / 能力同步）
├── recipe/        # 自定义配方序列化器（女神磁盘隐藏合成配方）
└── util/          # 工具类（GoddessType 枚举）

src/main/resources/
├── assets/miner_dimension_neptunia/    # lang 语言 / models 物品模型 / textures 贴图 / config 颜色配置
├── data/                               # 合成配方与战利品修改器配置（JSON）
└── META-INF/                           # mods.toml 等模组元数据

tools/                                  # Python 辅助脚本（占位立绘生成、光碟材质生成、贴图压缩）
```

---

## Core Systems / 核心系统概览

Data flow of the transformation system / 女神化完整数据流：

```
Key press 玩家按键 (KeyBindings)
  → TransformRequestPacket (C → S 请求变身)
  → Server-side Capability switches GoddessType / applies Goddess data 服务端切换女神类型并应用属性加成
  → GoddessAbilitySyncPacket (S → C 同步能力)
  → Client GoddessHudRenderer draws the bar using GoddessColorConfig 客户端渲染女神化条
```

> **Note 注意**: The goddess system is a standalone module. Adding a new goddess requires  
> **zero changes** to capability / network / gui code — just follow the steps below.  
> 女神化系统已独立拆分，新增女神**不需要**改动 capability / network / gui 代码。

> **Anti-stacking 防叠加**: Every transform start / revert **clears all previous goddess
> attribute modifiers first** (matched by `goddess_boost_` name prefix), so switching
> goddesses can never stack multipliers.  
> 每次变身/解除都会**先清除所有女神属性加成**（按 `goddess_boost_` 名称前缀匹配），
> 更换女神类型不会产生乘区叠加。

---

## Goddesses 内置女神一览

All attribute multipliers are applied as `MULTIPLY_BASE`（数值 = 倍率 − 1）.  
所有属性加成以 `MULTIPLY_BASE` 应用（实际增量 = 倍率 − 1）。

| 显示名 | 枚举名 | 攻速 | 攻击 | 移速 | 护甲 | 护甲韧性 | 定位 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 原始之初 | `prototype` | — | ×2.0 | ×2.0 | ×2.0 | ×2.0 | 纯测试，后续会删除 |
| 绀紫之心 | `purple_heart` | ×1.3 | ×1.3 | ×1.3 | ×1.3 | ×1.3 | 均衡 |
| 圣黑之心 | `black_heart` | ×1.5 | ×1.5 | ×1.1 | ×1.2 | ×1.2 | 速攻 |
| 群白之心 | `white_heart` | ×0.7 | ×2.0 | ×1.1 | ×1.35 | ×1.35 | 重装 |
| 翡绿之心 | `green_heart` | ×1.5 | ×1.5 | ×1.3 | ×1.1 | ×1.1 | 速攻 |

- 四位 CPU 女神遵循"各属性变化量之和 ≤ 1.5"的平衡规则（如绀紫之心 0.3 × 5 = 1.5）。
- 译名采用 3DM《超次元游戏海王星:重生2》官方中文版：绀紫之心 / 圣黑之心 / 群白之心 / 翡绿之心。

---

## Goddess Disk 女神磁盘

### 获取方式 / Acquisition

| 方式 | 说明 |
| --- | --- |
| 末地城宝箱 | **35%** 概率开出 1 个（`data/miner_dimension_neptunia/loot_modifiers/goddess_disk_in_end_city.json` 可调概率与数量） |
| 合成 | 隐藏配方，见下表（**不在配方书 / JEI 中展示**，但可以正常合成） |

### 合成配方 / Crafting Recipe（隐藏）

```
  空    下界之星     空
 信标   任意唱片   绿宝石块
  空   下界合金锭    空
```

- "任意唱片" = 任意原版音乐唱片（`#minecraft:music_discs` 标签）。
- 配方通过自定义序列化器标记为"特殊配方"从而对配方书隐藏，JEI 侧由插件隐藏。

### 材质 / Texture

- 物品贴图：`assets/miner_dimension_neptunia/textures/item/goddess_disk.png`（32×32 斜角光碟）。
- 可用 `tools/generate_disk_texture.py` 重新生成/微调。

### 创造模式 / Creative

- 模组注册了 **Neptunia** 创造标签页，女神磁盘可在创造模式物品栏直接搜索获取（同时也是 JEI 物品列表的数据来源）。

---

## Goddess Selection Screen 女神选择界面

使用女神磁盘后打开的**游戏王卡牌风格**选择界面：

- 每张卡牌：顶部名字条 → **立绘框**（等比缩放居中）→ 下方**介绍文字**（自动换行）。
- **每行最多 4 张**，按界面宽度自动调整每行数量并换行，每行独立居中。
- 文字按卡牌宽度分为 **4 档字号**（1.0 / 0.85 / 0.72 / 0.62），仅在初始化时计算一次。
- 悬停高亮、点击即选中。

### 替换立绘 / Replacing the Art

1. 将图片放到 `assets/miner_dimension_neptunia/textures/gui/goddess/`，文件名 = 枚举名小写（如 `purple_heart.png`）。
2. 建议 **512×512**（任意尺寸也可，代码按实际像素等比缩放），非 512 请用工具压缩：

```bash
py tools/resize_texture.py <原图.png> <输出路径.png> 512 512
```

3. 若图片尺寸不是 512×512，需同步修改 `goddess/GoddessRegistry.java` 中该女神 `setArtTexture(..., 宽, 高)` 的参数。

---

## JEI Integration / JEI 集成

- 通过 `compat/` 下的 `@JeiPlugin` 插件实现（**未安装 JEI 时相关类不会被加载**，零影响）。
- **隐藏**女神磁盘的合成配方（JEI 运行时 API `hideRecipes`，配方重载后自动重新隐藏）。
- 女神磁盘显示信息页提示：*"Can be found in End City treasure chests."*（`lang/en_us.json` 的 `jei.miner_dimension_neptunia.goddess_disk.info`）。
- 物品本身可通过名字 / `@miner_dimension_neptunia` 在 JEI 中搜索到（创造标签页 + 额外物品注册双保险）。

---

## Commands / 命令

All commands require **OP level 2** (`/op <player>`).

| Command 命令 | Description 说明 |
| --- | --- |
| `/neptunia goddess clear` | Clear your own goddess ability 清除自己的女神化能力 |
| `/neptunia goddess clear <player>` | Clear a target player's goddess ability 清除指定玩家的女神化能力 |
| `/neptunia goddess add <player> <type>` | Add goddess ability to a target player 为指定玩家添加女神化能力 |

可用类型 `type`：`prototype`、`purple_heart`、`black_heart`、`white_heart`、`green_heart`（Tab 自动补全）。

> 在目标玩家**正在变身时**执行 `add` 会先移除旧女神的属性加成，再切换类型，不会叠加乘区。

---

## Adding a New Goddess / 添加新女神

新增一位女神只需按顺序修改 **2 个文件 + 1 张图**：

### Step 1 / 第 1 步：添加枚举 — `util/GoddessType.java`

```java
public enum GoddessType {
    NONE,
    PROTOTYPE,
    PURPLE_HEART,
    BLACK_HEART,
    WHITE_HEART,
    GREEN_HEART;
    // 这里添加新女神名称，后续步骤名称需要保持统一
    // Add new goddess names here. Keep the name consistent in the following steps!
}
```

### Step 2 / 第 2 步：注册数据 — `goddess/GoddessRegistry.java`

```java
private void registerDefaultGoddesses() {
    // 示例：新女神
    Goddess purpleHeart = new Goddess(GoddessType.PURPLE_HEART, "绀紫之心")
            .setDescription("均衡型。五项能力全面提升，攻守兼备。")   // 卡牌下方介绍文字
            .setArtTexture(new ResourceLocation(Neptunia.MODID,
                    "textures/gui/goddess/purple_heart.png"), 512, 512)  // 立绘路径 + 实际像素尺寸
            .addAttributeBoost(Attributes.ATTACK_SPEED, 1.3)
            .addAttributeBoost(Attributes.ATTACK_DAMAGE, 1.3)
            .addAttributeBoost(Attributes.ARMOR, 1.3)
            .addAttributeBoost(Attributes.MOVEMENT_SPEED, 1.3)
            .addAttributeBoost(Attributes.ARMOR_TOUGHNESS, 1.3);
    registerGoddess(purpleHeart);   // 必须注册，否则不生效
}
```

- `.setDescription(...)`：选择界面卡牌上的介绍文字（建议简短，小卡上显示有限）。
- `.setArtTexture(...)`：立绘贴图路径与图片实际宽高；未设置则卡牌立绘区留空。
- 倍率建议遵循"各属性变化量之和 ≤ 1.5"的平衡规则。

### Step 3 / 第 3 步：立绘贴图与颜色配置

- 立绘：放到 `assets/miner_dimension_neptunia/textures/gui/goddess/<枚举名小写>.png`（512×512，见[替换立绘](#替换立绘-replacing-the-art)）。
- HUD 颜色：在 `assets/miner_dimension_neptunia/config/goddess_colors.json` 添加同键名（枚举名小写）的颜色块：

```json
{
    "purple_heart": {
        "main": "#8B5CF6",
        "secondary": "#A78BFA",
        "border": "#6D28D9",
        "text": "#FFFFFF",
        "background": "#1E1B4B"
    }
}
```

| Field 字段 | Used for 用途 |
| --- | --- |
| `main` | Primary bar color 女神化条主色 |
| `secondary` | Secondary / fill gradient 副色 / 渐变填充 |
| `border` | Bar border 边框 |
| `text` | HUD text 文字颜色 |
| `background` | Bar background 背景色 |

- Loaded at startup by `config/GoddessColorConfig.java`, consumed by `GoddessHudRenderer`.
- After editing, reload resources with **F3+T** (no game restart needed).

---

## GUI / UI File Guide (for UI developers) / GUI 文件速查（给做 UI 的人）

All UI code lives in `client/gui/`:

| File 文件 | Responsibility 职责 |
| --- | --- |
| `client/gui/GoddessHudRenderer.java` | In-game HUD: transformation bar position, size, animation, text 女神化条的位置、尺寸、动画、文字渲染 |
| `client/gui/GoddessSelectionScreen.java` | 卡牌式女神选择界面：布局（每行最多 4 张自动换行）、字号档位、悬停高亮 |
| `client/gui/ModConfigScreen.java` | ★ Main config screen: displays a list of config modules 配置主界面：显示配置模块列表 |
| `client/gui/config/ConfigModule.java` | ★ Interface for config modules 配置模块接口 |
| `client/gui/config/HudConfigModule.java` | ★ HUD config sub-screen (sliders for position & scale) HUD 配置子界面（位置/缩放滑块） |
| `client/gui/config/Modules.java` | ★ Module registry (add new modules here) 模块注册中心 |

### Quick lookup / 常见需求速查

| What you want to change 你想改什么 | Where to go 目标文件 |
| --- | --- |
| Bar colors 女神化条颜色 | `resources/.../config/goddess_colors.json` (no code change) |
| Bar position / size / style 位置 / 大小 / 样式 | `client/gui/GoddessHudRenderer.java` |
| 选择界面卡牌大小 / 每行张数 / 字号档位 | `client/gui/GoddessSelectionScreen.java` 顶部常量 |
| 卡牌上的立绘 | `textures/gui/goddess/*.png` + `goddess/GoddessRegistry.java` |
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
| `TransformRequestPacket` | C → S | Request transform / revert 请求变身 / 解除（含属性加成应用与防叠加清理） |
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

## Tools 工具脚本

`tools/` 下的 Python 辅助脚本（全部纯标准库，无需 pip install）：

| Script 脚本 | Usage 用途 |
| --- | --- |
| `tools/generate_goddess_placeholder.py` | 生成女神立绘**占位图**（128×128，PALETTES 里加配色即可生成新女神占位） |
| `tools/generate_disk_texture.py` | 生成/微调**女神磁盘材质**（32×32 斜角光碟，改顶部几何参数后重跑） |
| `tools/resize_texture.py` | 把任意 PNG **双线性插值压缩**到指定尺寸（支持 8 位 RGB/RGBA）：`py tools/resize_texture.py <输入.png> <输出.png> 512 512` |

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
- **F3+T** reloads resources (lang, JSON configs, models, textures) without restarting.
- Configuration changes are saved via the in-game config screen and persisted to `.toml` files in `run/config/`.
- 开发环境 `runClient` 会自动附带 JEI（`runtimeOnly` 依赖），方便测试 JEI 集成；构建产物不含 JEI。

---

## License / 许可证

All Rights Reserved © MReimu(XenoWang)
