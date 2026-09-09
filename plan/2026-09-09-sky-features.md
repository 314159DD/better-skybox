# Sky Features Round 2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the GPU Skybox plugin a day/night cycle for cubemaps, lightning in stormy areas, per-area procedural moods, a real moon phase, and distance-based blending at area borders.

**Architecture:** One `SkyClock` owns "what time is it in the sky" for both renderers. `sky_areas.json` (generated from 117 HD tables by `tools/hd_to_sky_areas.py`) grows optional per-area fields (`skyDawn/skyDusk/skyNight`, `lightning`, `preset`); `SkyAreas.Area` mirrors them. The plugin decides per frame which cubemap and which fog colour apply and feeds both shaders the same `flash` and `horizonOffset` uniforms. Pure logic lives in small classes (`SkyClock`, `Lightning`, `BorderBlend`) so it is unit-testable without OpenGL.

**Tech Stack:** Java 11 (RuneLite 1.12.38 client API, LWJGL GL33), GLSL 330, Gradle 8.10, JUnit 4, Python 3 (numpy, Pillow) for asset tooling.

**Spec:** This plan implements the five features agreed in the 2026-09-09 session (see `HANDOFF.md`, section 6, and the feature list below). No separate spec file exists; the feature descriptions in each task are the spec.

## Global Constraints

- Java source level 11 (`options.release.set(11)` in `build.gradle`); no `record`, no `switch` expressions, no `var` in fields.
- RuneLite code style: tabs, Allman braces (`{` on its own line), one class per file, `@Slf4j` for logging.
- Config keys never change once shipped (users keep their values); new settings get new keys.
- Config panel sections and their positions: Renderer 10, Sky 20, Cubemap sky 30, Procedural sky 40, Fog and blending 50. Positions inside a section restart at 1.
- All GL calls happen on the client thread inside the draw callbacks; never touch GL from `@Subscribe` handlers directly.
- Build: `./gradlew agentJar --offline -q` (jar at `build/libs/gpu-skybox-agent.jar`, launcher picks it up on client restart). Tests: `./gradlew test` (first run needs network to fetch JUnit; afterwards `--offline` works).
- No em dashes anywhere (code comments, docs, config descriptions).
- Every task ends with a commit; commit messages end with the session attribution lines already used in this repo (see `git log`).
- `sky_areas.json` is generated: never hand-edit it, edit `tools/hd_to_sky_areas.py` and rerun `python tools/hd_to_sky_areas.py`.

## Feature list (the spec)

1. **Time-of-day cubemaps.** In CUBEMAP mode the sky follows the same clock as the procedural sky (Time of day: presets, CLOCK, CYCLE). Four phases: DAWN, DAY, DUSK, NIGHT. Each area may name a cubemap per phase; missing phases fall back to global defaults chosen in the config (Dawn / Dusk / Night cubemap dropdowns). Switching phases crossfades over the existing time-based fade.
2. **Lightning.** In areas 117 HD flags with `lightningEffects` (Wilderness high, Barrows, Darkmeyer, Draynor Manor, Tempoross), random flashes brighten the sky and the fog for a few frames. Toggle in the Sky section, on by default. Works in both sky modes.
3. **Area moods for the procedural sky.** Some areas force a procedural preset (Morytania DUSK, Darkmeyer and Vampyrium BLOOD_MOON, Kharidian desert DAY, Lunar Isle NIGHT) regardless of the user's Time of day. Toggle "Area moods" in the Procedural section, on by default.
4. **Real moon phase.** With Time of day = CLOCK the moon's illumination follows the real calendar; with CYCLE it advances one lunar month per 29.5 cycles. Presets and CUSTOM keep the Moon phase slider.
5. **Border blending.** An area change blends by distance walked from the crossing tile instead of by wall-clock time, so stepping back and forth across a border never pops. Teleports still fade instantly-ish (one tile step to full). Time-of-day changes keep the seconds-based fade.

## File structure

| File | Responsibility |
| --- | --- |
| `src/main/java/com/gpuskybox/SkyClock.java` (new) | hour of day from config, phase buckets, moon illumination, elapsed seconds. Pure. |
| `src/main/java/com/gpuskybox/Lightning.java` (new) | strike scheduler + flash intensity curve. Pure, seeded. |
| `src/main/java/com/gpuskybox/BorderBlend.java` (new) | distance-based blend progress with bounce-back. Pure. |
| `src/main/java/com/gpuskybox/SkyAreas.java` | + fields `skyDawn`, `skyDusk`, `skyNight`, `lightning`, `preset`; `skyFor(Phase)` helper |
| `src/main/java/com/gpuskybox/ProceduralSkyRenderer.java` | uses `SkyClock`; accepts a preset override; `flash` uniform |
| `src/main/java/com/gpuskybox/SkyboxRenderer.java` | `flash` uniform; `overrideBlend` for border blending |
| `src/main/java/com/gpuskybox/GpuSkyboxPlugin.java` | owns `SkyClock`, `Lightning`, `BorderBlend`; picks cubemap per phase; fog flash |
| `src/main/java/com/gpuskybox/GpuSkyboxConfig.java` | new items (see each task) |
| `src/main/resources/com/gpuskybox/sky_frag.glsl`, `proc_sky_frag.glsl` | `uniform float flash` |
| `tools/hd_to_sky_areas.py` | emits `lightning`, `preset`, `skyNight` etc. |
| `src/test/java/com/gpuskybox/*Test.java` (new) | JUnit 4 tests for the pure classes |

---

### Task 0: JUnit test infrastructure

**Files:**
- Modify: `build.gradle` (dependencies block, lines 22-27)
- Create: `src/test/java/com/gpuskybox/SmokeTest.java`

**Interfaces:**
- Produces: a working `./gradlew test` so every later task can run its tests.

- [ ] **Step 1: Add JUnit 4 to the test classpath**

In `build.gradle`, inside `dependencies { ... }`, after the `testImplementation group: 'net.runelite'...` line add:

```groovy
	testImplementation 'junit:junit:4.13.2'
```

and after the `tasks.withType(JavaCompile)` block add:

```groovy
tasks.named('test') {
	useJUnit()
}
```

- [ ] **Step 2: Write a smoke test**

`src/test/java/com/gpuskybox/SmokeTest.java`:

```java
package com.gpuskybox;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class SmokeTest
{
	@Test
	public void junitRuns()
	{
		assertEquals(4, 2 + 2);
	}
}
```

- [ ] **Step 3: Run the tests (online once, to fetch JUnit)**

Run: `./gradlew test -q`
Expected: BUILD SUCCESSFUL, no failures. Then confirm `./gradlew test --offline -q` also passes.

- [ ] **Step 4: Commit**

```bash
git add build.gradle src/test/java/com/gpuskybox/SmokeTest.java
git commit -m "test: JUnit 4 on the test classpath"
```

---

### Task 1: SkyClock, shared time source

