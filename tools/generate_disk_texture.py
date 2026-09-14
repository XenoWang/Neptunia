#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
生成女神磁盘物品材质（纯标准库，无需安装任何依赖）。

样式：光碟沿左下角↔右上角对角斜放，盘面清晰可见，
带中心圆孔、彩虹反光环带、底部薄边和投影阴影。

各世代色泽：Gen1 纯银无色泽，世代越高彩虹反光越丰富（Gen5 最饱满）。
    世代   色泽混合度   饱和度
    Gen1   0.0         0.0   普通光碟
    Gen2   0.30        0.35
    Gen3   0.55        0.45
    Gen4   0.80        0.55
    Gen5   1.0         0.65   色泽最丰满

用法：
    py tools/generate_disk_texture.py        # 生成全部 5 个世代
    py tools/generate_disk_texture.py 3      # 只生成指定世代（1~5）

输出：textures/item/important_item/goddess_disk_gen1.png ~ gen5.png
"""

import math
import os
import struct
import sys
import zlib

SIZE = 32          # 输出尺寸 32x32
SS = 3             # 每像素 3x3 超采样抗锯齿
OUT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..",
                       "src", "main", "resources", "assets",
                       "miner_dimension_neptunia", "textures", "item", "important_item")

# ---- 各世代色泽参数（混合度 = 彩虹色混入盘面的比例，饱和度 = 彩虹饱和度） ----
GEN_TINTS = [0.0, 0.30, 0.55, 0.80, 1.0]
GEN_SATS = [0.0, 0.35, 0.45, 0.55, 0.65]

# ---- 光碟几何：盘面沿左下角↔右上角对角斜放 ----
CX, CY = 15.5, 16.0        # 盘面中心
RX, RY = 11.0, 8.0         # 盘面椭圆半径（局部坐标；RY 越大盘面越圆）
SQ = math.sqrt(2.0) / 2.0  # cos45° = sin45°

# 底面椭圆偏移（屏幕向下 = 底部可见的薄边厚度）
RIM_DX, RIM_DY = 0.0, 2.2

# 投影阴影：向右下偏移
SHADOW_DX, SHADOW_DY = 1.5, 3.5
SHADOW_RX, SHADOW_RY = 12.0, 9.0


def hsv_to_rgb(h, s, v):
    """h: 0..1, s: 0..1, v: 0..1 -> (r, g, b) 0..255"""
    h = h % 1.0
    i = int(h * 6)
    f = h * 6 - i
    p, q, t = v * (1 - s), v * (1 - f * s), v * (1 - (1 - f) * s)
    return tuple(int(c * 255) for c in [(v, t, p), (q, v, p), (p, v, t), (p, q, v), (t, p, v), (v, p, q)][i % 6])


def to_local(x, y, cx, cy):
    """屏幕坐标 -> 光碟局部坐标（lx 沿左下-右上对角，ly 沿厚度方向）"""
    dx = x - cx
    dy = y - cy
    lx = dx * SQ - dy * SQ
    ly = dx * SQ + dy * SQ
    return lx, ly


def disc_color(x, y, tint, sat):
    """返回 (r, g, b, a)；tint=彩虹混入比例(0=纯银)，sat=彩虹饱和度"""
    # ---- 盘面 ----
    lx, ly = to_local(x, y, CX, CY)
    r2 = (lx / RX) ** 2 + (ly / RY) ** 2
    if r2 <= 1.0:
        r = math.sqrt(r2)
        angle = math.atan2(ly / RY, lx / RX)  # -pi..pi

        if r < 0.18:
            return (0, 0, 0, 0)  # 中心圆孔

        if r < 0.22:
            return (0xB8, 0xB8, 0xC2, 255)  # 圆孔边缘细环

        if r < 0.36:
            # 中心银色轮毂（顶侧略亮）
            t = (r - 0.22) / 0.14
            shade = max(0, min(255, int(226 + 8 * t - 14 * ly / RY)))
            return (shade, shade, min(255, shade + 4), 255)

        if r < 0.43:
            return (0xB0, 0xB0, 0xBA, 255)  # 轮毂边缘环

        if r <= 0.80:
            # 彩虹反光带：按世代混合度与银色底混合（Gen1 纯银无色泽）
            hue = (angle / (2 * math.pi) + 0.5) % 1.0
            rainbow = hsv_to_rgb(hue, sat, 0.98)
            color = [int(c * tint + s * (1 - tint)) for c, s in zip(rainbow, (0xDF, 0xDF, 0xE6))]

            # 顶侧（ly<0 一侧）柔和白色高光
            top_light = max(0.0, -ly / RY) * 0.35
            color = [int(c + (255 - c) * top_light) for c in color]
            return tuple(color) + (255,)

        if r < 0.88:
            return (0xB4, 0xB4, 0xBE, 255)  # 外圈分隔环

        # 外缘银色环：顶侧受光更亮、底侧略暗
        t = (r - 0.88) / 0.12
        light = max(0.0, -ly / RY) * 55
        dark = max(0.0, ly / RY) * 40
        shade = max(0, min(255, int(212 - 24 * t + light - dark)))
        return (shade, shade, min(255, shade + 4), 255)

    # ---- 底面偏移椭圆：底部可见的薄边 ----
    rlx, rly = to_local(x, y, CX + RIM_DX, CY + RIM_DY)
    rr2 = (rlx / RX) ** 2 + (rly / RY) ** 2
    if rr2 <= 1.0:
        r = math.sqrt(rr2)
        if r > 0.90:
            return (0xCB, 0xCB, 0xD4, 255)  # 薄边高光细线
        return (0xAD, 0xAD, 0xB8, 255)

    # ---- 投影阴影 ----
    slx, sly = to_local(x, y, CX + SHADOW_DX, CY + SHADOW_DY)
    sr2 = (slx / SHADOW_RX) ** 2 + (sly / SHADOW_RY) ** 2
    if sr2 <= 1.0:
        r = math.sqrt(sr2)
        return (0, 0, 0, int(60 * (1.0 - r)))

    return (0, 0, 0, 0)  # 透明


def sample(x, y, tint, sat):
    """3x3 超采样"""
    rs = gs = bs = as_ = 0
    for i in range(SS):
        for j in range(SS):
            r, g, b, a = disc_color(x + (i + 0.5) / SS, y + (j + 0.5) / SS, tint, sat)
            rs += r * a
            gs += g * a
            bs += b * a
            as_ += a
    if as_ == 0:
        return (0, 0, 0, 0)
    return (round(rs / as_), round(gs / as_), round(bs / as_), round(as_ / (SS * SS)))


def make_png(size, pixel_fn):
    raw = bytearray()
    for yy in range(size):
        raw.append(0)
        for xx in range(size):
            raw += bytes(pixel_fn(xx, yy))
    ihdr = struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0)  # 8bit RGBA

    def chunk(tag, data):
        return (struct.pack(">I", len(data)) + tag + data
                + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF))

    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr)
            + chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + chunk(b"IEND", b""))


if __name__ == "__main__":
    # 可选参数：只生成指定世代（1~5），不传则生成全部
    gens = range(1, 6)
    if len(sys.argv) > 1:
        g = int(sys.argv[1])
        if not 1 <= g <= 5:
            print("[错误] 世代参数需为 1~5")
            sys.exit(1)
        gens = [g]

    os.makedirs(OUT_DIR, exist_ok=True)
    for g in gens:
        tint = GEN_TINTS[g - 1]
        sat = GEN_SATS[g - 1]
        path = os.path.join(OUT_DIR, "goddess_disk_gen%d.png" % g)
        with open(path, "wb") as f:
            f.write(make_png(SIZE, lambda x, y: sample(x, y, tint, sat)))
        print("[OK] Gen%d (tint=%.2f, sat=%.2f) -> %s" % (g, tint, sat, path))
