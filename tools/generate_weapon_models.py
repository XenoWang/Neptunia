#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
生成女神武器的物品模型 JSON（带厚度的薄板，而非纯平面贴图）。

背景：
  原版 `item/generated` 会把物品贴图自动挤成 1 像素厚的盒子（z 7.5~8.5），
  并在贴图轮廓边缘生成侧面。本脚本生成的是显式 `elements` 模型，
  厚度可自由调整（见 THICKNESS），且六个面都显式映射，不受原版生成器限制。

输出：
  每个 `textures/item/weapon/<名字>.png` 对应生成 `models/item/<名字>.json`。

用法：
    py tools/generate_weapon_models.py            # 使用 THICKNESS 默认值生成
    py tools/generate_weapon_models.py 2.0        # 指定厚度（模型单位，1 = 1 像素）

厚度参考（模型单位：1 = 1 像素 = 1/16 方块）：
    0.5  → 默认，正常武器刀身薄片厚度
    0.8  → 0.05 方块
    1.0  → 与原版自动生成一致
    2.0  → 明显能看出是一块厚板
    3.0  → 厚重感最强（像砖）

注意：模型里 `uv` 使用 0~16 归一化坐标；north 面按原版规则水平翻转，
      否则从背面看贴图会左右镜像。
"""

import glob
import json
import os
import sys

MODID = "miner_dimension_neptunia"
ASSETS = os.path.join("src", "main", "resources", "assets", MODID)
TEXTURE_DIR = os.path.join(ASSETS, "textures", "item", "weapon")
MODEL_DIR = os.path.join(ASSETS, "models", "item")

# 模型空间以 z=8 为中心平面，THICKNESS 为总厚度
# 0.5 ≈ 正常武器刀身的薄片厚度（比原版自动挤出的 1.0 还薄一半）
THICKNESS = 0.5


def build_model(name, thickness):
    """构造一个带厚度的薄板模型（继承 item/handheld 的显示变换）"""
    z0 = 8.0 - thickness / 2.0
    z1 = 8.0 + thickness / 2.0
    tex = "#layer0"

    return {
        "parent": "item/handheld",
        "textures": {
            "layer0": "%s:item/weapon/%s" % (MODID, name),
            "particle": "%s:item/weapon/%s" % (MODID, name),
        },
        "elements": [
            {
                "from": [0, 0, round(z0, 4)],
                "to": [16, 16, round(z1, 4)],
                "faces": {
                    # 正面：原版 item/generated 的 south 面就是正贴
                    "south": {"uv": [0, 0, 16, 16], "texture": tex},
                    # 背面：原版规则为水平翻转，否则从背面看是镜像的
                    "north": {"uv": [16, 0, 0, 16], "texture": tex},
                    # 四个侧面：取贴图对应边缘的一条极窄像素带作为厚度方向的颜色
                    "west": {"uv": [0, 0, round(thickness, 4), 16], "texture": tex},
                    "east": {"uv": [round(16 - thickness, 4), 0, 16, 16], "texture": tex},
                    "up": {"uv": [0, 0, 16, round(thickness, 4)], "texture": tex},
                    "down": {"uv": [0, round(16 - thickness, 4), 16, 16], "texture": tex},
                },
            }
        ],
    }


def main():
    thickness = float(sys.argv[1]) if len(sys.argv) > 1 else THICKNESS
    if thickness <= 0:
        print("[错误] 厚度必须大于 0")
        sys.exit(1)

    textures = sorted(glob.glob(os.path.join(TEXTURE_DIR, "*.png")))
    if not textures:
        print("[错误] 在 %s 下没找到任何贴图" % TEXTURE_DIR)
        sys.exit(1)

    print("厚度 = %s 模型单位（= %.4f 方块）" % (thickness, thickness / 16.0))
    for path in textures:
        name = os.path.splitext(os.path.basename(path))[0]
        model = build_model(name, thickness)
        out = os.path.join(MODEL_DIR, name + ".json")
        with open(out, "w", encoding="utf-8") as f:
            json.dump(model, f, indent=2, ensure_ascii=False)
            f.write("\n")
        print("  已生成 models/item/%s.json" % name)
    print("共 %d 个模型" % len(textures))


if __name__ == "__main__":
    main()
