#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
生成女神武器的占位材质（纯标准库，无需安装任何依赖）。

16x16 像素风：剑类为斜置刀身 + 护手 + 握柄，战锤为粗柄 + 方形锤头，
长枪为细长枪杆 + 枪尖。正式材质做好后直接同名覆盖即可。

用法：
    py tools/generate_weapon_textures.py

输出：src/main/resources/assets/miner_dimension_neptunia/textures/item/
"""

import os
import struct
import zlib

SIZE = 16
OUT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..",
                       "src", "main", "resources", "assets",
                       "miner_dimension_neptunia", "textures", "item")

# 每个武器的配色
PALETTES = {
    # 刺剑：细银刃 + 金护手
    "prototype_rapier": {
        "shape": "sword",
        "blade": (0xD8, 0xDC, 0xE4),
        "blade_light": (0xF2, 0xF5, 0xFA),
        "guard": (0xC8, 0xA2, 0x4B),
        "handle": (0x5A, 0x3E, 0x2A),
    },
    # 太刀：紫刃 + 黑柄金护手
    "purple_heart_katana": {
        "shape": "sword",
        "blade": (0x9B, 0x7B, 0xD8),
        "blade_light": (0xC9, 0xB4, 0xF0),
        "guard": (0xC8, 0xA2, 0x4B),
        "handle": (0x2E, 0x22, 0x45),
    },
    # 长剑：暗钢刃 + 银护手
    "black_heart_longsword": {
        "shape": "sword",
        "blade": (0x6E, 0x72, 0x82),
        "blade_light": (0xA8, 0xAD, 0xBD),
        "guard": (0xB8, 0xBC, 0xC8),
        "handle": (0x24, 0x26, 0x2E),
    },
    # 战锤：白钢锤头 + 木柄
    "white_heart_hammer": {
        "shape": "hammer",
        "head": (0xE2, 0xE6, 0xEE),
        "head_light": (0xF8, 0xFA, 0xFF),
        "head_dark": (0xA8, 0xAE, 0xBC),
        "handle": (0x7A, 0x55, 0x38),
        "handle_dark": (0x53, 0x38, 0x24),
    },
    # 长枪：绿枪杆 + 银枪尖
    "green_heart_spear": {
        "shape": "spear",
        "shaft": (0x4E, 0x9B, 0x5E),
        "shaft_dark": (0x2F, 0x66, 0x3C),
        "tip": (0xD8, 0xDC, 0xE4),
        "tip_light": (0xF2, 0xF5, 0xFA),
        "binding": (0xC8, 0xA2, 0x4B),
    },
}


def blank():
    return [[(0, 0, 0, 0) for _ in range(SIZE)] for _ in range(SIZE)]


def put(p, x, y, color):
    if 0 <= x < SIZE and 0 <= y < SIZE and color is not None:
        p[y][x] = color + (255,)


def draw_sword(p, c):
    """斜置刀身（左下→右上）+ 横向护手 + 握柄"""
    # 刀身：2px 宽
    for i in range(9):
        x, y = 5 + i, 11 - i
        put(p, x, y, c["blade_light"])
        put(p, x, y + 1, c["blade"])
    # 护手：横条
    for x in range(3, 9):
        put(p, x, 12, c["guard"])
    # 握柄：向左下延伸
    for i in range(3):
        put(p, 4 - i, 13 + i, c["handle"])
        put(p, 5 - i, 13 + i, c["handle"])


def draw_hammer(p, c):
    """粗柄（左下→右上）+ 右上方形锤头"""
    for i in range(10):
        x, y = 3 + i, 14 - i
        put(p, x, y, c["handle"])
        put(p, x, y - 1, c["handle_dark"])
    # 锤头
    for x in range(9, 14):
        for y in range(1, 6):
            put(p, x, y, c["head"])
    for x in range(9, 14):
        put(p, x, 2, c["head_light"])
        put(p, x, 5, c["head_dark"])
    for y in range(1, 6):
        put(p, 13, y, c["head_dark"])


def draw_spear(p, c):
    """细长枪杆（左下→右上）+ 枪尖 + 缠绳"""
    for i in range(11):
        x, y = 3 + i, 13 - i
        put(p, x, y, c["shaft"])
        put(p, x, y + 1, c["shaft_dark"])
    # 枪尖：末端三角
    for x, y in ((13, 3), (14, 2), (15, 1), (14, 3), (13, 2), (15, 2), (14, 1)):
        put(p, x, y, c["tip"])
    put(p, 14, 2, c["tip_light"])
    put(p, 15, 1, c["tip_light"])
    # 缠绳
    put(p, 11, 5, c["binding"])
    put(p, 11, 6, c["binding"])


SHAPES = {"sword": draw_sword, "hammer": draw_hammer, "spear": draw_spear}


def make_png(pixels):
    raw = bytearray()
    for row in pixels:
        raw.append(0)  # 每行过滤器类型 0
        for r, g, b, a in row:
            raw += bytes((r, g, b, a))
    ihdr = struct.pack(">IIBBBBB", SIZE, SIZE, 8, 6, 0, 0, 0)  # 8bit RGBA

    def chunk(tag, data):
        return (struct.pack(">I", len(data)) + tag + data
                + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF))

    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr)
            + chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + chunk(b"IEND", b""))


if __name__ == "__main__":
    os.makedirs(OUT_DIR, exist_ok=True)
    for name, palette in PALETTES.items():
        pixels = blank()
        SHAPES[palette["shape"]](pixels, palette)
        path = os.path.join(OUT_DIR, name + ".png")
        with open(path, "wb") as f:
            f.write(make_png(pixels))
        print("[OK] 已生成武器材质: " + path)