**Files:**
- Create: `src/main/java/com/gpuskybox/SkyClock.java`
- Create: `src/test/java/com/gpuskybox/SkyClockTest.java`
- Modify: `src/main/java/com/gpuskybox/ProceduralSkyRenderer.java` (fields `startNanos`, `hour`; methods `elapsedSeconds`, `update`, `altitudeForHour`, `azimuthForHour`, `hourForAzimuth`, `draw`)
- Modify: `src/main/java/com/gpuskybox/GpuSkyboxPlugin.java` (field next to `skyAreas`; `shouldDrawCubemap` where `proceduralSky.update(config)` is called; `drawSkybox` where `proceduralSky.draw(...)` is called)
- Modify: `src/main/java/com/gpuskybox/GpuSkyboxConfig.java` (`skyPreset`, `cycleMinutes` move to `skySection`)

**Interfaces:**
- Produces:
  - `enum SkyClock.Phase { NIGHT, DAWN, DAY, DUSK }`
  - `float SkyClock.hour(GpuSkyboxConfig config)` (0..24, wraps)
  - `static SkyClock.Phase SkyClock.phase(float hour)`
  - `static float SkyClock.altitudeForHour(float hour)`, `static float SkyClock.azimuthForHour(float hour)`, `static float SkyClock.hourForAzimuth(float azimuth)`
  - `float SkyClock.elapsedSeconds()`
  - `void ProceduralSkyRenderer.update(GpuSkyboxConfig config, SkyClock clock)` and `void ProceduralSkyRenderer.draw(float[] skyProj, int fog, int quadVao, GpuSkyboxConfig config, SkyClock clock)`

- [ ] **Step 1: Write the failing test**

`src/test/java/com/gpuskybox/SkyClockTest.java`:

```java
package com.gpuskybox;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class SkyClockTest
{
	@Test
	public void phasesCoverTheDay()
	{
		assertEquals(SkyClock.Phase.NIGHT, SkyClock.phase(0f));
		assertEquals(SkyClock.Phase.NIGHT, SkyClock.phase(4.99f));
		assertEquals(SkyClock.Phase.DAWN, SkyClock.phase(5f));
		assertEquals(SkyClock.Phase.DAWN, SkyClock.phase(6.99f));
		assertEquals(SkyClock.Phase.DAY, SkyClock.phase(7f));
		assertEquals(SkyClock.Phase.DAY, SkyClock.phase(16.99f));
		assertEquals(SkyClock.Phase.DUSK, SkyClock.phase(17f));
		assertEquals(SkyClock.Phase.DUSK, SkyClock.phase(19.99f));
		assertEquals(SkyClock.Phase.NIGHT, SkyClock.phase(20f));
		assertEquals(SkyClock.Phase.NIGHT, SkyClock.phase(23.99f));
	}

	@Test
	public void sunGeometryRoundTrips()
	{
		assertEquals(60f, SkyClock.altitudeForHour(12f), 1e-4f);
		assertEquals(-60f, SkyClock.altitudeForHour(0f), 1e-4f);
		assertEquals(90f, SkyClock.azimuthForHour(6f), 1e-4f);
		assertEquals(180f, SkyClock.azimuthForHour(12f), 1e-4f);
		assertEquals(12f, SkyClock.hourForAzimuth(180f), 1e-4f);
		assertEquals(0f, SkyClock.hourForAzimuth(0f), 1e-4f);
	}
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --offline -q`
Expected: compilation error, `SkyClock` does not exist.

- [ ] **Step 3: Create SkyClock**

`src/main/java/com/gpuskybox/SkyClock.java`:

```java
package com.gpuskybox;

import java.time.LocalTime;
import com.gpuskybox.GpuSkyboxConfig.SkyPreset;

/**
 * The one place that answers "what time is it in the sky". Both renderers and the plugin ask it, so the
 * cubemap phase, the procedural sun and the star map always agree.
 */
class SkyClock
{
	enum Phase
	{
		NIGHT,
		DAWN,
		DAY,
		DUSK
	}

	private final long startNanos = System.nanoTime();

	float elapsedSeconds()
	{
		return (float) ((System.nanoTime() - startNanos) / 1e9);
	}

	/** Hour of the sky day in [0, 24). Presets map to the hour their sun azimuth implies. */
	float hour(GpuSkyboxConfig config)
	{
		SkyPreset preset = config.skyPreset();
		switch (preset)
		{
			case CLOCK:
			{
				LocalTime now = LocalTime.now();
				return now.getHour() + now.getMinute() / 60f + now.getSecond() / 3600f;
			}
			case CYCLE:
			{
				float day = elapsedSeconds() / (config.cycleMinutes() * 60f);
				return (6 + day * 24) % 24;
			}
			case CUSTOM:
				return hourForAzimuth(config.sunAzimuth());
			default:
				return hourForAzimuth(preset.azimuth);
		}
	}

	static Phase phase(float hour)
	{
		if (hour < 5 || hour >= 20)
		{
			return Phase.NIGHT;
		}
		if (hour < 7)
		{
			return Phase.DAWN;
		}
		if (hour < 17)
		{
			return Phase.DAY;
		}
		return Phase.DUSK;
	}

	/** Sun altitude in degrees for a 24h clock: -60 at midnight, +60 at noon. */
	static float altitudeForHour(float hour)
	{
		return (float) Math.sin((hour - 6) / 24 * 2 * Math.PI) * 60;
	}

	/** East at 6h, south at noon, west at 18h. */
	static float azimuthForHour(float hour)
	{
		return 90 + (hour - 6) * 15;
	}

	static float hourForAzimuth(float azimuth)
	{
		return ((azimuth - 90) / 15 + 6 + 24) % 24;
	}
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --offline -q`
Expected: PASS.

- [ ] **Step 5: Make ProceduralSkyRenderer use SkyClock**

In `ProceduralSkyRenderer.java`:

1. Delete the field `private final long startNanos = System.nanoTime();`, the method `elapsedSeconds()`, and the three static methods `altitudeForHour`, `azimuthForHour`, `hourForAzimuth` (they now live in `SkyClock`). Delete the `import java.time.LocalTime;` line.
2. Replace the whole `update` method head (from `void update(GpuSkyboxConfig config)` through `starRotation(hour, starRot);`) with:

```java
	void update(GpuSkyboxConfig config, SkyClock clock)
	{
		SkyPreset preset = config.skyPreset();
		hour = clock.hour(config);
		float altitude, azimuth;
		switch (preset)
		{
			case CLOCK:
			case CYCLE:
				altitude = SkyClock.altitudeForHour(hour);
				azimuth = SkyClock.azimuthForHour(hour);
				break;
			case CUSTOM:
				altitude = config.sunAltitude();
				azimuth = config.sunAzimuth();
				break;
			default:
				altitude = preset.altitude;
				azimuth = preset.azimuth;
		}
		starRotation(hour, starRot);
```

