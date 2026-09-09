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
   - 117 HD areas.json + environments.json -> tools/hd_to_sky_areas.py -> sky_areas.json (162 areas, 75
     with a sky, all with 117 HD fog colour). Matching on world tiles + region boxes, both modes. AUTO fog
     uses the area colour. Misthalin/Asgarnia/Kandarin + 13 islands are our own boxes in EXTRA (117 HD has
     none). Lookups for 37 known places verified against the JSON in Python, 0 unmapped.
   - NASA Deep Star Map 8k -> tools/starmap_to_cubemap.py -> skybox/stars (1024 px faces). Procedural
     sky samples it (Real star map, default on), sky turns with the hour around a pole tilted 40 degrees.
   - 8 more CC0 cubemaps (5 ambientCG, 3 OpenGameArt stylised). Jar is 39 MB now.
   - CubemapLoader split out of SkyboxRenderer so both renderers share it.
   NOTHING of the day's work has been seen in-game yet.
5. NEXT: none of sprint/sky-features-2 has been seen in-game yet. In-game checklist, in this order:
   (a) Time of day = CYCLE, Cycle length 1, in Lumbridge: dawn, day, sunset and night cubemaps all cycle
   inside one minute. (b) walk into the Wilderness at level 30+: lightning flashes start within 22 s.
   (c) Sky type = PROCEDURAL, walk into Canifis: dusk regardless of the Time of day setting (area mood).
   (d) walk back and forth across the Al Kharid gate a few times: no pop, the sky eases across the border
   both ways.
6. DONE (2026-09-09, sprint/sky-features-2): five features shipped on top of the shared SkyClock -
   real moon phase (CLOCK/CYCLE follow the calendar instead of the Moon phase slider), time-of-day
   cubemaps (dawn/day/dusk/night per area with global phase defaults), lightning flashes in 117 HD storm
   areas (sky and fog), area moods (areas force a procedural preset over Time of day), and border blending
   by distance (Area fade is now tiles walked past the border, not seconds; stepping back reverses instead
   of restarting). Commit REPLACE_WITH_HASH. Themed AI skies are still off the table (Blockade Labs
   declined); Spacescape (MIT), Kenney stylised sets and an ambientCG sweep remain open candidates for a
   later round.

## Gotchas
- Compile with `./gradlew agentJar --offline -q` (7 s). Restart the client to pick up the jar.
- `ref/` is gitignored: sparse RuneLite checkout + 117 HD PR 558/655 files, needed for diffs only.
- `run.bat` dev client = legacy login only on this machine.
- Config panel: five sections (Renderer 10, Sky 20, Cubemap 30, Procedural 40, Fog 50), positions restart at 1 inside each. Keys unchanged.
- Poly Haven and ambientCG APIs need a User-Agent header (403 otherwise); the scripts set one.
- 117 HD regionBoxes are two CORNER region ids of a box, not an id range. Cost one wrong lookup pass.
- EXR reading: cv2 with OPENCV_IO_ENABLE_OPENEXR=1 (set in starmap_to_cubemap.py); no OpenEXR package needed.
- The Bash tool chokes on inline python heredocs containing triple quotes or escaped apostrophes; write scripts to the scratchpad and run them.
