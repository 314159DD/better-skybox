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
3. NEXT: start RuneLite from the Jagex Launcher, enable GPU Skybox, set Sky type = PROCEDURAL,
   Time of day = CYCLE with Cycle length 1, and check sun/moon/stars/clouds/aurora over one cycle.
4. AFTER: `python tools/polyhaven_to_cubemap.py <asset>` (numpy + PIL present) to add Poly Haven
   skies, then add the folder name to `GpuSkyboxConfig.SkyboxTexture`. Asset names not chosen yet.

## Gotchas
- Compile with `./gradlew agentJar --offline -q` (7 s). Restart the client to pick up the jar.
- `ref/` is gitignored: sparse RuneLite checkout + 117 HD PR 558/655 files, needed for diffs only.
- `run.bat` dev client = legacy login only on this machine.
- Config positions: procedural items 35-49, general sky items 51-59.