The rest of `update` (the `direction(sunDir, ...)`, blood moon branch, `evaluate(...)` calls) stays unchanged.

3. Change the `draw` signature to `void draw(float[] skyProj, int fog, int quadVao, GpuSkyboxConfig config, SkyClock clock)` and replace `float seconds = elapsedSeconds();` with `float seconds = clock.elapsedSeconds();`.

- [ ] **Step 6: Wire the clock in the plugin**

In `GpuSkyboxPlugin.java`:

1. Next to `private final SkyAreas skyAreas = new SkyAreas();` add `private final SkyClock skyClock = new SkyClock();`.
2. In `shouldDrawCubemap`, change `proceduralSky.update(config);` to `proceduralSky.update(config, skyClock);`.
3. In `drawSkybox`, change `proceduralSky.draw(skyProj, sky, vaoUiHandle, config);` to `proceduralSky.draw(skyProj, sky, vaoUiHandle, config, skyClock);`.

- [ ] **Step 7: Move Time of day and Cycle length to the Sky section**

In `GpuSkyboxConfig.java`, the items with `keyName = "skyPreset"` and `keyName = "cycleMinutes"`: change `section = proceduralSection` to `section = skySection`, set their positions to 5 and 6, and change the `skyPreset` description to:

```java
		description = "Sets the sky's clock for both sky types. Presets fix the sun position, CLOCK follows your local time, CYCLE runs a full day in the minutes set below, CUSTOM uses the sun sliders in the Procedural section.",
```

Then renumber the remaining `proceduralSection` items so positions run 1..14 in this order: sunAltitude, sunAzimuth, sunDisk, moonEnabled, moonSize, moonPhase, starsEnabled, starMap, starBrightness, shootingStars, nebula, aurora, cloudCover, cloudSpeed.

- [ ] **Step 8: Build and test**

Run: `./gradlew agentJar test --offline -q`
Expected: BUILD SUCCESSFUL, tests pass, `build/libs/gpu-skybox-agent.jar` timestamp updated.

- [ ] **Step 9: Commit**

```bash
git add -A
git commit -m "SkyClock: one time source for both renderers; Time of day moves to the Sky section"
```

---

### Task 2: Real moon phase

**Files:**
- Modify: `src/main/java/com/gpuskybox/SkyClock.java`
- Modify: `src/test/java/com/gpuskybox/SkyClockTest.java`
- Modify: `src/main/java/com/gpuskybox/ProceduralSkyRenderer.java` (`update`, the non-blood-moon branch)
- Modify: `src/main/java/com/gpuskybox/GpuSkyboxConfig.java` (`moonPhase` description)

**Interfaces:**
- Produces: `static float SkyClock.moonIllumination(java.time.LocalDateTime utc)` (0 = new, 1 = full) and `float SkyClock.moonIllumination(GpuSkyboxConfig config)` (calendar for CLOCK, cycle-driven for CYCLE, -1 otherwise).

- [ ] **Step 1: Write the failing test**

Add to `SkyClockTest.java`:

```java
	@Test
	public void moonIlluminationFollowsTheCalendar()
	{
		// reference new moon: 2000-01-06 18:14 UTC
		assertEquals(0f, SkyClock.moonIllumination(java.time.LocalDateTime.of(2000, 1, 6, 18, 14)), 1e-3f);
		// half a synodic month later it is full
		assertEquals(1f, SkyClock.moonIllumination(java.time.LocalDateTime.of(2000, 1, 21, 4, 36)), 1e-2f);
		// first quarter: roughly half lit
		assertEquals(0.5f, SkyClock.moonIllumination(java.time.LocalDateTime.of(2000, 1, 14, 0, 0)), 0.05f);
	}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --offline -q`
Expected: compilation error, `moonIllumination` not defined.

- [ ] **Step 3: Implement**

Add to `SkyClock.java` (imports `java.time.LocalDateTime`, `java.time.ZoneOffset`, `java.time.temporal.ChronoUnit`):

```java
	private static final double SYNODIC_MONTH_DAYS = 29.530588;
	private static final LocalDateTime REFERENCE_NEW_MOON = LocalDateTime.of(2000, 1, 6, 18, 14);

	/** Fraction of the moon's disk that is lit, 0 = new, 1 = full. */
	static float moonIllumination(LocalDateTime utc)
	{
		double days = ChronoUnit.SECONDS.between(REFERENCE_NEW_MOON, utc) / 86400.0;
		double age = ((days % SYNODIC_MONTH_DAYS) + SYNODIC_MONTH_DAYS) % SYNODIC_MONTH_DAYS;
		return (float) ((1 - Math.cos(age / SYNODIC_MONTH_DAYS * 2 * Math.PI)) / 2);
	}

	/**
	 * Illumination for the current sky time: the real calendar under CLOCK, one lunar month per 29.5 cycles
	 * under CYCLE, or -1 when the preset leaves the moon to the slider.
	 */
	float moonIllumination(GpuSkyboxConfig config)
	{
		switch (config.skyPreset())
		{
			case CLOCK:
				return moonIllumination(LocalDateTime.now(ZoneOffset.UTC));
			case CYCLE:
			{
				double cycles = elapsedSeconds() / (config.cycleMinutes() * 60.0);
				return (float) ((1 - Math.cos(cycles / SYNODIC_MONTH_DAYS * 2 * Math.PI)) / 2);
			}
			default:
				return -1;
		}
	}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --offline -q`
Expected: PASS.

- [ ] **Step 5: Use it in the renderer**

In `ProceduralSkyRenderer.update`, the `else` branch currently reads `moonPhase = config.moonPhase() / 100f;`. Replace with:

```java
			float real = clock.moonIllumination(config);
			moonPhase = real >= 0 ? real : config.moonPhase() / 100f;
```

In `GpuSkyboxConfig.java` change the `moonPhase` description to `"Procedural sky: 0 = new moon, 100 = full moon. Ignored under CLOCK and CYCLE, where the moon follows the calendar."`.

- [ ] **Step 6: Build, test, commit**

Run: `./gradlew agentJar test --offline -q` (expected: success).

```bash
git add -A
git commit -m "Real moon phase under CLOCK and CYCLE"
```

---

### Task 3: Time-of-day cubemaps

**Files:**
- Modify: `tools/hd_to_sky_areas.py` (THEMES/EXTRA output, add per-phase tables)
- Modify: `src/main/java/com/gpuskybox/SkyAreas.java` (Area fields, `skyFor`)
- Create: `src/test/java/com/gpuskybox/SkyAreasTest.java`
- Modify: `src/main/java/com/gpuskybox/GpuSkyboxConfig.java` (Cubemap section: three dropdowns + toggle)
- Modify: `src/main/java/com/gpuskybox/GpuSkyboxPlugin.java` (`selectCubemap`)
- Modify: `README.md` (settings table)

