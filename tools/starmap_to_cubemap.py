"""NASA Deep Star Maps 2020 (public domain, https://svs.gsfc.nasa.gov/4851) to the star cubemap the procedural sky samples.

Usage: python tools/starmap_to_cubemap.py [ref/nasa/starmap_2020_8k.exr]
Writes src/main/resources/com/betterskybox/skybox/stars/{px..nz}.png, 1024x1024, sRGB.
The EXR is linear 0..1; exposure and gamma below are tuned so the Milky Way band reads without blowing out the bright stars.

Credit (required by NASA SVS): NASA/Goddard Space Flight Center Scientific Visualization Studio. Gaia DR2: ESA/Gaia/DPAC.
"""
import os
import sys

os.environ["OPENCV_IO_ENABLE_OPENEXR"] = "1"
import cv2
import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import polyhaven_to_cubemap as ph

FACE = 1024
FLOOR = 0.0015     # sky background level in the EXR, subtracted so empty sky is black (stars are added over the gradient)
EXPOSURE = 5.0
GAMMA = 1 / 2.2
OUT = os.path.join(ph.BUNDLED, "stars")


def face_dirs(face):
    ph.FACE = FACE
    return ph.face_dirs(face)


def main(src):
    im = cv2.imread(src, cv2.IMREAD_UNCHANGED)[..., ::-1]  # BGR -> RGB, float32 linear
    im = np.clip((im - FLOOR) * EXPOSURE, 0, 1) ** GAMMA
    eq = (im * 255 + 0.5).astype(np.uint8)
    h, w = eq.shape[:2]
    os.makedirs(OUT, exist_ok=True)
    for face in ("px", "nx", "py", "ny", "pz", "nz"):
        d = face_dirs(face)
        theta = np.arctan2(d[..., 0], d[..., 2])
        phi = np.arcsin(np.clip(d[..., 1], -1, 1))
        x = ((0.5 + theta / (2 * np.pi)) * w).astype(int) % w
        y = ((0.5 - phi / np.pi) * h).astype(int).clip(0, h - 1)
        Image.fromarray(eq[y, x]).save(os.path.join(OUT, face + ".png"), optimize=True)
    print("wrote", OUT)


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else os.path.join(ph.ROOT, "ref", "nasa", "starmap_2020_8k.exr"))
