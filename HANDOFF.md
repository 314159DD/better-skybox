# gpu-skybox HANDOFF

Last wrap: 2026-09-09 (session after the 2026-09-09 01:37 crash)

## What this is
RuneLite external plugin: the stock GPU renderer copied into `com.gpuskybox`, plus a sky pass
(cubemap texture or procedural day/night sky). Loaded into the official launcher via
`build/libs/gpu-skybox-agent.jar` (`-javaagent`, see README). Repo has ONE commit (initial).

## State
1. DONE: cubemap sky (RS3 greenlands / magicblue / debug / custom folder), procedural sky with
   presets, fog colour modes, horizon blend, agent jar + launcher settings.json wired.
2. DONE (this session): procedural sky extras finished. Config now has CYCLE preset + cycleMinutes,
   sunDisk, moonSize, moonPhase, starBrightness, shootingStars, nebula, aurora, cloudCover, cloudSpeed.
   `gradlew agentJar` builds clean. NOT yet seen in-game after this change.
3. DONE (2026-09-09 midday): 12 Poly Haven CC0 pure skies bundled (jar now 21 MB), converter takes
   `--file/--name/--custom` for local panoramas, `SkyAreas` + `sky_areas.json` (10 coarse areas) pick the
   cubemap per map region, `SkyboxRenderer` caches textures and crossfades (config: Sky by area, Area fade).
   Builds clean. NOTHING of this seen in-game yet.
4. NEXT: in-game check. (a) Sky type = CUBEMAP, walk Lumbridge -> Al Kharid -> desert, expect the log
   line `Sky area: Kharidian Desert -> ...` and a 4 s crossfade. (b) Sky type = PROCEDURAL, Time of day =
   CYCLE, Cycle length 1, watch one day. Then tune rectangles in sky_areas.json (they are guesses).
5. AFTER: Steven's research agent reports on 360-degree image-gen tools; themed skies (swamp, wildy
   storm, ...) then go through `--file --name <n> --custom` and get referenced from sky_areas.json.

## Gotchas
- Compile with `./gradlew agentJar --offline -q` (7 s). Restart the client to pick up the jar.
- `ref/` is gitignored: sparse RuneLite checkout + 117 HD PR 558/655 files, needed for diffs only.
- `run.bat` dev client = legacy login only on this machine.
- Config positions: procedural items 35-49, general sky items 51-59, area items 60-61.
- Poly Haven API needs a User-Agent header (403 otherwise); the script sets one.
