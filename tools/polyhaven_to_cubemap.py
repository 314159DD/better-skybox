"""Download Poly Haven (CC0) tonemapped sky panoramas and convert them to 6 cubemap faces.

Usage: python tools/polyhaven_to_cubemap.py <asset_name> [<asset_name> ...]
Writes src/main/resources/com/gpuskybox/skybox/<asset_name>/{px,nx,py,ny,pz,nz}.png (512x512).
Face convention matches SkyboxRenderer.equirectToFaces in Java (standard GL cubemap layout).
"""
import json
import os
import sys
import urllib.request

import numpy as np
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CACHE = os.path.join(ROOT, "ref", "polyhaven")
OUT = os.path.join(ROOT, "src", "main", "resources", "com", "gpuskybox", "skybox")
FACE = 512
Image.MAX_IMAGE_PIXELS = None


def download(name):
    os.makedirs(CACHE, exist_ok=True)
    path = os.path.join(CACHE, name + ".jpg")
    if os.path.exists(path):
        return path
    with urllib.request.urlopen(f"https://api.polyhaven.com/files/{name}") as r:
        url = json.load(r)["tonemapped"]["url"]
    print("downloading", url)
    urllib.request.urlretrieve(url, path)
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


def convert(name):
    src = download(name)
    img = Image.open(src).convert("RGB")
    if img.width > 4096:
        img = img.resize((4096, 2048), Image.LANCZOS)
    eq = np.asarray(img)
    h, w = eq.shape[:2]
    out_dir = os.path.join(OUT, name)
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
    for n in sys.argv[1:]:
        convert(n)
