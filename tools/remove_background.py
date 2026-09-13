#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
一键抠图工具：去除图片背景 + 擦除 AI 生成标识，输出透明背景 PNG。

依赖：Pillow（py -m pip install Pillow）

用法：
    py tools/remove_background.py <输入图> <输出.png> [目标尺寸] [背景容差]

参数：
    输入图       JPG / PNG 等 Pillow 支持的格式
    输出.png     透明背景输出文件
    目标尺寸     可选，默认 64；等比缩放并居中放到 N×N 透明画布（MC 物品贴图推荐 16/32/64）
    背景容差     可选，默认 40；每个像素与"背景参考色"的最大差异。
                 背景带柔和阴影 / 渐变时适当调大（如 60~90）

原理：
    1. 取图片四边的中位数颜色作为背景参考色（比洪水填充更抗渐变/阴影）
    2. 计算每个像素与参考色的距离，二值化为"背景相似区"，只保留与四边连通的部分
    3. 剩余区域里与主体不连通的"孤岛"（如 AI 生成水印）一并擦除
    4. 等比缩放居中放到目标尺寸，输出 RGBA 透明 PNG
"""

import sys

from PIL import Image, ImageChops, ImageDraw

BORDER_STRIDE = 8  # 四边采样间隔


def median_border_color(img):
    """采样四边像素，返回 RGB 中位数颜色作为背景参考色"""
    w, h = img.size
    pixels = []
    for x in range(0, w, 4):
        pixels.append(img.getpixel((x, 0)))
        pixels.append(img.getpixel((x, h - 1)))
    for y in range(0, h, 4):
        pixels.append(img.getpixel((0, y)))
        pixels.append(img.getpixel((w - 1, y)))
    n = len(pixels)
    return tuple(sorted(p[c] for p in pixels)[n // 2] for c in range(3))


def fill_connected_border(mask, fill_value):
    """在 0/255 二值掩码上，从四边出发填充所有与边缘连通的 255 区域"""
    w, h = mask.size
    seeds = []
    for x in range(0, w, BORDER_STRIDE):
        seeds.extend([(x, 0), (x, h - 1)])
    for y in range(0, h, BORDER_STRIDE):
        seeds.extend([(0, y), (w - 1, y)])
    for seed in seeds:
        # 只有种子本身是"背景相似"像素时才填充（thresh=0 只填与种子同色的连通区域）
        if mask.getpixel(seed) == 255:
            ImageDraw.floodfill(mask, seed, fill_value, thresh=0)
    return mask


def find_target_pixel_near_center(mask, target_value, bbox):
    """在包围盒中心附近螺旋搜索值为 target_value 的像素（作为主体填充种子）"""
    cx = (bbox[0] + bbox[2]) // 2
    cy = (bbox[1] + bbox[3]) // 2
    max_r = max(bbox[2] - bbox[0], bbox[3] - bbox[1]) // 2
    w, h = mask.size
    for r in range(0, max_r + 1, 3):
        for x in range(cx - r, cx + r + 1, 3):
            for y in (cy - r, cy + r):
                if 0 <= x < w and 0 <= y < h and mask.getpixel((x, y)) == target_value:
                    return (x, y)
        for y in range(cy - r, cy + r + 1, 3):
            for x in (cx - r, cx + r):
                if 0 <= x < w and 0 <= y < h and mask.getpixel((x, y)) == target_value:
                    return (x, y)
    return None


def remove_background(img, tolerance):
    """抠除背景并擦除孤岛水印，返回透明背景的 RGBA 图"""
    rgba = img.convert("RGBA")
    w, h = rgba.size

    # ---- 1. 计算"背景相似"二值图（与固定参考色比较，抗渐变/阴影） ----
    ref = median_border_color(rgba)
    diff = ImageChops.difference(rgba.convert("RGB"), Image.new("RGB", (w, h), ref))
    dist = ImageChops.add(ImageChops.add(diff.getchannel(0), diff.getchannel(1)), diff.getchannel(2))
    similar = dist.point(lambda v: 255 if v <= tolerance else 0)

    # ---- 2. 只保留与四边连通的背景相似区，其余为前景 ----
    bg = fill_connected_border(similar.copy(), 200)
    bg = bg.point(lambda v: 255 if v == 200 else 0)   # 255 = 背景
    keep = bg.point(lambda v: 255 - v)                # 255 = 保留（前景）
    if keep.getbbox() is None:
        return Image.new("RGBA", (w, h), (0, 0, 0, 0))
    rgba.putalpha(keep)
    rgba = Image.composite(rgba, Image.new("RGBA", (w, h), (0, 0, 0, 0)), rgba)

    # ---- 3. 擦除与主体不连通的孤岛（AI 水印等） ----
    bbox = keep.getbbox()
    seed = find_target_pixel_near_center(keep, 255, bbox)
    if seed is None:
        return Image.new("RGBA", (w, h), (0, 0, 0, 0))
    subject = keep.copy()
    ImageDraw.floodfill(subject, seed, 200, thresh=0)
    subject = subject.point(lambda v: 255 if v == 200 else 0)  # 255 = 主体连通区
    rgba.putalpha(subject)
    rgba = Image.composite(rgba, Image.new("RGBA", (w, h), (0, 0, 0, 0)), rgba)

    return rgba


def fit_to_canvas(img, target):
    """等比缩放并居中放到 target×target 透明画布"""
    img.thumbnail((target, target), Image.LANCZOS)
    canvas = Image.new("RGBA", (target, target), (0, 0, 0, 0))
    canvas.paste(img, ((target - img.width) // 2, (target - img.height) // 2), img)
    return canvas


if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("用法: py tools/remove_background.py <输入图> <输出.png> [目标尺寸=64] [背景容差=40]")
        sys.exit(1)
    src, dst = sys.argv[1], sys.argv[2]
    target = int(sys.argv[3]) if len(sys.argv) > 3 else 64
    tolerance = int(sys.argv[4]) if len(sys.argv) > 4 else 40

    img = Image.open(src)
    print("[OK] 读取 %s (%dx%d)" % (src, img.width, img.height))
    result = remove_background(img, tolerance)
    result = fit_to_canvas(result, target)
    result.save(dst, "PNG")
    print("[OK] 已输出透明背景图: %s (%dx%d)" % (dst, target, target))