**Interfaces:**
- Consumes: `SkyClock.hour(config)`, `SkyClock.phase(hour)` from Task 1.
- Produces:
  - `SkyAreas.Area` fields `String skyDawn, skyDusk, skyNight` (nullable)
  - `String SkyAreas.Area.skyFor(SkyClock.Phase phase)`: the area's cubemap for that phase, or `sky` when the phase has no override, or null.
  - config `boolean skyboxByTime()`, `SkyboxTexture skyboxDawn()`, `SkyboxTexture skyboxDusk()`, `SkyboxTexture skyboxNight()`.

- [ ] **Step 1: Fetch the dawn sky**

Run: `python tools/polyhaven_to_cubemap.py qwantani_dawn_puresky`
Expected: `wrote .../skybox/qwantani_dawn_puresky` and six 512x512 PNGs in that folder.

- [ ] **Step 2: Write the failing test**

`src/test/java/com/gpuskybox/SkyAreasTest.java`:

```java
package com.gpuskybox;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SkyAreasTest
{
	@Test
	public void phaseOverridesFallBackToTheDaySky()
	{
		SkyAreas.Area a = new SkyAreas.Area();
		a.sky = "day";
		a.skyNight = "night";
		assertEquals("day", a.skyFor(SkyClock.Phase.DAY));
		assertEquals("day", a.skyFor(SkyClock.Phase.DUSK));
		assertEquals("night", a.skyFor(SkyClock.Phase.NIGHT));
	}

	@Test
	public void areaWithoutSkyStaysNull()
	{
		SkyAreas.Area a = new SkyAreas.Area();
		assertNull(a.skyFor(SkyClock.Phase.DAY));
		assertNull(a.skyFor(SkyClock.Phase.NIGHT));
	}
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew test --offline -q`
Expected: compilation error, `skyNight` / `skyFor` missing.

- [ ] **Step 4: Add the fields and helper to SkyAreas.Area**

In `SkyAreas.java`, inside `static class Area`, after `String sky;` add:

```java
		/** Per-phase cubemaps; null means "use sky" (and the global dawn/dusk/night defaults when sky is null too). */
		String skyDawn;
		String skyDusk;
		String skyNight;
```

and after `contains(...)` add:

```java
		String skyFor(SkyClock.Phase phase)
		{
			switch (phase)
			{
				case DAWN:
					return skyDawn != null ? skyDawn : sky;
				case DUSK:
					return skyDusk != null ? skyDusk : sky;
				case NIGHT:
					return skyNight != null ? skyNight : sky;
				default:
					return sky;
			}
		}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew test --offline -q`
Expected: PASS.

- [ ] **Step 6: Config items**

In `GpuSkyboxConfig.java`, add to the `SkyboxTexture` enum, before `DEBUG("debug")`:

```java
		DAWN("qwantani_dawn_puresky"),
```

Then in the Cubemap section, after `skyboxCustomName` (position 2), insert these four items and renumber `skyboxByArea` to 7, `skyboxFadeSeconds` to 8, `skyboxRotation` to 9, `skyboxRotationSpeed` to 10:

```java
	@ConfigItem(
		keyName = "skyboxByTime",
		name = "Sky by time",
		description = "Follow the Time of day: dawn, day, dusk and night cubemaps. Day is the Cubemap above (or the area's sky); the other three are set below unless an area brings its own.",
		position = 3,
		section = cubemapSection
	)
	default boolean skyboxByTime()
	{
		return true;
	}

	@ConfigItem(
		keyName = "skyboxDawn",
		name = "Dawn cubemap",
		description = "Sky by time: shown from 5:00 to 7:00 sky time.",
		position = 4,
		section = cubemapSection
	)
	default SkyboxTexture skyboxDawn()
	{
		return SkyboxTexture.DAWN;
	}

	@ConfigItem(
		keyName = "skyboxDusk",
		name = "Dusk cubemap",
		description = "Sky by time: shown from 17:00 to 20:00 sky time.",
		position = 5,
		section = cubemapSection
	)
	default SkyboxTexture skyboxDusk()
	{
		return SkyboxTexture.SUNSET;
	}

	@ConfigItem(
		keyName = "skyboxNight",
		name = "Night cubemap",
		description = "Sky by time: shown from 20:00 to 5:00 sky time.",
		position = 6,
		section = cubemapSection
	)
	default SkyboxTexture skyboxNight()
	{
		return SkyboxTexture.NIGHT;
	}
```

Also rename the `skyboxFadeSeconds` item's `name` to `"Time fade"` and its description to `"Seconds to crossfade when the sky changes with the time of day or the config. 0 = instant."` (Task 6 adds the distance-based area fade next to it).

- [ ] **Step 7: Pick the cubemap per phase in the plugin**

Replace the whole `selectCubemap()` method in `GpuSkyboxPlugin.java` with:

```java
	/**
	 * Cubemap for this frame: the area's sky for the current phase, else the global phase default, else the
	 * manual Cubemap. A failed load falls through to the next candidate.
	 */
	private void selectCubemap()
	{
		float fade = config.skyboxFadeSeconds();
		SkyClock.Phase phase = config.skyboxByTime() ? SkyClock.phase(skyClock.hour(config)) : SkyClock.Phase.DAY;
		if (currentArea != null)
		{
			String areaSky = currentArea.skyFor(phase);
			if (areaSky != null && skyboxRenderer.select(areaSky, fade))
			{
				return;
			}
		}
		GpuSkyboxConfig.SkyboxTexture texture;
		switch (phase)
		{
			case DAWN:
				texture = config.skyboxDawn();
				break;
			case DUSK:
				texture = config.skyboxDusk();
				break;
			case NIGHT:
				texture = config.skyboxNight();
				break;
			default:
				texture = config.skyboxTexture();
		}
		String name = texture == GpuSkyboxConfig.SkyboxTexture.CUSTOM
			? config.skyboxCustomName().trim()
			: texture.dir;
		skyboxRenderer.select(name, fade);
	}
```

Note: with `currentArea.skyFor(phase)` returning the area's day sky for dusk when the area has no `skyDusk`, an area with its own sky keeps it all day unless the converter gives it phase skies. That is intended: a Morytania overcast sky at "night" is still overcast. The converter step below adds night skies where they matter.

- [ ] **Step 8: Converter emits phase skies**

In `tools/hd_to_sky_areas.py`, after the `EXTRA` list add:

