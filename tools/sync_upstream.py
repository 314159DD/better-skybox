#!/usr/bin/env python3
"""Regenerate the copied RuneLite GPU renderer from an upstream checkout.

Every file this script owns is upstream's, put through four mechanical rename rules and, for the two
files that carry hooks, a patch in patches/. Nothing else in src/ is touched.

    python tools/sync_upstream.py --check           # fail if src/ has drifted from upstream + patches
    python tools/sync_upstream.py --apply           # regenerate src/ from upstream + patches
    python tools/sync_upstream.py --update-patches  # rewrite patches/ from the current src/

See SYNC.md for the pinned upstream revision and the list of intentional deltas.
"""

import argparse
import difflib
import io
import os
import shutil
import subprocess
import sys
import tempfile

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

UPSTREAM_JAVA = os.path.join("runelite-client", "src", "main", "java", "net", "runelite", "client", "plugins", "gpu")
UPSTREAM_RESOURCES = os.path.join("runelite-client", "src", "main", "resources", "net", "runelite", "client", "plugins", "gpu")

TARGET_JAVA = os.path.join("src", "main", "java", "com", "betterskybox")
TARGET_RESOURCES = os.path.join("src", "main", "resources", "com", "betterskybox")

# Applied in order. GpuPluginConfig must go before GpuPlugin, which is a prefix of it.
RULES = [
    ("package net.runelite.client.plugins.gpu", "package com.betterskybox"),
    ("net.runelite.client.plugins.gpu.", "com.betterskybox."),
    ("GpuPluginConfig", "BetterSkyboxConfig"),
    ("GpuPlugin", "BetterSkyboxPlugin"),
]

# Upstream basename -> ours. Everything else keeps its name.
RENAMED = {
    "GpuPlugin.java": "BetterSkyboxPlugin.java",
    "GpuPluginConfig.java": "BetterSkyboxConfig.java",
}

# Files whose hooks live in a patch instead of in the copy.
PATCHED = ["BetterSkyboxPlugin.java", "BetterSkyboxConfig.java"]

PATCH_DIR = "patches"


def read(path):
    """Read a text file as LF, whatever the working copy uses."""
    with io.open(path, encoding="utf-8", newline="") as f:
        return f.read().replace("\r\n", "\n")


def write(path, text):
    parent = os.path.dirname(path)
    if parent and not os.path.isdir(parent):
        os.makedirs(parent)
    with io.open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(text)


def rename(text):
    for old, new in RULES:
        text = text.replace(old, new)
    return text


def pairs(upstream):
    """(upstream absolute path, our repo-relative path) for every copied file, plus the ones we skip."""
    copied = []
    skipped = []
    for src_dir, dst_dir, keep in (
        (UPSTREAM_JAVA, TARGET_JAVA, lambda n: n.endswith(".java")),
        (UPSTREAM_RESOURCES, TARGET_RESOURCES, lambda n: n.endswith(".glsl") or n.endswith(".txt")),
    ):
        root = os.path.join(upstream, src_dir)
        if not os.path.isdir(root):
            sys.exit("not a RuneLite checkout: %s is missing" % root)
        for dirpath, dirnames, filenames in os.walk(root):
            dirnames.sort()
            for name in sorted(filenames):
                rel = os.path.relpath(os.path.join(dirpath, name), root)
                if not keep(name):
                    skipped.append(os.path.join(src_dir, rel).replace(os.sep, "/"))
                    continue
                ours = os.path.join(dst_dir, os.path.dirname(rel), RENAMED.get(name, name))
                if not os.path.isfile(os.path.join(REPO, ours)):
                    skipped.append(os.path.join(src_dir, rel).replace(os.sep, "/"))
                    continue
                copied.append((os.path.join(dirpath, name), ours.replace(os.sep, "/")))
    return copied, skipped


