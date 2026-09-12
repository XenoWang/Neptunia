#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
生成女神选择界面的占位立绘（纯标准库，无需安装任何依赖）。

用法：
    py tools/generate_goddess_placeholder.py

生成的图片输出到 src/main/resources/assets/miner_dimension_neptunia/textures/gui/goddess/ 下。
添加新女神时，在 PALETTES 中增加一组配色并调用 generate() 即可。
正式立绘请直接替换同名 PNG（默认按 128x128 注册，尺寸不同需同步修改
GoddessRegistry 中 setArtTexture 的宽高参数）。
"""

import os
import struct
import zlib

SIZE = 128
OUT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..",
                       "src", "main", "resources", "assets",
                       "miner_dimension_neptunia", "textures", "gui", "goddess")

# 每个女神的占位立绘配色
PALETTES = {
    "prototype": {  # 原型女神：紫色系
        "bg_top": (0x8A, 0x6B, 0xC9),
        "bg_bottom": (0x3A, 0x2A, 0x63),
        "border": (0xC8, 0xA2, 0x4B),
        "silhouette": (0xE8, 0xDD, 0xF8),
        "halo": (0xE8, 0xC5, 0x6B),
    },
    # 新女神示例（蓝色系）：
    # "purple_heart": {
    #     "bg_top": (0x4B, 0x7E, 0xC9),
    #     "bg_bottom": (0x1F, 0x33, 0x63),
    #     "border": (0xC8, 0xA2, 0x4B),
    #     "silhouette": (0xD8, 0xE8, 0xF8),
    #     "halo": (0xE8, 0xC5, 0x6B),
    # },
}


def in_ellipse(x, y, cx, cy, rx, ry):
    dx = (x - cx) / rx
    dy = (y - cy) / ry
    return dx * dx + dy * dy <= 1.0


def in_diamond(x, y, cx, cy, rx, ry):
    return abs(x - cx) / rx + abs(y - cy) / ry <= 1.0


def pixel_at(x, y, pal):
    """计算 (x, y) 处的像素颜色"""
    # 背景：垂直渐变
    t = y / (SIZE - 1)
    bg = tuple(int(a + (b - a) * t) for a, b in zip(pal["bg_top"], pal["bg_bottom"]))

    # 外边框
    border_w = 4
    if x < border_w or y < border_w or x >= SIZE - border_w or y >= SIZE - border_w:
        return pal["border"]

    # 内衬细线
    if x == border_w or y == border_w or x == SIZE - border_w - 1 or y == SIZE - border_w - 1:
        return pal["border"]

    # 身体（上窄下宽的梯形裙摆）
    if 60 <= y <= 100:
        half_w = 12 + (y - 60) * 16 / 36
        if abs(x - 64) <= half_w:
            return pal["silhouette"]

    # 肩部（椭圆）
    if in_ellipse(x, y, 64, 62, 30, 12):
        return pal["silhouette"]

    # 头部（圆）
    if in_ellipse(x, y, 64, 42, 13, 14):
        return pal["silhouette"]

    # 光环（菱形，位于剪影后方，最后判断以免遮住剪影）
    if in_diamond(x, y, 64, 40, 26, 15):
        return pal["halo"]

    return bg


def make_png(pixels):
    """pixels: 二维列表，每项为 (r, g, b) 元组"""
    raw = bytearray()
    for row in pixels:
        raw.append(0)  # 每行过滤器类型 0
        for r, g, b in row:
            raw += bytes((r, g, b, 255))
    ihdr = struct.pack(">IIBBBBB", SIZE, SIZE, 8, 6, 0, 0, 0)  # 8bit RGBA

    def chunk(tag, data):
        return (struct.pack(">I", len(data)) + tag + data
                + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF))

    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr)
            + chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + chunk(b"IEND", b""))


def generate(name, pal):
    pixels = [[pixel_at(x, y, pal) for x in range(SIZE)] for y in range(SIZE)]
    os.makedirs(OUT_DIR, exist_ok=True)
    path = os.path.join(OUT_DIR, name + ".png")
    with open(path, "wb") as f:
        f.write(make_png(pixels))
    print("[OK] 已生成占位立绘: " + path)


if __name__ == "__main__":
    for name, pal in PALETTES.items():
        generate(name, pal)