```python
NIGHT_SKY = "qwantani_night_puresky"
AURORA = "ambientcg_nightskyhdri007"
# per-area night overrides: (area name) -> cubemap. Areas not listed keep their day sky at night.
NIGHT_THEMES = {
    "Misthalin": NIGHT_SKY, "Asgarnia": NIGHT_SKY, "Kandarin": NIGHT_SKY, "Al Kharid": NIGHT_SKY,
    "KHARIDIAN_DESERT": NIGHT_SKY, "KHARIDIAN_DESERT_MID": NIGHT_SKY, "KHARIDIAN_DESERT_DEEP": NIGHT_SKY,
    "KARAMJA": NIGHT_SKY, "Feldip Hills": NIGHT_SKY, "KOUREND": NIGHT_SKY, "VARLAMORE": NIGHT_SKY,
    "FREMENNIK_PROVINCE": AURORA, "GIELINOR_SNOWY_NORTHERN_REGION": AURORA, "ZEAH_SNOWY_NORTHERN_REGION": AURORA,
    "FREMENNIK_ISLES_NORTH": AURORA, "FREMENNIK_ISLES_FAR_NORTH": AURORA, "Miscellania": AURORA,
}
DUSK_THEMES = {
    "Misthalin": "qwantani_sunset_puresky", "Asgarnia": "qwantani_sunset_puresky", "Kandarin": "qwantani_dusk_1_puresky",
    "KHARIDIAN_DESERT": "industrial_sunset_puresky", "KHARIDIAN_DESERT_MID": "industrial_sunset_puresky",
    "KHARIDIAN_DESERT_DEEP": "industrial_sunset_puresky", "Al Kharid": "industrial_sunset_puresky",
}
DAWN_THEMES = {
    "Misthalin": "qwantani_dawn_puresky", "Asgarnia": "qwantani_dawn_puresky", "Kandarin": "ambientcg_morningskyhdri013b",
}
```

In `entry(...)`, after `if sky: e["sky"] = sky` add:

```python
    for key, table in (("skyDawn", DAWN_THEMES), ("skyDusk", DUSK_THEMES), ("skyNight", NIGHT_THEMES)):
        if name in table:
            e[key] = table[name]
```

And in `main()`, the `EXTRA` branch builds its dict inline; change it to call `entry` so the phase tables apply there too. Replace:

```python
        e = entry(areas, name, sky, None) if boxes is None else {"name": name, "sky": sky, "aabbs": [norm_aabb(b) for b in boxes]}
```

with:

```python
        e = entry(areas, name, sky, None, boxes)
```

and change the `entry` signature and its first lines to:

```python
def entry(areas, name, sky, fog, boxes=None):
    if boxes is None:
        aabbs, regions, rboxes = flatten(areas, name)
    else:
        aabbs, regions, rboxes = [norm_aabb(b) for b in boxes], [], []
    if not (aabbs or regions or rboxes):
        return None
    e = {"name": name}
```

(rename the later `boxes` uses inside `entry` to `rboxes`).

Run: `python tools/hd_to_sky_areas.py`
Expected: `wrote 162 areas, 75 with a sky`; `grep -c skyNight src/main/resources/com/gpuskybox/sky_areas.json` prints a number of at least 15.

- [ ] **Step 9: README**

In the settings table of `README.md`, after the `| Cubemap | ...` row add:

```
| Sky by time | on | cubemap: dawn/day/dusk/night follow the Time of day; areas may bring their own night or dusk sky |
| Dawn / Dusk / Night cubemap | DAWN / SUNSET / NIGHT | cubemap: phase defaults for areas without their own |
```

and change the `| Time of day | ...` row's first cell text to `Time of day (Sky section)`.

- [ ] **Step 10: Build, test, commit**

Run: `./gradlew agentJar test --offline -q` (expected: success).

```bash
git add -A
git commit -m "Time-of-day cubemaps: dawn/day/dusk/night per area with global defaults"
```

---

### Task 4: Lightning

**Files:**
- Create: `src/main/java/com/gpuskybox/Lightning.java`
- Create: `src/test/java/com/gpuskybox/LightningTest.java`
- Modify: `tools/hd_to_sky_areas.py` (carry `lightningEffects`)
- Modify: `src/main/java/com/gpuskybox/SkyAreas.java` (`boolean lightning`)
- Modify: `src/main/resources/com/gpuskybox/sky_frag.glsl`, `src/main/resources/com/gpuskybox/proc_sky_frag.glsl` (`uniform float flash`)
- Modify: `src/main/java/com/gpuskybox/SkyboxRenderer.java`, `src/main/java/com/gpuskybox/ProceduralSkyRenderer.java` (`draw` gets a `float flash` parameter)
- Modify: `src/main/java/com/gpuskybox/GpuSkyboxPlugin.java` (owns `Lightning`, passes flash, brightens fog)
- Modify: `src/main/java/com/gpuskybox/GpuSkyboxConfig.java` (Sky section toggle)

**Interfaces:**
- Produces:
  - `Lightning(java.util.Random random)`; `float Lightning.intensity(double nowSeconds, boolean active)` returns 0..1. When `active` is false it returns 0 and resets so the next storm starts fresh.
  - `SkyAreas.Area.lightning` boolean.
  - `void SkyboxRenderer.draw(float[] skyProj, int sky, int quadVao, GpuSkyboxConfig config, float flash)`
  - `void ProceduralSkyRenderer.draw(float[] skyProj, int fog, int quadVao, GpuSkyboxConfig config, SkyClock clock, float flash)`
  - config `boolean lightningEnabled()`.

- [ ] **Step 1: Write the failing test**

`src/test/java/com/gpuskybox/LightningTest.java`:

```java
package com.gpuskybox;

import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LightningTest
{
	@Test
	public void inactiveIsAlwaysDark()
	{
		Lightning l = new Lightning(new Random(1));
		for (double t = 0; t < 60; t += 0.1)
		{
			assertEquals(0f, l.intensity(t, false), 0f);
		}
	}

	@Test
	public void strikesHappenAndFadeWithinAFewFrames()
	{
		Lightning l = new Lightning(new Random(7));
		int litFrames = 0;
		int frames = 0;
		float peak = 0;
		for (double t = 0; t < 120; t += 1 / 50.0)
		{
			float i = l.intensity(t, true);
			assertTrue(i >= 0f && i <= 1f);
			frames++;
			if (i > 0.05f)
			{
				litFrames++;
			}
			peak = Math.max(peak, i);
		}
		assertTrue("at least one strike in two minutes", peak > 0.8f);
		assertTrue("lit less than 5% of the time", litFrames < frames / 20);
	}

	@Test
	public void deactivatingResetsTheSchedule()
	{
		Lightning l = new Lightning(new Random(3));
		l.intensity(0, true);
		l.intensity(100, false);
		// after a reset the very next active frame must not be mid-flash
		assertEquals(0f, l.intensity(100.01, true), 0f);
	}
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --offline -q`
Expected: compilation error, `Lightning` missing.

- [ ] **Step 3: Implement Lightning**

`src/main/java/com/gpuskybox/Lightning.java`:

