#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
将 PNG 图片按双线性插值缩放到指定尺寸（纯标准库，无需安装任何依赖）。

用法：
    py tools/resize_texture.py <输入.png> <输出.png> <宽> <高>

支持：8 位 RGB(2) / RGBA(6) 颜色类型、非隔行、过滤类型 0-4 的 PNG。

示例（把立绘缩放到 512x512）：
    py tools/resize_texture.py "src/main/resources/assets/miner_dimension_neptunia/textures/gui/goddess/prototype.png" 同路径 512 512
"""

import struct
import sys
import zlib


def decode_png(path):
    data = open(path, "rb").read()
    assert data[:8] == b"\x89PNG\r\n\x1a\n", "不是有效的 PNG 文件"
    pos = 8
    idat = bytearray()
    w = h = bd = ct = None
    while pos < len(data):
        ln = struct.unpack(">I", data[pos:pos + 4])[0]
        tag = data[pos + 4:pos + 8]
        payload = data[pos + 8:pos + 8 + ln]
        if tag == b"IHDR":
            w, h, bd, ct, comp, filt, inter = struct.unpack(">IIBBBBB", payload)
            assert bd == 8, "只支持 8 位深度，当前位深 %d" % bd
            assert ct in (2, 6), "只支持 RGB(2)/RGBA(6)，当前颜色类型 %d" % ct
            assert inter == 0, "不支持隔行(Adam7) PNG"
        elif tag == b"IDAT":
            idat += payload
        elif tag == b"IEND":
            break
        pos += 12 + ln
    raw = zlib.decompress(bytes(idat))
    ch = 4 if ct == 6 else 3
    stride = w * ch

    # ---- 逆过滤 ----
    rows = bytearray()
    prev = bytearray(stride)
    p = 0
    for _ in range(h):
        ft = raw[p]
        p += 1
        row = bytearray(raw[p:p + stride])
        p += stride
        if ft == 1:  # Sub
            for i in range(ch, stride):
                row[i] = (row[i] + row[i - ch]) & 0xFF
        elif ft == 2:  # Up
            for i in range(stride):
                row[i] = (row[i] + prev[i]) & 0xFF
        elif ft == 3:  # Average
            for i in range(stride):
                left = row[i - ch] if i >= ch else 0
                row[i] = (row[i] + ((left + prev[i]) >> 1)) & 0xFF
        elif ft == 4:  # Paeth
            for i in range(stride):
                a = row[i - ch] if i >= ch else 0
                b = prev[i]
                c = prev[i - ch] if i >= ch else 0
                pr = a + b - c
                pa, pb, pc = abs(pr - a), abs(pr - b), abs(pr - c)
                if pa <= pb and pa <= pc:
                    pred = a
                elif pb <= pc:
                    pred = b
                else:
                    pred = c
                row[i] = (row[i] + pred) & 0xFF
        rows += row
        prev = row
    return w, h, ch, bytes(rows)


def bilinear_resize(w, h, ch, src, out_w, out_h):
    """双线性插值缩放"""
    def px(x, y, c):
        off = (y * w + x) * ch + c
        return src[off]

    out = bytearray(out_w * out_h * ch)
    sx_scale = w / out_w
    sy_scale = h / out_h
    for oy in range(out_h):
        sy = (oy + 0.5) * sy_scale - 0.5
        y0 = max(0, min(h - 1, int(sy)))
        y1 = max(0, min(h - 1, y0 + 1))
        fy = sy - y0
        if y0 == y1:
            fy = 0.0
        for ox in range(out_w):
            sx = (ox + 0.5) * sx_scale - 0.5
            x0 = max(0, min(w - 1, int(sx)))
            x1 = max(0, min(w - 1, x0 + 1))
            fx = sx - x0
            if x0 == x1:
                fx = 0.0
            base = (oy * out_w + ox) * ch
            for c in range(ch):
                p00 = px(x0, y0, c)
                p10 = px(x1, y0, c)
                p01 = px(x0, y1, c)
                p11 = px(x1, y1, c)
                top = p00 + (p10 - p00) * fx
                bot = p01 + (p11 - p01) * fx
                out[base + c] = int(round(top + (bot - top) * fy))
    return out


def encode_png(w, h, ch, pixels):
    raw = bytearray()
    for y in range(h):
        raw.append(0)  # 过滤类型 0
        raw += pixels[y * w * ch:(y + 1) * w * ch]
    ihdr = struct.pack(">IIBBBBB", w, h, 8, 6 if ch == 4 else 2, 0, 0, 0)

    def chunk(tag, data):
        return (struct.pack(">I", len(data)) + tag + data
                + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF))

    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr)
            + chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + chunk(b"IEND", b""))


if __name__ == "__main__":
    if len(sys.argv) != 5:
        print("用法: py tools/resize_texture.py <输入.png> <输出.png> <宽> <高>")
        sys.exit(1)
    src_path, dst_path, out_w, out_h = sys.argv[1], sys.argv[2], int(sys.argv[3]), int(sys.argv[4])
    w, h, ch, src = decode_png(src_path)
    print("[OK] 读取 %s (%dx%d, %d通道)" % (src_path, w, h, ch))
    out = bilinear_resize(w, h, ch, src, out_w, out_h)
    with open(dst_path, "wb") as f:
        f.write(encode_png(out_w, out_h, ch, out))
    print("[OK] 已输出 %s (%dx%d)" % (dst_path, out_w, out_h))
