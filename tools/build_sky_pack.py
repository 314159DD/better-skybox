"""Zip the sky pack and write the manifest that ships in the jar.

Usage: python tools/build_sky_pack.py

Reads sky-pack/<name>/{px,nx,py,ny,pz,nz}.png (gitignored; polyhaven_to_cubemap.py and starmap_to_cubemap.py
write it), zips it to build/better-skybox-sky-pack.zip with that same layout, and writes the folder names to
src/main/resources/com/betterskybox/sky_pack_manifest.json, which is committed: the plugin ships no cubemaps
of its own but its config enum and its area table only name skies the pack has, and ResourceIntegrityTest
checks that against the manifest.

The zip is the release asset better-skybox-sky-pack.zip on tag sky-pack-v1, which is what SkyPack.PACK_URL
points at. Entries are stored, not deflated: PNG is already compressed.
"""
import json
import os
import zipfile

FACES = ("px", "nx", "py", "ny", "pz", "nz")
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PACK = os.path.join(ROOT, "sky-pack")
ZIP = os.path.join(ROOT, "build", "better-skybox-sky-pack.zip")
MANIFEST = os.path.join(ROOT, "src", "main", "resources", "com", "betterskybox", "sky_pack_manifest.json")


def main():
    names = sorted(n for n in os.listdir(PACK) if os.path.isdir(os.path.join(PACK, n)))
    os.makedirs(os.path.dirname(ZIP), exist_ok=True)
    with zipfile.ZipFile(ZIP, "w", zipfile.ZIP_STORED) as pack:
        for name in names:
            for face in FACES:
                pack.write(os.path.join(PACK, name, face + ".png"), name + "/" + face + ".png")
    with open(MANIFEST, "w", encoding="utf-8", newline="\n") as f:
        f.write(json.dumps(names, indent=2) + "\n")
    print("wrote", ZIP, os.path.getsize(ZIP), "bytes,", len(names), "folders")
    print("wrote", MANIFEST)


if __name__ == "__main__":
    main()