```java
package com.gpuskybox;

import java.util.Random;

/**
 * Schedules lightning strikes and shapes each one as a short burst of 1 to 3 flickers. Pure: feed it the
 * current time in seconds and whether the player stands in a stormy area.
 */
class Lightning
{
	private static final double MIN_GAP = 6;
	private static final double MAX_GAP = 22;
	private static final double FLICKER_LENGTH = 0.09;
	private static final double FLICKER_GAP = 0.07;

	private final Random random;
	private double nextStrike = -1;
	private int flickers;
	private float strength;

	Lightning(Random random)
	{
		this.random = random;
	}

	/** 0 = no flash, 1 = full white-out. */
	float intensity(double now, boolean active)
	{
		if (!active)
		{
			nextStrike = -1;
			return 0f;
		}
		if (nextStrike < 0)
		{
			schedule(now);
			return 0f;
		}
		double sinceStrike = now - nextStrike;
		if (sinceStrike < 0)
		{
			return 0f;
		}
		double burstLength = flickers * (FLICKER_LENGTH + FLICKER_GAP);
		if (sinceStrike >= burstLength)
		{
			schedule(now);
			return 0f;
		}
		double inFlicker = sinceStrike % (FLICKER_LENGTH + FLICKER_GAP);
		if (inFlicker >= FLICKER_LENGTH)
		{
			return 0f;
		}
		// sharp attack, exponential-ish decay inside each flicker
		float t = (float) (inFlicker / FLICKER_LENGTH);
		return strength * (1f - t * t);
	}

	private void schedule(double now)
	{
		nextStrike = now + MIN_GAP + random.nextDouble() * (MAX_GAP - MIN_GAP);
		flickers = 1 + random.nextInt(3);
		strength = 0.7f + random.nextFloat() * 0.3f;
	}
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --offline -q`
Expected: PASS. If `strikesHappenAndFadeWithinAFewFrames` fails on "at least one strike", the seed produced gaps that sum past 120 s; MIN/MAX_GAP guarantee at least 5 strikes in 120 s, so a failure here means the burst loop is wrong, not the seed.

- [ ] **Step 5: Converter and Area flag**

In `tools/hd_to_sky_areas.py`, in `main()` inside the environments loop, pass the flag through: change `e = entry(areas, name, sky, fog)` to:

```python
        e = entry(areas, name, sky, fog)
        if e and env.get("lightningEffects"):
            e["lightning"] = True
```

Run `python tools/hd_to_sky_areas.py`; `grep -c '"lightning":true' src/main/resources/com/gpuskybox/sky_areas.json` must print 6 or more.

In `SkyAreas.Area` add after `String fog;`:

```java
		/** 117 HD lightningEffects: random flashes while the player is here. */
		boolean lightning;
```

- [ ] **Step 6: Shader uniform**

In both `sky_frag.glsl` and `proc_sky_frag.glsl` add after the `brightness` uniform:

```glsl
uniform float flash;         // lightning, 0..1: lifts the whole sky towards white
```

In `sky_frag.glsl` change the final line to:

```glsl
  vec3 outSky = mix(fogColor.rgb, sky, t);
  FragColor = vec4(mix(outSky, vec3(1.0), flash * 0.7), 1.0);
```

In `proc_sky_frag.glsl` change the last two lines of `main` (`sky += (hash2(...` and `FragColor = ...`) to:

```glsl
  sky = mix(sky, vec3(1.0), flash * 0.7);
  sky += (hash2(gl_FragCoord.xy) - 0.5) / 255.0;
  FragColor = vec4(clamp(sky, 0.0, 1.0), 1.0);
```

- [ ] **Step 7: Renderers take the flash**

`SkyboxRenderer.java`: add `private int uniFlash;`, in `initProgram` add `uniFlash = glGetUniformLocation(program, "flash");`, change `draw` signature to `void draw(float[] skyProj, int sky, int quadVao, GpuSkyboxConfig config, float flash)` and add `glUniform1f(uniFlash, flash);` after the brightness uniform.

`ProceduralSkyRenderer.java`: same three edits, signature `void draw(float[] skyProj, int fog, int quadVao, GpuSkyboxConfig config, SkyClock clock, float flash)`.

- [ ] **Step 8: Plugin: schedule, pass, brighten fog**

In `GpuSkyboxPlugin.java`:

1. Fields, next to `skyClock`: `private final Lightning lightning = new Lightning(new java.util.Random());` and `private float flash;`.
2. In `updateArea()`, after the area is resolved (end of the method), add:

```java
		boolean stormy = config.lightningEnabled() && currentArea != null && currentArea.lightning;
		flash = lightning.intensity(skyClock.elapsedSeconds(), stormy);
```

3. In `fogColor(boolean cubemap)`, wrap the existing switch: rename the method body into `private int baseFogColor(boolean cubemap)` and add:

```java
	private int fogColor(boolean cubemap)
	{
		int base = baseFogColor(cubemap);
		if (flash <= 0f)
		{
			return base;
		}
		int r = (base >> 16 & 0xFF) + Math.round((255 - (base >> 16 & 0xFF)) * flash * 0.6f);
		int g = (base >> 8 & 0xFF) + Math.round((255 - (base >> 8 & 0xFF)) * flash * 0.6f);
		int b = (base & 0xFF) + Math.round((255 - (base & 0xFF)) * flash * 0.6f);
		return r << 16 | g << 8 | b;
	}
```

4. In `drawSkybox`, pass `flash` as the new last argument to both `proceduralSky.draw(...)` and `skyboxRenderer.draw(...)`.

- [ ] **Step 9: Config toggle**

In the Sky section of `GpuSkyboxConfig.java`, add at position 7:

```java
	@ConfigItem(
		keyName = "lightningEnabled",
		name = "Lightning",
		description = "Random lightning flashes where 117 HD marks storms: Wilderness (high), Barrows, Darkmeyer, Draynor Manor, Tempoross.",
		position = 7,
		section = skySection
	)
	default boolean lightningEnabled()
	{
		return true;
	}
```

- [ ] **Step 10: Build, test, commit**

Run: `./gradlew agentJar test --offline -q` (expected: success).

```bash
git add -A
git commit -m "Lightning flashes in 117 HD storm areas, sky and fog"
```

---

### Task 5: Area moods for the procedural sky

**Files:**
- Modify: `tools/hd_to_sky_areas.py` (`PRESETS` table)
- Modify: `src/main/java/com/gpuskybox/SkyAreas.java` (`String preset`, parsed enum)
- Modify: `src/test/java/com/gpuskybox/SkyAreasTest.java`
- Modify: `src/main/java/com/gpuskybox/ProceduralSkyRenderer.java` (`update` takes an override)
- Modify: `src/main/java/com/gpuskybox/GpuSkyboxPlugin.java` (`shouldDrawCubemap`)
- Modify: `src/main/java/com/gpuskybox/GpuSkyboxConfig.java` (Procedural section toggle)

**Interfaces:**
- Produces:
  - `SkyAreas.Area.preset` (String from JSON) and `transient GpuSkyboxConfig.SkyPreset presetValue` (null when absent or unknown), filled in `SkyAreas.load`.
  - `void ProceduralSkyRenderer.update(GpuSkyboxConfig config, SkyClock clock, GpuSkyboxConfig.SkyPreset override)`; `override == null` means "use config".
  - config `boolean areaMoods()`.

