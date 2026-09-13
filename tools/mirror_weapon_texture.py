#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
把武器贴图沿「左下↔右上」对角线镜像（转置 + 旋转 180°），修正刀刃朝向。

原版（如钻石剑）贴图约定：刀尖朝右上、刀柄朝左下，**刀刃朝向左上侧**、
刀背朝向右下侧。AI 生成图通常刀刃朝向右下、刀背朝向左上——正好相反，
手持挥砍时看起来就像在用刀背打人。

本脚本在**保持刀尖朝右上、刀柄朝左下**的前提下，把刀刃/刀背互换到原版方向，
无需重画贴图。变换自身互逆（跑两次 = 原图）。

用法：
    py tools/mirror_weapon_texture.py <输入.png> [输出.png]
    省略输出路径时原地覆盖。

依赖：Pillow（pip install pillow）
注意：仅适用于正方形贴图（MC 物品贴图均为正方形）。
"""

import sys

from PIL import Image


def main():
    if len(sys.argv) not in (2, 3):
        print("用法: py tools/mirror_weapon_texture.py <输入.png> [输出.png]")
        sys.exit(1)
    src = sys.argv[1]
    dst = sys.argv[2] if len(sys.argv) == 3 else src
    img = Image.open(src)
    w, h = img.size
    if w != h:
        print("[错误] 仅支持正方形贴图，当前尺寸 %dx%d" % (w, h))
        sys.exit(1)
    print("[OK] 读取 %s (%dx%d, %s)" % (src, w, h, img.mode))
    # 沿左下↔右上对角线镜像 = 转置 + 旋转 180°
    out = img.transpose(Image.Transpose.TRANSPOSE).transpose(Image.Transpose.ROTATE_180)
    out.save(dst)
    print("[OK] 已输出 %s" % dst)


if __name__ == "__main__":
    main()
