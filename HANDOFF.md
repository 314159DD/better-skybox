# Better Skybox HANDOFF

Last wrap: 2026-09-10 00:20 (hub submission)

## What this is
RuneLite external plugin: the stock GPU renderer copied into `com.betterskybox`, plus a sky pass
(cubemap texture or procedural day/night sky). Repo: https://github.com/314159DD/better-skybox (private
until Steven flips it). Loaded into the official launcher via `build/libs/better-skybox-agent.jar`
(`-javaagent`, see CONTRIBUTING.md); README.md is the hub-facing page, CONTRIBUTING.md the dev side.

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
7. DONE 2026-09-09 evening (branch sprint/hub-ready, 6 SDD tasks): rename to Better Skybox + com.betterskybox, agent in
   its own source set, LICENSE/NOTICE, SkyPass split (plugin diff vs upstream 25 lines) + tools/sync_upstream.py,
   11 correctness fixes, async cubemap decode, GPU settings import (16 keys, once), 62 tests, README/CONTRIBUTING,
   icon + banner. GitHub: 314159DD/better-skybox (PRIVATE, no AI attribution anywhere, keep it that way; old repo
   better-skybox-old to delete).
8. DONE 2026-09-10: Task 7 (NASA star map over night cubemaps), final review + fix wave (12 findings), Task 8
   (jar 262 KB: no cubemaps and no star map in the jar; ONE optional sky pack, PNG originals, 36.3 MB, via the
   "Download sky pack" setting into ~/.runelite/better-skybox/pack; PROCEDURAL is the default sky). 92 tests.
   Repo PUBLIC, release sky-pack-v1 live, plugin-hub PR #16334 open (fork 314159DD/plugin-hub, branch
   better-skybox, file plugins/better-skybox = commit 09ecfa8). Hub packager facts: jar limit 10 MiB hard
   (warning at 8), standard build compiles src/main only, client 1.12.38.
9. NEXT: (a) Steven tests the pack download in the client (toggle Download sky pack, wait for the "installed"
   chat line, check NIGHT cubemap + star map). (b) Watch PR #16334 CI; on a failure fix, push, and update the
   commit= hash in plugins/better-skybox on the fork branch (same PR). (c) Rename the on-disk folder to
   better-skybox when the client is closed (launcher settings.json path follows). (d) Later packs: new tag,
   new PACK_URL in SkyPack.java.