def git(args, cwd, ok=(0,)):
    p = subprocess.Popen(["git"] + args, cwd=cwd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    out, err = p.communicate()
    if p.returncode not in ok:
        sys.exit("git %s failed:\n%s" % (" ".join(args), err.decode("utf-8", "replace")))
    return out.decode("utf-8", "replace")


def patch_path(name):
    return os.path.join(REPO, PATCH_DIR, name.replace(".java", ".patch"))


def generate(upstream):
    """Renamed upstream plus patches, as {our repo-relative path: text}."""
    copied, skipped = pairs(upstream)
    out = {}
    for src, ours in copied:
        out[ours] = rename(read(src))

    work = tempfile.mkdtemp(prefix="sync_upstream_")
    try:
        for ours, text in sorted(out.items()):
            name = os.path.basename(ours)
            if name not in PATCHED:
                continue
            patch = patch_path(name)
            if not os.path.isfile(patch):
                sys.exit("missing %s; run --update-patches" % patch)
            target = os.path.join(work, name)
            write(target, text)
            # both sides LF, whatever the working copy checked out
            local = os.path.join(work, name + ".patch")
            write(local, read(patch))
            git(["apply", "-p1", "--whitespace=nowarn", local], cwd=work)
            patched = read(target)
            if patched == text and os.path.getsize(patch) > 0:
                sys.exit("%s did not apply to %s" % (patch, name))
            out[ours] = patched
    finally:
        shutil.rmtree(work, ignore_errors=True)
    return out, skipped


def check(upstream):
    generated, skipped = generate(upstream)
    drifted = []
    for ours, text in sorted(generated.items()):
        current = read(os.path.join(REPO, ours))
        if current == text:
            continue
        drifted.append(ours)
        sys.stdout.writelines(difflib.unified_diff(
            text.splitlines(True), current.splitlines(True),
            fromfile="upstream+patches/" + ours, tofile="src/" + ours))
    print("%d copied files checked, %d upstream files not copied" % (len(generated), len(skipped)))
    if drifted:
        print("DRIFT in %d file(s): %s" % (len(drifted), ", ".join(drifted)))
        return 1
    print("clean: every copied file matches upstream plus the rename rules and patches/")
    return 0


def apply(upstream):
    generated, skipped = generate(upstream)
    changed = 0
    for ours, text in sorted(generated.items()):
        path = os.path.join(REPO, ours)
        if read(path) == text:
            continue
        write(path, text)
        changed += 1
        print("updated %s" % ours)
    print("%d copied files, %d updated" % (len(generated), changed))
    return 0


def update_patches(upstream):
    copied, _ = pairs(upstream)
    for src, ours in copied:
        name = os.path.basename(ours)
        if name not in PATCHED:
            continue
        before = rename(read(src))
        after = read(os.path.join(REPO, ours))
        if not before.endswith("\n") or not after.endswith("\n"):
            sys.exit("%s has no trailing newline, which a patch cannot round-trip" % name)
        diff = "".join(difflib.unified_diff(
            before.splitlines(True), after.splitlines(True),
            fromfile="a/" + name, tofile="b/" + name))
        write(patch_path(name), diff)
        print("wrote %s (%d lines)" % (os.path.relpath(patch_path(name), REPO), diff.count("\n")))
    return 0


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--upstream", default=os.path.join(REPO, "ref", "runelite"),
        help="path to a RuneLite checkout at the pinned revision (default: ref/runelite)")
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--check", action="store_true", help="exit 1 if src/ differs from upstream plus patches")
    mode.add_argument("--apply", action="store_true", help="regenerate src/ from upstream plus patches")
    mode.add_argument("--update-patches", action="store_true", help="rewrite patches/ from the current src/")
    args = parser.parse_args()

    upstream = os.path.abspath(args.upstream)
    if args.check:
        return check(upstream)
    if args.apply:
        return apply(upstream)
    return update_patches(upstream)


if __name__ == "__main__":
    sys.exit(main())
