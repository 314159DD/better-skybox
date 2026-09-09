"""Turn equirectangular sky panoramas into the 6 cubemap faces the plugin loads.

Usage:
  python tools/polyhaven_to_cubemap.py <polyhaven_asset> [...]      download CC0 tonemapped JPGs from Poly Haven
  python tools/polyhaven_to_cubemap.py --file <panorama> --name <n>  convert a local 2:1 panorama (image gen output)
  add --custom to write to ~/.runelite/gpu-skybox/<name>/ instead of the bundled resources

Writes <out>/<name>/{px,nx,py,ny,pz,nz}.png (512x512). Face convention is the standard GL cubemap
layout, matching sky_frag.glsl (sample direction y up).
"""
import argparse
import json
import os
import urllib.request

import numpy as np
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CACHE = os.path.join(ROOT, "ref", "polyhaven")
BUNDLED = os.path.join(ROOT, "src", "main", "resources", "com", "gpuskybox", "skybox")
CUSTOM = os.path.join(os.path.expanduser("~"), ".runelite", "gpu-skybox")
FACE = 512
UA = {"User-Agent": "gpu-skybox/0.1 (RuneLite plugin, github.com/314159DD)"}
Image.MAX_IMAGE_PIXELS = None


def fetch(url):
    return urllib.request.urlopen(urllib.request.Request(url, headers=UA))


def download(name):
    os.makedirs(CACHE, exist_ok=True)
    path = os.path.join(CACHE, name + ".jpg")
    if os.path.exists(path):
        return path
    with fetch(f"https://api.polyhaven.com/files/{name}") as r:
        url = json.load(r)["tonemapped"]["url"]
    print("downloading", url)
    with fetch(url) as r, open(path, "wb") as f:
        f.write(r.read())
    return path


def face_dirs(face):
    """Unit direction for every pixel of one face. u,v in [-1,1], v grows downwards."""
    a = (np.arange(FACE) + 0.5) / FACE * 2 - 1
    u, v = np.meshgrid(a, a)
    one = np.ones_like(u)
    if face == "px":
        d = (one, -v, -u)
    elif face == "nx":
        d = (-one, -v, u)
    elif face == "py":
        d = (u, one, v)
    elif face == "ny":
        d = (u, -one, -v)
    elif face == "pz":
        d = (u, -v, one)
    else:  # nz
        d = (-u, -v, -one)
    d = np.stack(d, axis=-1)
    return d / np.linalg.norm(d, axis=-1, keepdims=True)


def convert(src, name, out_root):
    img = Image.open(src).convert("RGB")
    if img.width > 4096:
        img = img.resize((4096, 2048), Image.LANCZOS)
    eq = np.asarray(img)
    h, w = eq.shape[:2]
    out_dir = os.path.join(out_root, name)
    os.makedirs(out_dir, exist_ok=True)
    for face in ("px", "nx", "py", "ny", "pz", "nz"):
        d = face_dirs(face)
        theta = np.arctan2(d[..., 0], d[..., 2])       # longitude
        phi = np.arcsin(np.clip(d[..., 1], -1, 1))     # latitude, +y up
        x = ((0.5 + theta / (2 * np.pi)) * w).astype(int) % w
        y = ((0.5 - phi / np.pi) * h).astype(int).clip(0, h - 1)
        Image.fromarray(eq[y, x]).save(os.path.join(out_dir, face + ".png"), optimize=True)
    print("wrote", out_dir)


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("assets", nargs="*", help="Poly Haven asset names")
    ap.add_argument("--file", help="local equirectangular panorama instead of a Poly Haven asset")
    ap.add_argument("--name", help="folder name for --file")
    ap.add_argument("--custom", action="store_true", help="write to ~/.runelite/gpu-skybox/ instead of the resources")
    args = ap.parse_args()
    out_root = CUSTOM if args.custom else BUNDLED
    if args.file:
        if not args.name:
            ap.error("--file needs --name")
        convert(args.file, args.name, out_root)
    for asset in args.assets:
        convert(download(asset), asset, out_root)