- [ ] **Step 1: Write the failing test**

Add to `SkyAreasTest.java`:

```java
	@Test
	public void presetStringsResolveToEnumOrNull()
	{
		assertEquals(GpuSkyboxConfig.SkyPreset.BLOOD_MOON, SkyAreas.parsePreset("BLOOD_MOON"));
		assertNull(SkyAreas.parsePreset(null));
		assertNull(SkyAreas.parsePreset("NOT_A_PRESET"));
	}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --offline -q`
Expected: compilation error, `parsePreset` missing.

- [ ] **Step 3: Implement in SkyAreas**

In `SkyAreas.Area` add after `boolean lightning;`:

```java
		/** Procedural preset forced in this area (a SkyPreset name), or null. */
		String preset;
		transient GpuSkyboxConfig.SkyPreset presetValue;
```

Add to `SkyAreas` (class level):

```java
	static GpuSkyboxConfig.SkyPreset parsePreset(String name)
	{
		if (name == null)
		{
			return null;
		}
		try
		{
			return GpuSkyboxConfig.SkyPreset.valueOf(name);
		}
		catch (IllegalArgumentException ex)
		{
			log.warn("sky_areas.json: unknown preset '{}'", name);
			return null;
		}
	}
```

In `load(...)`, the loop `for (Area a : areas) { a.fogColor = ...; }` gains a second line: `a.presetValue = parsePreset(a.preset);`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --offline -q`
Expected: PASS.

- [ ] **Step 5: Renderer override**

In `ProceduralSkyRenderer.update`, change the signature to `void update(GpuSkyboxConfig config, SkyClock clock, SkyPreset override)` and the first line to:

```java
		SkyPreset preset = override != null ? override : config.skyPreset();
```

The `hour = clock.hour(config);` line must respect the override for the star map and phase, so replace it with:

```java
		hour = override != null ? SkyClock.hourForAzimuth(override.azimuth) : clock.hour(config);
```

and in the `switch`, the `CLOCK`/`CYCLE` cases stay (an override is never CLOCK or CYCLE because the converter only emits fixed presets).

- [ ] **Step 6: Plugin and config**

In `GpuSkyboxPlugin.shouldDrawCubemap`, change `proceduralSky.update(config, skyClock);` to:

```java
			GpuSkyboxConfig.SkyPreset mood = config.areaMoods() && currentArea != null ? currentArea.presetValue : null;
			proceduralSky.update(config, skyClock, mood);
```

(`updateArea()` already runs before this line, so `currentArea` is fresh.)

In the Procedural section of `GpuSkyboxConfig.java`, add at position 15 (after cloudSpeed):

```java
	@ConfigItem(
		keyName = "areaMoods",
		name = "Area moods",
		description = "Let areas override the Time of day: Morytania stays at dusk, Darkmeyer under a blood moon, the desert at high noon.",
		position = 15,
		section = proceduralSection
	)
	default boolean areaMoods()
	{
		return true;
	}
