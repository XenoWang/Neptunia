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
- [Goddess Weapons 女神武器](#goddess-weapons-女神武器)
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
├── capability/      # 女神化能力系统（附加到玩家、NBT 读写、序列化）
├── client/          # 客户端代码
│   ├── config/      #   客户端配置加载（女神化条颜色等）
│   └── gui/         #   界面与渲染
│       └── config/  #     配置界面模块
├── command/         # 命令（/neptunia goddess ...）
├── compat/          # 第三方模组兼容
│   └── jei/         #   JEI 插件（隐藏配方 + 获取提示）
├── config/          # 配置定义（ForgeConfigSpec）
├── goddess/         # ★ 女神数据模型与注册中心（新增女神核心入口）
├── item/            # 物品与创造模式标签页注册中心
│   ├── ingredient/  #   材料类物品（合成 / 升级素材）
│   ├── usable/      #   可使用物品（右键触发效果，如女神磁盘）
│   └── weapon/      #   女神专属武器与伪耐久逻辑
├── loot/            # 战利品修改器注册（按维度向宝箱注入各世代女神磁盘）
├── network/         # 网络同步包（变身请求 / 类型选择 / 能力同步）
├── recipe/          # 自定义配方序列化器（女神磁盘隐藏合成配方）
└── util/            # 工具类（GoddessType 枚举）

src/main/resources/
├── assets/miner_dimension_neptunia/
│   ├── config/      #   女神化条颜色配置
│   ├── lang/        #   语言文件（en_us / zh_cn）
│   ├── models/item/ #   物品模型
│   └── textures/
│       ├── gui/goddess/  # 女神立绘
│       └── item/         # 物品贴图
├── data/
│   ├── forge/loot_modifiers/       # 战利品修改器启用列表
│   └── miner_dimension_neptunia/
│       ├── loot_modifiers/         # 战利品修改器配置（各维度宝箱 × 各世代磁盘）
│       └── recipes/                # 合成配方
└── META-INF/        # mods.toml 等模组元数据

tools/               # Python 辅助脚本（占位贴图生成、贴图压缩）
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

> **Anti-stacking 防叠加**: Every transform start / revert **clears previous goddess
> attribute modifiers first**, so switching goddesses can never stack multipliers.  
> 每次变身/解除都会**先清除旧的女神属性加成**，更换女神类型不会产生乘区叠加。

---

## Goddesses 内置女神一览

属性倍率作用于基础值（最终属性 = 基础值 × 倍率）。

| 显示名 | 枚举名 | 攻速 | 攻击 | 移速 | 护甲 | 护甲韧性 | 定位 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 原始之初 | `prototype` | — | ×3.0 | ×3.0 | ×3.0 | ×3.0 | 纯测试用 |
| 绀紫之心 | `purple_heart` | ×1.3 | ×1.3 | ×1.3 | ×1.3 | ×1.3 | 均衡 |
| 圣黑之心 | `black_heart` | ×1.5 | ×1.5 | ×1.1 | ×1.2 | ×1.2 | 速攻 |
| 群白之心 | `white_heart` | ×0.7 | ×2.0 | ×1.1 | ×1.35 | ×1.35 | 重装 |
| 翡绿之心 | `green_heart` | ×1.5 | ×1.5 | ×1.3 | ×1.1 | ×1.1 | 速攻 |

- 四位 CPU 女神遵循"各属性变化量之和 ≤ 1.5"的平衡规则（如绀紫之心 0.3 × 5 = 1.5）。
- 译名采用 3DM《超次元游戏海王星:重生2》官方中文版：绀紫之心 / 圣黑之心 / 群白之心 / 翡绿之心。

---

## Goddess Disk 女神磁盘

女神磁盘分为 **5 个世代（Gen1~Gen5）**，世代决定**女神化后的属性强弱**，也决定获取维度。
选定女神后**消耗 1 张**对应世代的磁盘；已拥有能力的玩家可再次使用磁盘**重新选择女神**（更换女神/世代，会先移除旧加成与飞行、结束当前变身，数值不会叠加）。

### 世代与获取 / Gens & Acquisition

| 世代 | 获取来源 | 属性强弱 |
| --- | --- | --- |
| Gen1 | 主世界宝箱（地牢 2% / 废弃矿井 2% / 沙漠神殿 3%，低概率） | 最弱 |
| Gen2 | 下界宝箱（要塞 6% / 堡垒遗迹 10%） | 中等偏弱 |
| Gen3 | 预留：未来新维度的结构宝箱 | 中等 |
| Gen4 | 末地城宝箱（**35%**） | 较强 |
| Gen5 | 预留：未来新维度的结构宝箱 | 满额 |

- 各掉落概率与注入目标在 `data/miner_dimension_neptunia/loot_modifiers/` 下按维度分文件配置，启用列表见 `data/forge/loot_modifiers/global_loot_modifiers.json`。
- Gen3 / Gen5 物品已注册（创造模式可取），新维度做好后加一份战利品修改器 JSON 即可。

### 属性强弱 / Stat Scaling

- 注册中心的属性倍率视为 **Gen5 满额数值**，实际倍率 = `1 + (注册值 − 1) × 世代系数`。
- 世代系数：Gen1 0.15 → Gen5 1.0（Prototype 的实际倍率即 ×1.3 / 1.7 / 2.0 / 2.5 / 3.0）。
- 世代记录在玩家能力中（`disk_gen` NBT），每次应用加成前仍会**先清除全部旧加成**，不会叠加。

### 变身时长 / Transform Duration

| 世代 | 时长 |
| --- | --- |
| Gen1 | 1 分钟 |
| Gen2 | 3 分钟 |
| Gen3 | 6 分钟 |
| Gen4 | 12 分钟 |
| Gen5 | 30 分钟 |

- 时长定义在 `GoddessDiskGen` 枚举（`getTransformDurationSeconds()`），客户端倒计时 / HUD 进度 / 登录兜底统一按世代读取。
- 倒计时归零由客户端通知服务端解除变身。

### 变身飞行 / Flight（Gen4 / Gen5）

- **Gen4**：变身期间获得创造飞行。
- **Gen5**：飞行 + 飞行速度 ×1.5（0.05 × 1.5 = 0.075）。
- 飞行状态跟随变身：变身开始授予（`GoddessFlightHandler#grantFlight`），结束/死亡时移除（`revokeFlight`）。
- **切换维度自动恢复**：服务端每 tick 保活校验，维度切换重置 abilities 后下一 tick 即恢复。
- **不误伤创造/旁观玩家**：移除飞行时跳过这两种模式（只恢复默认速度）。
- 变身状态与磁盘世代通过 `PlayerEvent.Clone` 跨死亡/维度复制；死亡重生会结束变身（移除加成与飞行），能力本身保留。

### 合成配方 / Crafting Recipe（隐藏）

每个世代一条隐藏配方，材料为该维度特色 + **对应世代的刻印装置**（配方中心）：

| 世代 | 布局（中心 = 刻印装置） | 材料 |
| --- | --- | --- |
| Gen1 | 紫水晶顶 + 金锭左右下 | 主世界 |
| Gen2 | 石英顶 + 烈焰粉左右 + 萤石粉下 | 下界 |
| Gen4 | 龙息顶 + 紫颂果左右 + 爆裂紫颂果下 | 末地 |

- Gen3 / Gen5 的配方待对应维度确定后再添加。
- 全部配方对配方书与 JEI 隐藏，但不影响正常合成（`isSpecial` + JEI `hideRecipes`）。

### 刻印装置 / Engrave Unit（MK1~MK5）

- 女神磁盘隐藏配方的**中心关键道具**，按世代对应（MK1 → Gen1 配方，以此类推）。
- **耐久条 = 刻印次数（10 次）**：每合成一次消耗 1 次，次数用完装置直接消失（原版"合成剩余物品"机制）。
- 后续规划：按维度由特定怪物掉落、更多用途（装备升级等）。

### 材质 / Texture

- 物品贴图：`textures/item/important_item/goddess_disk_gen1~5.png`（32×32 斜角光碟）。
- Gen1 纯银无色泽，世代越高彩虹反光越丰富（Gen5 最饱满）。
- 可用 `py tools/generate_disk_texture.py` 重新生成全部（或 `py tools/generate_disk_texture.py 3` 只生成指定世代）。

### 创造模式 / Creative

- 模组注册了 **Neptunia** 创造标签页，全部 5 个世代磁盘可在创造模式物品栏直接搜索获取（同时也是 JEI 物品列表的数据来源）。

---

## Goddess Selection Screen 女神选择界面

使用女神磁盘后打开的**卡牌风格**选择界面：

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

## Goddess Weapons 女神武器

每位女神有一把专属武器，分为 **6 个阶位（Tier）**，使用女神磁盘选定该女神后**自动获得 Tier 1**（已拥有同种武器不重复给予，背包满时掉落在脚下）。

| 女神 | 武器 | 注册名前缀 | 攻速特点 |
| --- | --- | --- | --- |
| 原始之初 | 刺剑 | `prototype_rapier_tier<N>` | 最快 |
| 绀紫之心 | 太刀 | `neptune_sword_tier<N>` | 较快 |
| 圣黑之心 | 长剑 | `noire_sword_tier<N>` | 标准 |
| 群白之心 | 战锤 | `blanc_hammer_tier<N>` | 最慢 |
| 翡绿之心 | 长枪 | `vert_spear_tier<N>` | 偏慢 |

### 伤害阶梯 / Damage Ladder

| Tier | 总伤害 | 说明 |
| --- | --- | --- |
| 1 | 7 | 铁剑级别以上（铁剑 6） |
| 2 | 8 | 钻石剑级别以上（钻石剑 7） |
| 3 | 12 | 前阶 ×1.5 |
| 4 | 18 | 前阶 ×1.5 |
| 5 | 27 | 前阶 ×1.5 |
| 6 | 54 | **最终武器**（前阶 ×2，为挑战最终 Boss 设计） |

- 攻速随阶位缓降，并按武器类型差异化（刺剑 2.0 → 战锤 1.0）。
- **完整数值表（30 件）见 `tools/weapon_tiers.csv`**（UTF-8 带 BOM，Excel 直接打开）。
- 全部为钻石品质，伪耐久条机制与 Tier 无关（见下）。
- 武器同时出现在 **Neptunia 创造标签页**，原版创造搜索与 JEI 均可检索；后续合成配方会正常展示（未做隐藏处理）。

### 伪耐久条 / Pseudo-Durability

武器**永远不会损坏**，耐久条相当于一个会自动恢复的资源条：

| 机制 | 说明 |
| --- | --- |
| 永不损坏 | 耐久最多扣到「上限 − 1」，单次扣减量被截断（见 `GoddessWeaponItem#damageItem`） |
| 随时间恢复 | 每秒 +10 点 |
| 击杀恢复 | 击杀任意生物额外 +200 点（远程击杀同样计入） |
| 作用范围 | 玩家背包内（含副手）的所有女神武器 |

> 恢复数值为 `item/weapon/GoddessWeaponEvents.java` 顶部的常量，按需调整即可。

### 模型与贴图 / Model & Texture

- 模型：由 `py tools/generate_weapon_models.py [厚度]` 统一生成（`item/handheld` 父模型 + 带厚度的薄板 elements，默认 0.5 模型单位 ≈ 0.03 方块，六个面均显式映射），**无需为不同武器类型单独建模型**。挥砍动作即原版手臂动画，第一 / 第三人称自动生效，无需额外动画文件。
- 贴图：**正方形 PNG，透明背景**（16~512 均支持，推荐 256/512 保留细节），放在 `textures/item/weapon/<注册名>.png`，同名覆盖即可生效。
- 占位贴图可用 `py tools/generate_weapon_textures.py` 重新生成。
- 作图要求：物品需**斜置绘制**（左下握柄 → 右上刀尖 / 锤头 / 枪尖），四周留 1 像素边距，与原版工具一致。
- **刀刃方向**：锋利一侧须**朝向左上**（与原版剑一致，否则手持挥砍像在用刀背打人）。AI 生成图若刀刃朝向右下，运行 `py tools/mirror_weapon_texture.py <贴图.png>` 一键修正。
- 未来计划：由建模师制作 Blockbench 模型并接入 GeckoLib，替换为真正的 3D 武器与自定义动画（当前为原版薄板模型）。

---

## JEI Integration / JEI 集成

- 通过 `compat/` 下的 JEI 插件实现（**未安装 JEI 时相关类不会被加载**，零影响）。
- **隐藏**女神磁盘的合成配方。
- 每个世代的磁盘显示各自的获取来源提示（`lang/en_us.json` 的 `jei.miner_dimension_neptunia.goddess_disk_genN.info`）。
- 物品本身可通过名字或 `@miner_dimension_neptunia` 在 JEI 中搜索到。

---

## Commands / 命令

All commands require **OP level 2** (`/op <player>`).

| Command 命令 | Description 说明 |
| --- | --- |
| `/neptunia goddess clear` | Clear your own goddess ability 清除自己的女神化能力 |
| `/neptunia goddess clear <player>` | Clear a target player's goddess ability 清除指定玩家的女神化能力 |
| `/neptunia goddess add <player> <type> [gen]` | Add goddess ability to a target player 为指定玩家添加女神化能力（`gen` 为磁盘世代 1~5，可省略，默认 Gen5） |

可用类型 `type`：`prototype`、`purple_heart`、`black_heart`、`white_heart`、`green_heart`（Tab 自动补全）；`gen` 同样支持 Tab 自动补全。

> 在目标玩家**正在变身时**执行 `add` 会先移除旧女神的属性加成，再切换类型，不会叠加乘区。

---

## Adding a New Goddess / 添加新女神

新增一位女神只需按顺序修改 **2 个文件 + 2 张图**（若需要专属武器，另加武器注册与武器贴图）：

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
            .setStarterWeapon(Neptunia.PURPLE_HEART_KATANA)          // 默认武器（物品注册项）
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
- `.setStarterWeapon(...)`：选定该女神后自动给予的默认武器（传物品注册项）；未设置则不给武器。
  新武器需先在 `item/weapon/` 下建类并在主类 `ITEMS` 中注册，详见[女神武器](#goddess-weapons-女神武器)。
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
| `client/gui/GoddessHudRenderer.java` | In-game HUD: Neptunia HDD-gauge style segmented bar (position, size, animation, text) 海王星 HDD 槽风格的分段能量读条（位置、尺寸、动画、文字） |
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
| 武器数值 / 伪耐久恢复速度 | `Neptunia.java`（伤害攻速）+ `item/weapon/GoddessWeaponEvents.java` 顶部常量 |
| 武器贴图 | `textures/item/*.png`（16×16，同名覆盖） |
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
| `tools/generate_disk_texture.py` | 生成/微调**女神磁盘材质**（32×32 斜角光碟，Gen1 纯银 → Gen5 彩虹满色泽）：`py tools/generate_disk_texture.py` 生成全部，或加 1~5 只生成指定世代 |
| `tools/generate_engrave_unit_texture.py` | 生成**光碟刻印装置材质**（64×64，LightScribe 光雕机造型，MK1 指示灯全灭 → MK5 三灯全亮且盘面最鲜艳）：`py tools/generate_engrave_unit_texture.py`，或加 1~5 只生成指定世代 |
| `tools/generate_weapon_textures.py` | 生成**女神武器占位贴图**（16×16，PALETTES 里加配色 / 选形状即可生成新武器占位） |
| `tools/remove_background.py` | **一键抠图**（去背景 + 擦除 AI 水印孤岛，输出透明 PNG）：`py tools/remove_background.py <输入图> <输出.png> 64 40`（后两个参数为尺寸与背景容差，依赖 Pillow） |
| `tools/resize_texture.py` | 把任意 PNG **双线性插值压缩**到指定尺寸（支持 8 位 RGB/RGBA）：`py tools/resize_texture.py <输入.png> <输出.png> 512 512` |
| `tools/mirror_weapon_texture.py` | **修正武器贴图刀刃朝向**（沿左下↔右上对角线镜像，刀刃/刀背互换到原版方向）：`py tools/mirror_weapon_texture.py <贴图.png>`，省略输出路径则原地覆盖（依赖 Pillow） |
| `tools/generate_weapon_models.py` | 生成**武器物品模型**（带厚度的薄板，默认 0.5 模型单位，六面显式 UV）：`py tools/generate_weapon_models.py [厚度]`，不传则用默认值 |

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
