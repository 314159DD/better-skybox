# gpu-skybox HANDOFF

Last wrap: 2026-09-09 late afternoon (crash recovery session)

## What this is
RuneLite external plugin: the stock GPU renderer copied into `com.gpuskybox`, plus a sky pass
(cubemap texture or procedural day/night sky). Loaded into the official launcher via
`build/libs/gpu-skybox-agent.jar` (`-javaagent`, see README).

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
5. DONE 2026-09-09: in-game checklist passed (Steven). Was:
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
   of restarting). Commit 29de069. Final whole-branch review (opus) + fix wave landed as bc259f6:
   teleports/plane changes and time-of-day switches hand the transition back to the seconds fade, lightning
   flash is gated on the frame's draw decision, manual Cubemap fallback is real, 21 unit tests green.
   In-game checklist passed, branch merged into master (ff, a8108b5). Distribution decision: RuneLite Plugin Hub as "Better Skybox" (name "Skybox" is taken by a flat-colour plugin); next sprint = hub-ready. Themed AI skies are still off the table (Blockade Labs
   declined); Spacescape (MIT), Kenney stylised sets and an ambientCG sweep remain open candidates for a
   later round.
7. NEXT: Plugin Hub road. Plan: plan/2026-09-09-hub-ready.md (4 sprints hub-1..hub-4 + the page). Review with all
   findings: .superpowers/sdd/hub-ready-review.md. Start with hub-1 (packaging). RS3 rips already dropped (5d35239).

## Gotchas
- Compile with `./gradlew agentJar --offline -q` (7 s). Restart the client to pick up the jar.
- `ref/` is gitignored: sparse RuneLite checkout + 117 HD PR 558/655 files, needed for diffs only.
- `run.bat` dev client = legacy login only on this machine.
- Config panel: five sections (Renderer 10, Sky 20, Cubemap 30, Procedural 40, Fog 50), positions restart at 1 inside each. Keys unchanged.
- Poly Haven and ambientCG APIs need a User-Agent header (403 otherwise); the scripts set one.
- 117 HD regionBoxes are two CORNER region ids of a box, not an id range. Cost one wrong lookup pass.
- EXR reading: cv2 with OPENCV_IO_ENABLE_OPENEXR=1 (set in starmap_to_cubemap.py); no OpenEXR package needed.
- The Bash tool chokes on inline python heredocs containing triple quotes or escaped apostrophes; write scripts to the scratchpad and run them.
