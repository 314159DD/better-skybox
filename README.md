# GPU Skybox

The stock RuneLite GPU plugin, copied into a sideloadable external plugin, plus a real sky:
a cubemap texture or a procedural day/night sky. Cubemaps and the procedural sky model come from
117 HD PRs #558 (RuffledPlume) and #655 (3-X).

## Run (Jagex account, official launcher)

`build/libs/gpu-skybox-agent.jar` is a `-javaagent` that registers the plugin inside the launcher-managed
client. The RuneLite launcher's own settings file, `settings.json` next to `RuneLite.exe`
(`%LOCALAPPDATA%\RuneLite\settings.json`), needs launch mode JVM and the agent:

```json
{
  "launchMode": "JVM",
  "jvmArguments": ["-javaagent:C:\\Coding\\III____Full_Circle\\gpu-skybox\\build\\libs\\gpu-skybox-agent.jar"]
}
```

Then start RuneLite from the Jagex Launcher as usual. Enable **GPU Skybox** in the plugin list once;
it declares a conflict with the stock GPU plugin (and 117 HD), so RuneLite switches those off by itself.
Rebuild with `gradlew agentJar` and restart the client to pick up changes. Delete `settings.json` to go back to stock.

`run.bat` starts a dev client from Gradle instead (no Jagex login on this machine, legacy login only).

## Settings (plugin panel, section "Skybox")

| Setting | Default | What it does |
| --- | --- | --- |
| Enable sky | on | sky pass instead of the flat fog colour |
| Sky type | CUBEMAP | CUBEMAP = texture, PROCEDURAL = gradient sky with sun, moon, stars |
| Cubemap | RS3_GREENLANDS | RS3_GREENLANDS, RS3_MAGICBLUE, twelve Poly Haven skies (PARTLY_CLOUDY, CLEAR, OVERCAST, MISTY, STORM_CLOUDS, MOUNTAIN, SNOW, SUNSET, HAZY_SUNSET, DUSK, MOONRISE, NIGHT), DEBUG, or CUSTOM. With Sky by area on, this is the fallback for unmapped areas |
| Custom cubemap folder | | folder under `~/.runelite/gpu-skybox/<name>/`: six `px nx py ny pz nz .png`, or one `skybox.png` 4x2 atlas (px nz nx pz / py ny) |
| Time of day | DAY | procedural: DAY, SUNRISE, DAWN, SUNSET, DUSK, NIGHT, BLOOD_MOON, CLOCK (local time), CYCLE (full day in `Cycle length` minutes), CUSTOM (sliders) |
| Sun altitude / azimuth | 30 / 235 | procedural, Time of day = CUSTOM |
| Moon, Stars | on | procedural |
| Cycle length | 24 | procedural, Time of day = CYCLE: real minutes per in-game day |
| Sun disk | on | procedural: draw the sun itself, not only the glow |
| Moon size / Moon phase | 100 / 100 | procedural, percent; phase 0 = new moon |
| Star brightness | 100 | procedural, percent |
| Shooting stars, Nebula, Aurora | on / on / off | procedural night-sky extras |
| Cloud cover / Cloud speed | 30 / 100 | procedural, percent; 0 cover = clear sky, 0 speed = still |
| Overworld only | on | underground keeps the flat colour |
| Prefer game skybox | off | where Jagex ships a skybox model, draw that instead |
| Fog colour | AUTO | AUTO / SKYBOX (sky horizon colour) / GAME (client colour, black without the Skybox colour plugin) / CUSTOM |
| Custom fog colour | light blue-grey | used with Fog colour = CUSTOM |
| Horizon blend | 5 | width of the fog to sky fade above the horizon, 0 = off |
| Fog tint | 15 | share of fog colour mixed into the sky |
| Sky brightness | 100 | multiplier in percent |
| Rotation / Drift speed | 0 / 0 | cubemap: static offset in degrees, drift in degrees per minute |
| Sky by area | on | cubemap: pick the sky from the map area (Wilderness, Morytania, desert, Karamja, Fremennik, Tirannwn, Kourend, Varlamore, ...) |
| Area fade | 4 | cubemap: crossfade seconds when the area sky changes, 0 = instant |

Terrain fog itself is the stock "Fog depth" setting at the top of the panel; 0 turns it off.
Draw distance, AA and the rest are the same as the stock GPU plugin but live in their own config group
(`gpuskybox`), so set them again once.

## Sky by area

`src/main/resources/com/gpuskybox/sky_areas.json` maps region rectangles to cubemap folders:

```json
{"name": "Wilderness", "sky": "wasteland_clouds_puresky", "regions": [[46, 55, 54, 63]]}
```

`regions` are `[x1, y1, x2, y2]` in region coordinates (region id = `x << 8 | y`), inclusive; the first
matching entry wins, so list small areas before the land they sit in. Drop a `sky_areas.json` into
`~/.runelite/gpu-skybox/` to replace the bundled table without rebuilding. The client log prints
`Sky area: <name> -> <sky>` on every change, which is the quickest way to tune the rectangles in-game.
The bundled rectangles are coarse first guesses.

## Adding skies

`tools/polyhaven_to_cubemap.py` (needs `numpy`, `pillow`) turns a 2:1 equirectangular panorama into the six faces:

```
python tools/polyhaven_to_cubemap.py kloppenheim_06_puresky          # CC0 sky from Poly Haven, bundled
python tools/polyhaven_to_cubemap.py --file pano.png --name swamp --custom   # local image, e.g. AI generated, to ~/.runelite/gpu-skybox/swamp/
```

Bundled skies also need an entry in `GpuSkyboxConfig.SkyboxTexture`; custom folders are picked with
Cubemap = CUSTOM plus the folder name, or referenced by folder name from `sky_areas.json`.
Poly Haven's `*_puresky` assets are sky-only panoramas and convert cleanly; anything with ground in it
shows that ground below the horizon.

## Layout

- `src/main/java/com/gpuskybox/` copy of `net.runelite.client.plugins.gpu` (renamed), plus
  `SkyboxRenderer.java` (cubemap cache + crossfade), `ProceduralSkyRenderer.java` (gradient sky), `SkyAreas.java` (region to sky table), `Agent.java` (launcher hook)
- `src/main/resources/com/gpuskybox/` GPU shaders, `sky_vert.glsl`, `sky_frag.glsl`, `proc_sky_frag.glsl`,
  `sky_gradient.json`, `sky_areas.json`, `skybox/<name>/` textures
- `ref/` (gitignored) sparse RuneLite checkout, example-plugin template, PR 558 and PR 655 files

## Updating the client version

`build.gradle` pins `runeLiteVersion`. When RuneLite updates, bump it and diff
`ref/runelite/.../plugins/gpu` against `src/main/java/com/gpuskybox` to pull in upstream renderer changes.