```

- [ ] **Step 7: Converter table**

In `tools/hd_to_sky_areas.py`, after `DAWN_THEMES` add:

```python
# procedural preset forced per area (GpuSkyboxConfig.SkyPreset names)
PRESETS = {
    "MORYTANIA": "DUSK", "VER_SINHAZA": "DUSK", "MEIYERDITCH": "DUSK", "BARROWS": "DUSK",
    "DARKMEYER": "BLOOD_MOON", "VAMPYRIUM": "BLOOD_MOON", "VAMPYRIUM_FOREST_INSTANCE": "BLOOD_MOON",
    "KHARIDIAN_DESERT": "DAY", "KHARIDIAN_DESERT_MID": "DAY", "KHARIDIAN_DESERT_DEEP": "DAY",
    "Lunar Isle": "NIGHT", "ZANARIS": "NIGHT", "ARCEUUS": "DUSK",
    "WILDERNESS_HIGH": "DUSK", "WILDERNESS_MID": "DUSK",
}
```

and in `entry(...)`, after the phase-sky loop: `if name in PRESETS: e["preset"] = PRESETS[name]`.

Run `python tools/hd_to_sky_areas.py`; `grep -c '"preset"' src/main/resources/com/gpuskybox/sky_areas.json` prints 10 or more.

- [ ] **Step 8: Build, test, commit**

Run: `./gradlew agentJar test --offline -q` (expected: success).

```bash
git add -A
git commit -m "Area moods: areas force a procedural preset"
```

---

### Task 6: Border blending by distance

**Files:**
- Create: `src/main/java/com/gpuskybox/BorderBlend.java`
- Create: `src/test/java/com/gpuskybox/BorderBlendTest.java`
- Modify: `src/main/java/com/gpuskybox/SkyboxRenderer.java` (`overrideBlend`, `blend()`)
- Modify: `src/main/java/com/gpuskybox/GpuSkyboxPlugin.java` (`updateArea`, `selectCubemap`, `shouldDrawCubemap`)
- Modify: `src/main/java/com/gpuskybox/GpuSkyboxConfig.java` (Cubemap section: `skyboxFadeTiles`)
- Modify: `README.md`, `HANDOFF.md`

**Interfaces:**
- Produces:
  - `BorderBlend`: `void cross(int x, int y)` records a crossing at the player's tile; `void bounce()` swaps direction when the player steps back over the same border; `float progress(int x, int y, int radiusTiles)` 0..1; `boolean active()`.
  - `void SkyboxRenderer.overrideBlend(float t)`: while `t` is in [0, 1] the renderer uses it instead of the time-based fade; pass -1 to release.
  - config `int skyboxFadeTiles()` (default 8, 0 = instant).

- [ ] **Step 1: Write the failing test**

`src/test/java/com/gpuskybox/BorderBlendTest.java`:

```java
package com.gpuskybox;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BorderBlendTest
{
	@Test
	public void progressGrowsWithDistanceFromTheCrossing()
	{
		BorderBlend b = new BorderBlend();
		b.cross(100, 100);
		assertTrue(b.active());
		assertEquals(0f, b.progress(100, 100, 8), 1e-6f);
		assertEquals(0.5f, b.progress(104, 100, 8), 1e-6f);
		assertEquals(1f, b.progress(100, 108, 8), 1e-6f);
		assertEquals(1f, b.progress(120, 120, 8), 1e-6f);
		assertFalse("finished blends release", b.active());
	}

	@Test
	public void bouncingBackReversesInsteadOfRestarting()
	{
		BorderBlend b = new BorderBlend();
		b.cross(100, 100);
		assertEquals(0.25f, b.progress(102, 100, 8), 1e-6f);
		b.bounce();
		// now 2 tiles from the crossing means 2 tiles back INTO the old area: 1 - 0.25
		assertEquals(0.75f, b.progress(102, 100, 8), 1e-6f);
		assertEquals(1f, b.progress(100, 100, 8), 1e-6f);
	}

	@Test
	public void zeroRadiusIsInstant()
	{
		BorderBlend b = new BorderBlend();
		b.cross(0, 0);
		assertEquals(1f, b.progress(0, 0, 0), 1e-6f);
	}
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --offline -q`
Expected: compilation error, `BorderBlend` missing.

- [ ] **Step 3: Implement BorderBlend**

`src/main/java/com/gpuskybox/BorderBlend.java`:

```java
package com.gpuskybox;

/**
 * Blend progress for an area change, driven by how far the player has walked from the tile where the
 * border was crossed. Stepping back across the same border reverses the blend instead of restarting it.
 */
class BorderBlend
{
	private boolean active;
	private boolean reversed;
	private int crossX;
	private int crossY;

	void cross(int x, int y)
	{
		active = true;
		reversed = false;
		crossX = x;
		crossY = y;
	}

	/** The player stepped back over the border just crossed: keep the crossing tile, flip direction. */
	void bounce()
	{
		reversed = !reversed;
	}

	boolean active()
	{
		return active;
	}

	/** 0 = still the old sky, 1 = fully the new one. Releases itself once it reaches 1. */
	float progress(int x, int y, int radiusTiles)
	{
		if (!active)
		{
			return 1f;
		}
		if (radiusTiles <= 0)
		{
			active = false;
			return 1f;
		}
		double dist = Math.hypot(x - crossX, y - crossY);
		float t = (float) Math.min(1.0, dist / radiusTiles);
		if (reversed)
		{
			t = 1f - t;
		}
		if (t >= 1f)
		{
			active = false;
			return 1f;
		}
		return t;
	}
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --offline -q`
Expected: PASS.

- [ ] **Step 5: Renderer override**

In `SkyboxRenderer.java` add a field `private float blendOverride = -1f;` and:

```java
	/** Drive the crossfade from outside (border blending); pass -1 to go back to the time-based fade. */
	void overrideBlend(float t)
	{
		blendOverride = t;
	}
```

In `blend()`, insert at the top:

```java
		if (blendOverride >= 0f && previous != null)
		{
			if (blendOverride >= 1f)
			{
				previous = null;
				blendOverride = -1f;
				return 1f;
			}
			return blendOverride;
		}
```

Also, in `select(...)`, the line `this.fadeSeconds = previous == null ? 0 : fadeSeconds;` stays, and add `blendOverride = -1f;` right after it, so a new switch starts clean; the plugin re-applies the override on the next frame when the switch was an area change.

- [ ] **Step 6: Plugin: crossing tracking**

In `GpuSkyboxPlugin.java`:

1. Fields: `private final BorderBlend borderBlend = new BorderBlend();`, `private SkyAreas.Area previousArea;`, `private String lastAreaSky;`.
2. Replace `updateArea()` with:

```java
	private void updateArea()
	{
		WorldPoint world = config.skyboxByArea() ? playerWorldPoint() : null;
		SkyAreas.Area area = world == null ? null : skyAreas.find(world);
		if (area != currentArea)
		{
			if (area == previousArea && borderBlend.active())
			{
				borderBlend.bounce();
			}
			else if (world != null)
			{
				borderBlend.cross(world.getX(), world.getY());
			}
			previousArea = currentArea;
			currentArea = area;
			log.info("Sky area: {}", area == null ? "unmapped" : area.name + " sky=" + area.sky + " fog=" + area.fog);
		}
		boolean stormy = config.lightningEnabled() && currentArea != null && currentArea.lightning;
		flash = lightning.intensity(skyClock.elapsedSeconds(), stormy);
		lastWorld = world;
	}
```

and add the field `private WorldPoint lastWorld;`.

3. In `shouldDrawCubemap`, after `selectCubemap();` add:

```java
				if (borderBlend.active() && lastWorld != null)
				{
					skyboxRenderer.overrideBlend(borderBlend.progress(lastWorld.getX(), lastWorld.getY(), config.skyboxFadeTiles()));
				}
```

Why this is enough: `select` only swaps textures when the name changes, so a time-of-day switch inside one area never has `borderBlend.active()` and keeps the seconds fade; an area switch sets the crossing tile in `updateArea` (same frame, before `selectCubemap`), `select` starts a fade, and the override takes it over from the next line on.

- [ ] **Step 7: Config**

In the Cubemap section of `GpuSkyboxConfig.java`, add after `skyboxFadeSeconds` (position 8; renumber rotation to 10 and drift to 11):

```java
	@Range(min = 0, max = 64)
	@ConfigItem(
		keyName = "skyboxFadeTiles",
		name = "Area fade",
		description = "Tiles to walk past an area border before the new sky is fully in. Walking back reverses it. 0 = instant.",
		position = 9,
		section = cubemapSection
	)
	default int skyboxFadeTiles()
	{
		return 8;
	}
```

- [ ] **Step 8: Docs**

`README.md` settings table: change the `| Area fade | 4 | ...` row to `| Area fade | 8 | cubemap: tiles walked past a border until the new sky is fully in; walking back reverses |` and add `| Time fade | 4 | seconds for time-of-day and config changes |`. Also add rows for `Lightning` (Sky, on), `Area moods` (Procedural, on) and note under Moon phase that CLOCK/CYCLE use the calendar.

`HANDOFF.md`: replace the section 6 "AFTER" line with a "DONE" line listing the five features and the commit hash of this task, and set NEXT to the in-game checklist: (a) CYCLE with Cycle length 1 in Lumbridge: dawn, day, sunset, night cubemaps in one minute; (b) walk into the Wilderness at level 30+: flashes within 22 s; (c) PROCEDURAL in Canifis: dusk regardless of Time of day; (d) walk back and forth across the Al Kharid gate: no pop.

- [ ] **Step 9: Build, test, commit**

Run: `./gradlew agentJar test --offline -q` (expected: success; `build/libs/gpu-skybox-agent.jar` fresh).

```bash
git add -A
git commit -m "Border blending by distance walked; Area fade in tiles"
```

---

## Self-review notes

- Spec coverage: feature 1 = Task 3 (+ Task 1 for the shared clock), feature 2 = Task 4, feature 3 = Task 5, feature 4 = Task 2, feature 5 = Task 6. Task 0 is infrastructure.
- Signatures used across tasks: `update(config, clock)` (Task 1) becomes `update(config, clock, override)` in Task 5; `draw(..., config, clock)` (Task 1) becomes `draw(..., config, clock, flash)` in Task 4; `SkyboxRenderer.draw(..., config)` becomes `draw(..., config, flash)` in Task 4. Executors of Task 5 and 6 will find the Task 4 signatures in place.
- Nothing here has been seen in-game; each task's "expected" lines are build and unit-test results. The in-game checklist lives in HANDOFF.md after Task 6.
