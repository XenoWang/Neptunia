#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
生成光碟刻印装置（Engrave Unit）物品材质（纯标准库，无需安装任何依赖）。

造型参考 LightScribe 光雕机：暗色金属机身 + 圆形凹陷托盘 + 光盘 +
从左上角伸出的激光刻印臂 + 状态指示灯。
世代差异：盘面彩虹色泽随世代递增（与女神磁盘一致），指示灯点亮数量随世代增加
（MK1 全部熄灭 → MK5 三灯全亮且最鲜艳）。

用法：
    py tools/generate_engrave_unit_texture.py        # 生成全部 5 个世代
    py tools/generate_engrave_unit_texture.py 3      # 只生成指定世代（1~5）

输出：textures/item/important_item/engrave_unit_mk1.png ~ mk5.png
"""

import math
import os
import struct
import sys
import zlib

SIZE = 64         # 输出尺寸 64x64
SS = 2            # 每像素 2x2 超采样抗锯齿
OUT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..",
                       "src", "main", "resources", "assets",
                       "miner_dimension_neptunia", "textures", "item", "important_item")

# ---- 各世代色泽参数（与女神磁盘一致） ----
GEN_TINTS = [0.0, 0.30, 0.55, 0.80, 1.0]
GEN_SATS = [0.0, 0.35, 0.45, 0.55, 0.65]

# ---- 几何布局 ----
BASE_CX, BASE_CY = 32.0, 32.0       # 机身中心
BASE_HW, BASE_HH = 27.0, 27.0       # 机身半宽/半高
BASE_R = 10.0                       # 机身圆角半径
TRAY_CX, TRAY_CY = 32.0, 35.0       # 托盘中心
TRAY_R = 20.0                       # 托盘半径
DISC_CX, DISC_CY = 32.0, 35.0       # 光盘中心（与托盘同心）
DISC_R = 15.0                       # 光盘半径
ARM_A = (9.0, 9.0)                  # 激光臂起点（机身左上角）
ARM_B = (28.5, 28.5)                # 激光臂终点（盘面上方）
ARM_HALF_W = 1.7                    # 激光臂半宽
HEAD_C = (30.0, 30.0)               # 刻印头中心
LEDS = [(14.0, 53.0), (21.0, 53.0), (28.0, 53.0)]  # 状态指示灯
LED_R = 1.8

# 指示灯颜色（按世代点亮 1~3 盏）
LED_COLORS = [(0x41, 0xE0, 0x8A), (0x43, 0xB7, 0xFF), (0xB0, 0x6B, 0xFF)]
# 刻印头镜头颜色（MK1 熄灭，MK2~MK5 依次更鲜艳）
LENS_COLORS = [None, (0x41, 0xE0, 0x8A), (0x43, 0xB7, 0xFF), (0xB0, 0x6B, 0xFF), (0xFF, 0x6B, 0xE1)]


def hsv_to_rgb(h, s, v):
    """h: 0..1, s: 0..1, v: 0..1 -> (r, g, b) 0..255"""
    h = h % 1.0
    i = int(h * 6)
    f = h * 6 - i
    p, q, t = v * (1 - s), v * (1 - f * s), v * (1 - (1 - f) * s)
    return tuple(int(c * 255) for c in [(v, t, p), (q, v, p), (p, v, t), (p, q, v), (t, p, v), (v, p, q)][i % 6])


def in_round_rect(x, y, cx, cy, hw, hh, r):
    dx = abs(x - cx)
    dy = abs(y - cy)
    if dx <= hw - r or dy <= hh - r:
        return True
    return (dx - (hw - r)) ** 2 + (dy - (hh - r)) ** 2 <= r * r


def dist_to_segment(px, py, ax, ay, bx, by):
    """点 (px,py) 到线段 AB 的距离"""
    vx, vy = bx - ax, by - ay
    wx, wy = px - ax, py - ay
    seg2 = vx * vx + vy * vy
    if seg2 <= 0:
        return math.hypot(wx, wy)
    t = max(0.0, min(1.0, (wx * vx + wy * vy) / seg2))
    return math.hypot(px - (ax + t * vx), py - (ay + t * vy))


def disc_band_color(angle, tint, sat, silver):
    """盘面彩虹反光带：按世代混合度与银色底混合"""
    hue = (angle / (2 * math.pi) + 0.5) % 1.0
    rainbow = hsv_to_rgb(hue, sat, 0.98)
    return tuple(int(c * tint + s * (1 - tint)) for c, s in zip(rainbow, silver))


def pixel(x, y, tint, sat, lit_count, lens_color):
    """返回 (r, g, b, a)
    <p>
    图层由下到上：机身 → 托盘 → 光盘 → 激光臂 → 刻印头 → 指示灯 → 散热槽。
    函数里按"上层优先"的顺序判断，机身圆角矩形作为兜底（决定整体轮廓）。
    """
    # ---- 光盘（正面正圆，色泽按世代；在最上层元素之下） ----
    d = math.hypot(x - DISC_CX, y - DISC_CY)
    if d <= DISC_R:
        r = d / DISC_R
        angle = math.atan2(y - DISC_CY, x - DISC_CX)
        # 光源方向：左上
        light = max(0.0, -(x - DISC_CX) / DISC_R - (y - DISC_CY) / DISC_R)

        if r < 0.16:
            return (48, 48, 51, 255)            # 中心圆孔（透出下方托盘色）
        if r < 0.20:
            return (0xB8, 0xB8, 0xC2, 255)      # 圆孔边缘细环
        if r < 0.35:
            # 中心银色轮毂
            shade = max(0, min(255, int(222 + 20 * light)))
            return (shade, shade, min(255, shade + 4), 255)
        if r < 0.42:
            return (0xB0, 0xB0, 0xBA, 255)      # 轮毂边缘环
        if r <= 0.82:
            # 彩虹反光带（按世代色泽）
            color = disc_band_color(angle, tint, sat, (0xDF, 0xDF, 0xE6))
            top_light = max(0.0, light) * 0.30
            color = tuple(int(c + (255 - c) * top_light) for c in color)
            return color + (255,)
        if r < 0.90:
            return (0xB4, 0xB4, 0xBE, 255)      # 外圈分隔环
        # 外缘环
        shade = max(0, min(255, int(212 - 20 * (r - 0.90) / 0.10 + 50 * max(0.0, light))))
        return (shade, shade, min(255, shade + 4), 255)

    # ---- 圆形凹陷托盘（盘面之外的托盘区域，比机身暗，边缘内阴影） ----
    dt = math.hypot(x - TRAY_CX, y - TRAY_CY)
    if dt <= TRAY_R:
        if dt > TRAY_R - 1.5:
            # 托盘内缘阴影圈
            k = (TRAY_R - dt) / 1.5
            shade = int(38 + 14 * k)
            return (shade, shade, shade + 3, 255)
        shade = 47 + int(5 * (1 - (x - TRAY_CX) / TRAY_R))
        shade = max(0, min(255, shade))
        return (shade, shade, shade + 3, 255)

    # ---- 激光刻印臂（从左上伸向盘面） ----
    if dist_to_segment(x, y, ARM_A[0], ARM_A[1], ARM_B[0], ARM_B[1]) <= ARM_HALF_W:
        return (40, 40, 46, 255)
    # 臂根部转轴
    if math.hypot(x - ARM_A[0], y - ARM_A[1]) <= 3.4:
        return (52, 52, 60, 255)

    # ---- 刻印头（镜头按世代点亮，MK1 熄灭） ----
    dh = math.hypot(x - HEAD_C[0], y - HEAD_C[1])
    if dh <= 3.6:
        return (38, 38, 44, 255)
    if lens_color is not None:
        if dh <= 2.0:
            return lens_color + (255,)
        if dh <= 4.2:
            # 镜头辉光（越靠外越透明）
            a = int(140 * (1 - (dh - 2.0) / 2.2))
            return lens_color + (max(0, min(255, a)),)

    # ---- 状态指示灯（按世代点亮） ----
    for i, (lx, ly) in enumerate(LEDS):
        dl = math.hypot(x - lx, y - ly)
        if dl <= LED_R:
            if i < lit_count:
                return LED_COLORS[i] + (255,)
            return (58, 60, 66, 255)  # 熄灭状态

    # ---- 右下散热槽 ----
    if 40 <= x <= 58 and 51 <= y <= 55:
        if (y - 51) % 1.6 < 0.8:
            return (30, 30, 35, 255)

    # ---- 机身（暗色金属，兜底：机身范围内的其余区域） ----
    if in_round_rect(x, y, BASE_CX, BASE_CY, BASE_HW, BASE_HH, BASE_R):
        # 距离机身边缘的"描边"（外缘一圈高光）
        edge = BASE_HW - max(abs(x - BASE_CX), abs(y - BASE_CY))
        t = (x + y) / (2 * SIZE)
        shade = int(96 - 38 * t + 6 * (1 - t) - 10 * max(0, y - x) / SIZE)
        # 外缘高光：机身外 1.5px 范围
        if edge < 1.6:
            shade = min(255, int(shade + 34 * (1.6 - edge) / 1.6))
        shade = max(0, min(255, shade))
        return (shade, shade, min(255, shade + 4), 255)

    return (0, 0, 0, 0)  # 透明


def sample(x, y, tint, sat, lit_count, lens_color):
    """2x2 超采样"""
    rs = gs = bs = as_ = 0
    for i in range(SS):
        for j in range(SS):
            r, g, b, a = pixel(x + (i + 0.5) / SS, y + (j + 0.5) / SS,
                               tint, sat, lit_count, lens_color)
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
        lit = max(0, g - 1)              # MK1 全灭 → MK5 三灯全亮
        lens = LENS_COLORS[g - 1]
        path = os.path.join(OUT_DIR, "engrave_unit_mk%d.png" % g)
        with open(path, "wb") as f:
            f.write(make_png(SIZE, lambda x, y: sample(x, y, tint, sat, lit, lens)))
        print("[OK] MK%d (tint=%.2f, sat=%.2f, LED=%d) -> %s" % (g, tint, sat, lit, path))
