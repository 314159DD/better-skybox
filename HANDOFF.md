# gpu-skybox HANDOFF

Last wrap: 2026-09-09 afternoon

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
3. DONE (2026-09-09 midday): 12 Poly Haven CC0 skies, converter --file/--name/--custom, area skies with
   crossfade (Sky by area, Area fade).
4. DONE (2026-09-09 afternoon, commit after 71b6edb): research-driven expansion.
   - 117 HD areas.json + environments.json -> tools/hd_to_sky_areas.py -> sky_areas.json (147 areas, 60
     with a sky, all with 117 HD fog colour). Matching on world tiles + region boxes, both modes. AUTO fog
     uses the area colour. Lookups for 28 known places verified against the JSON in Python.
   - NASA Deep Star Map 8k -> tools/starmap_to_cubemap.py -> skybox/stars (1024 px faces). Procedural
     sky samples it (Real star map, default on), sky turns with the hour around a pole tilted 40 degrees.
   - 8 more CC0 cubemaps (5 ambientCG, 3 OpenGameArt stylised). Jar is 39 MB now.
   - CubemapLoader split out of SkyboxRenderer so both renderers share it.
   NOTHING of the day's work has been seen in-game yet.
5. NEXT: in-game check, in this order. (a) CUBEMAP mode, walk Lumbridge -> Al Kharid -> Shantay Pass:
   log `Sky area: KHARIDIAN_DESERT ...`, crossfade, fog turns sand-coloured. (b) PROCEDURAL, Time of day =
   NIGHT: Milky Way visible, stars sharp, no seam; then CYCLE with Cycle length 1 to see it turn.
   (c) Morytania in daylight: the dark 117 HD fog (#1e314b) under an overcast sky may look wrong; if so,
   switch Fog colour to SKYBOX or lighten fogs in hd_to_sky_areas.py.
6. AFTER: themed AI skies are off the table for now (Blockade Labs declined). Candidates from the research
   still open: Spacescape (MIT) for procedural night skies, Kenney stylised sets, ambientCG sweep for more.

## Gotchas
- Compile with `./gradlew agentJar --offline -q` (7 s). Restart the client to pick up the jar.
- `ref/` is gitignored: sparse RuneLite checkout + 117 HD PR 558/655 files, needed for diffs only.
- `run.bat` dev client = legacy login only on this machine.
- Config panel: five sections (Renderer 10, Sky 20, Cubemap 30, Procedural 40, Fog 50), positions restart at 1 inside each. Keys unchanged.
- Poly Haven and ambientCG APIs need a User-Agent header (403 otherwise); the scripts set one.
- 117 HD regionBoxes are two CORNER region ids of a box, not an id range. Cost one wrong lookup pass.
- EXR reading: cv2 with OPENCV_IO_ENABLE_OPENEXR=1 (set in starmap_to_cubemap.py); no OpenEXR package needed.
- The Bash tool chokes on inline python heredocs containing triple quotes or escaped apostrophes; write scripts to the scratchpad and run them.
