# Syncing with the RuneLite GPU plugin

Better Skybox is the stock GPU renderer plus a sky pass. 33 of its files are copies of RuneLite's
`net.runelite.client.plugins.gpu`, and none of them is hand edited: `tools/sync_upstream.py` regenerates
every one of them from a RuneLite checkout by applying four rename rules and, for the two files that carry
hooks, a patch from `patches/`. Anyone can run the check and see for themselves that the renderer is upstream's.

## Pinned upstream

| | |
| --- | --- |
| Client version | 1.12.38 (`runeLiteVersion` in `build.gradle`) |
| Source | https://github.com/runelite/runelite |
| Revision | `ac79ed8bd8926bec7bf172aa291574b4d944b0e7` (2026-09-04) |

`ref/` is gitignored, so fetch the checkout yourself:

```
git clone --filter=blob:none https://github.com/runelite/runelite.git ref/runelite
git -C ref/runelite checkout ac79ed8bd8926bec7bf172aa291574b4d944b0e7
```

## The four rename rules

Applied in this order to every copied file. `GpuPluginConfig` must go before `GpuPlugin`, which is a prefix
of it.

| From | To |
| --- | --- |
| `package net.runelite.client.plugins.gpu` | `package com.betterskybox` |
| `net.runelite.client.plugins.gpu.` | `com.betterskybox.` |
| `GpuPluginConfig` | `BetterSkyboxConfig` |
| `GpuPlugin` | `BetterSkyboxPlugin` |

`GpuPlugin.java` and `GpuPluginConfig.java` are also renamed on disk, to `BetterSkyboxPlugin.java` and
`BetterSkyboxConfig.java`. Nothing else is renamed.

## The three commands

```
python tools/sync_upstream.py --check           # exit 1 if any copied file has drifted; prints the diff
python tools/sync_upstream.py --apply           # regenerate the copied files from upstream + patches
python tools/sync_upstream.py --update-patches  # rewrite patches/ from the current src/
```

Python 3, standard library only, plus `git apply` on the PATH. Pass `--upstream <path>` for a checkout
somewhere other than `ref/runelite`. Line endings are normalised on both sides, so a CRLF working copy is
fine. `--check` is what a reviewer runs, and what CI should run.

Bumping the client version:

1. Check out the new RuneLite revision in `ref/runelite` and update the table above.
2. `--apply`. If a patch fails to apply, upstream moved one of the hook sites: fix that one hunk by hand in
   `src/`, then `--update-patches`.
3. If the bump added or removed an upstream file, decide whether it is ours. The copied set is pinned in
   `COPIED` in `tools/sync_upstream.py`: a file it lists that upstream no longer has fails the run, and one
   upstream added shows up in the `not copied:` lines. Add or remove it there and update the list below.
4. `./gradlew clean test jar --offline` and a run in game.

## What is copied

21 Java files and 12 resources.

- **13 Java files, verbatim after the rename:** `GLBuffer`, `GpuFloatBuffer`, `GpuIntBuffer`, `Mat4`,
  `ModelUploader`, `RegionManager`, `SceneUploader`, `Shader`, `ShaderException`, `TextureManager`, `VAO`,
  `VBO`, `Zone`.
- **6 more in sub-packages, verbatim after the rename:** `config/AntiAliasingMode`, `config/ColorBlindMode`,
  `config/UIScalingMode`, `regions/Region`, `regions/Regions`, `template/Template`.
- **2 with a patch:** `BetterSkyboxPlugin.java`, `BetterSkyboxConfig.java`.
- **12 resources, verbatim:** `colorblind.glsl`, `frag.glsl`, `fragui.glsl`, `hsl_to_rgb.glsl`, `vert.glsl`,
  `vertui.glsl`, `scale/bicubic.glsl`, `scale/hybrid.glsl`, `scale/xbr_lv2_common.glsl`,
  `scale/xbr_lv2_frag.glsl`, `scale/xbr_lv2_vert.glsl`, `regions/regions.txt`. The rename rules match nothing
  in them; they are byte copies.

The 33 paths are pinned in `COPIED` in `tools/sync_upstream.py`, so a copy deleted from `src/` fails the run
instead of dropping out of it. Upstream's `.clang-format` is not copied; `--check` names it under "not copied"
and does not fail on it, and any upstream file a client bump adds shows up in the same place.

## Intentional deltas

### `patches/BetterSkyboxPlugin.patch`

Seven hooks, nothing else. The sky itself lives in `SkyPass`.

| Site | Hook |
| --- | --- |
| `@PluginDescriptor` | name, description, tags, `conflicts = {"GPU", "117 HD"}` |
| fields | `@Inject private SkyPass skyPass;` |
| `startUp` | `skyPass.init(createTemplate());` |
| `shutDown` | `skyPass.shutdown();` |
| `onConfigChanged` | `skyPass.onConfigChanged(configChanged);` |
| `preSceneDrawToplevel` | `skyPass.beginFrame(scene)` and `skyPass.fogColor(client.getSkyboxColor())` |
| `preSceneDrawToplevel` | the sky pass draws in place of `drawSkybox` when it is on |

`drawSkybox` itself is untouched: when the sky pass is off, the game's own skybox model is drawn exactly as
upstream draws it.

### `patches/BetterSkyboxConfig.patch`

Bigger, and mechanical. Every upstream `@ConfigItem` keeps its `keyName`, name, description and default;
what changes is where it sits in the panel.

- `GROUP` is `betterskybox`, not `gpu`, so the renderer settings are ours and cannot collide with the stock
  plugin's.
- Every upstream item gains `section = rendererSection` (or `fogSection` for `fogDepth`) and a renumbered
  `position`, so the stock settings collapse into one section above the sky sections.
- Five `@ConfigSection` constants, four enums (`SkyMode`, `SkyboxTexture`, `SkyPreset`, `FogColorMode`) and
  about 40 sky `@ConfigItem`s are appended.

Upstream config changes conflict on the section and position lines every time. That is the price of keeping
the panel readable; the alternative (a `GpuRendererConfig` base interface that Better Skybox extends) loses
the collapsible Renderer group.

## Not covered by the check

Files this plugin owns outright. They have no upstream counterpart and `--apply` never touches them:
`SkyPass`, `SkyboxRenderer`, `ProceduralSkyRenderer`, `StarMap`, `CubemapLoader`, `CubemapLoads`, `SkyAreas`,
`SkyClock`, `Lightning`, `BorderBlend`, `GpuSettingsImport`, the `sky_*.glsl` shaders, `sky_areas.json`,
`sky_gradient.json` and the bundled cubemaps under `skybox/`.
