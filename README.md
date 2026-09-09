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
| Cubemap | RS3_GREENLANDS | RS3_GREENLANDS, RS3_MAGICBLUE, DEBUG, or CUSTOM |
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

Terrain fog itself is the stock "Fog depth" setting at the top of the panel; 0 turns it off.
Draw distance, AA and the rest are the same as the stock GPU plugin but live in their own config group
(`gpuskybox`), so set them again once.

## Layout

- `src/main/java/com/gpuskybox/` copy of `net.runelite.client.plugins.gpu` (renamed), plus
  `SkyboxRenderer.java` (cubemap), `ProceduralSkyRenderer.java` (gradient sky), `Agent.java` (launcher hook)
- `src/main/resources/com/gpuskybox/` GPU shaders, `sky_vert.glsl`, `sky_frag.glsl`, `proc_sky_frag.glsl`,
  `sky_gradient.json`, `skybox/<name>/` textures
- `ref/` (gitignored) sparse RuneLite checkout, example-plugin template, PR 558 and PR 655 files

## Updating the client version

`build.gradle` pins `runeLiteVersion`. When RuneLite updates, bump it and diff
`ref/runelite/.../plugins/gpu` against `src/main/java/com/gpuskybox` to pull in upstream renderer changes.
