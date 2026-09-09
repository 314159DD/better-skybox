# Sky sources, AI panorama generators, OSRS region datasets (research 2026-09-09)

Target: cubemap sky, 6 faces 512x512 PNG, converted from a 2:1 equirectangular tonemapped JPG/PNG.
Hobby sideload plugin, prefer CC0 / CC-BY / paid-with-commercial-rights.
Items marked (unverified) were seen only in search snippets; the page itself blocked or was not opened.

## 1. Free sky panorama libraries beyond Poly Haven

| Name | URL | License | Format / resolution | Cost | Bulk / API | Verdict |
|---|---|---|---|---|---|---|
| ambientCG HDRI, Sky category | https://ambientcg.com/list?type=HDRI&category=Sky | CC0 1.0 (docs.ambientcg.com/license) | JPG + EXR, Day/Evening/Morning/Night Sky categories | Free | JSON API `https://ambientcg.com/api/v2/full_json` (confirmed, ~2887 assets with download URLs) | Best Poly Haven alternative: CC0, scriptable, sky-tagged |
| OpenGameArt "Cloudy Skyboxes" | https://opengameart.org/content/cloudy-skyboxes-0 | CC0 (on page) | PNG, equirect 2048x1024 AND cubemap cross 2048x1536 (512 px faces) | Free | ZIP per asset, no API | Direct fit, already ships 512 px faces |
| OpenGameArt "Planet Surface Skyboxes" | https://opengameart.org/content/planet-surface-skyboxes | CC0 (same publisher family, listing says free for anything) | PNG, cubemap + equirect, 64 variants | Free | ZIP | Sky-dome only, alien tints usable for underwater / dungeon |
| OpenGameArt "Space Skyboxes" | https://opengameart.org/content/space-skyboxes-0 | "Free for anything" (unverified badge) | Spacescape output, cubemap/panorama | Free | ZIP | Night / space skies |
| Kenney Skyboxes | https://kenney.nl/assets/skyboxes | CC0 | 5 stylized sets, resolution not stated on page | Free | Included in "All-in-1" bundle | Small but stylized, matches OSRS look |
| NASA SVS Deep Star Maps 2020 | https://svs.gsfc.nasa.gov/4851 | Public domain (NASA) | EXR equirect 4K / 8K / 16K / 64K | Free | Direct per-resolution links, no API | Best night-sky base layer |
| ESO Milky Way panoramas | https://www.eso.org/public/images/eso0932a/ , https://www.eso.org/public/images/eso1908e/ | CC BY 4.0 | JPG, multi-K panorama | Free | Manual | Good night sky, attribution only |
| ESA Gaia all-sky equirect | https://sci.esa.int/web/gaia/-/60196-gaia-s-sky-in-colour-equirectangular-projection | CC BY-SA 3.0 IGO | JPG 8192x4096, tagged for 360 viewers | Free | Manual | Purpose-built star map; ShareAlike applies to derivatives |
| HDRMAPS freebies | https://hdrmaps.com/freebies/ | CC BY 4.0 (unverified, page 403) | EXR up to 10K | Free | No API | Attribution OK, confirm license on page |
| HDRI Skies free | https://hdri-skies.com/free-hdris/ | Snippets say free 2K commercial (unverified, page 403) | 2K free, up to 20K paid | Free 2K / paid | No | Supplementary, confirm terms |
| CGEES | https://cgees.com/ | CC0 claimed (unverified, not fetched) | HDRI 1K to 24K | Free | Unknown | Check directly |
| openhdri.org Sky | https://openhdri.org/ | CC0 claimed (unverified, page 403) | Unknown | Free | Unknown | Check directly |
| Location Textures panoramas | https://locationtextures.com/panoramas/ | Free tier non-commercial, $15 per pano for commercial | EXR 4096x2048 free | Free (NC) / paid | Manual | OK for hobby use only; includes ground |
| NoEmotion HDRs | https://noemotionhdrs.net/ | CC BY-ND 4.0 (on page) | TIFF ~15000x7500 | Free | Manual, bandwidth-capped | Poor fit: ND forbids the cubemap conversion, ground included |
| Wikimedia Commons 360 equirect category | https://commons.wikimedia.org/wiki/Category:360%C2%B0_panoramas_with_equirectangular_projection | Per file (CC0 / CC-BY / CC-BY-SA) | Mixed | Free | Commons API (generic) | Hand-pick only, sky-only files rare |
| Sketchfab (e.g. Milky Way skybox) | https://sketchfab.com/3d-models/milky-way-skybox-hdri-panorama-b57711d6a450410ca612c4a36f08ce21 | Per uploader (unverified) | Varies | Free / paid mix | No | One-off finds, check license tab each time |
| itch.io "Free AI-Generated Space Skyboxes" | https://crediblesteve.itch.io/space-skyboxes | Not stated in snippet (unverified) | Unknown | Pay what you want | Manual | Verify license on listing first |
| Spacescape (generator, not library) | https://github.com/FrozenStormInteractive/Spacescape | MIT | Renders cubemap faces directly | Free | N/A | Procedural night/space skies, zero license risk |
| Unity Asset Store free skyboxes | https://assetstore.unity.com/packages/2d/textures-materials/sky/free-hdr-skyboxes-pack-175525 | Unity Asset Store EULA: no redistribution outside a compiled product | Varies | Free | No | Not usable: plugin ships raw PNGs |
| Fab (Unreal) free sky packs | https://www.fab.com/category/material/nature-terrain--sky | Fab EULA: same restriction | Varies | Free / paid | No | Not usable, same reason |
| sIBL Archive (hdrlabs) | http://www.hdrlabs.com/sibl/archive.html | Historically CC BY-NC-SA | Old HDR sets | Free | No | Dead (404), skip |

## 2. AI text-to-360 panorama generators

| Name | URL | API | Cost / image | Max res | Style control | Output license | Sky-only mode | Verdict |
|---|---|---|---|---|---|---|---|---|
| Blockade Labs Skybox AI | https://skybox.blockadelabs.com/ | Yes, https://api-documentation.blockadelabs.com/ (full API on Business ~$112/mo, limited on Essential ~$20/mo; Standard tier API status unverified) | ~$0.20 to 0.40 effective (Essential: 100 credits/mo) | 8K equirect; 16K on Business (per-face vs total unverified) | Fantasy / anime / sci-fi / painterly presets + free text | Paid tiers: you own output, full commercial, no distribution restriction. Free tier: preview / watermark, conflicting reports on export | Yes, "skydome, no ground objects" preset | Recommended, purpose-built |
| SkyboxKit | https://skyboxkit.com/ | None documented | ~$0.09 to 0.14 per credit, credits/image unclear | 3840x1920 PNG, also exports six 1024x1024 cubemap faces | Prompt + reference image | You retain ownership, commercial allowed | No | Worth a manual trial, no batch scripting |
| igorriti/flux-360 (Replicate) | https://replicate.com/igorriti/flux-360 | Replicate API | ~$0.014 to 0.018 per run | Undocumented (Flux-dev, ~1024 to 2048) | Flux prompt, decent stylized | FLUX.1-dev non-commercial | No | Cheap, but license blocks redistribution |
| jbilcke-hf/flux-dev-panorama-lora-2 | https://huggingface.co/jbilcke-hf/flux-dev-panorama-lora-2 (also https://replicate.com/jbilcke/flux-dev-panorama-lora) | Replicate API | Replicate per-run | Trained 2048x1024 | Weak on fantasy (Street View training data) | FLUX.1-dev non-commercial, author says personal use only | No | License blocker, weak fantasy |
| Akbartus/Flux360-LORA | https://huggingface.co/Akbartus/Flux360-LORA | No, local only | GPU cost | Undocumented | Trigger "equirectangular 360 degree panorama" | FLUX.1-dev non-commercial | No | License blocker |
| 360 Diffusion LoRA for Flux (Civitai) | https://civitai.com/models/1221997/360-diffusion-lora-for-flux | No, local only | GPU cost | 2:1, e.g. 1024x512 | Author reports near-zero seam, rare pole artifacts | FLUX.1-dev non-commercial | No | Good quality, license blocker |
| ProGamerGov/sdxl-360-diffusion | https://huggingface.co/ProGamerGov/sdxl-360-diffusion | No, local SDXL / ComfyUI | GPU cost | 1024x512 to 2048x1024, then upscale | Diverse dataset, handles fantasy prompts | MIT | No native; ComfyUI circular padding + mask | Best free self-hosted option license-wise |
| ModelsLab 360 Panorama (SD1.5/Flux) | https://modelslab.com/models/modelslab/360-panorama-sd1-5-flux-flux | Yes | ~$0.0047 per call, $21/mo minimum | Not stated | Standard prompt | Not stated (unverified) | No | Cheapest API, "mostly seamless most of the time" per their own page |
| HunyuanWorld 1.0 (Tencent) | https://github.com/Tencent-Hunyuan/HunyuanWorld-1.0 | No, self-hosted, heavy | GPU cost | Panorama is an intermediate step to a 3D mesh | Text or image | Repo license not opened (unverified) | No, full scene | Overkill |
| CubeDiff (Google, ICLR 2025) | https://arxiv.org/abs/2501.17162 | No public code or API found | N/A | Generates 6 cubemap faces directly (no pole smear by design) | Per-face text control | N/A | N/A | Ideal on paper, not usable today |
| PanFusion / DiffPano / MVDiffusion | https://chengzhag.github.io/publication/panfusion , https://github.com/zju3dv/DiffPano , https://github.com/Tangshitao/MVDiffusion | No, research code | GPU cost | 512 to 1024 per view, stitched | Research grade | Mostly academic / NC (unverified per repo) | No | ML engineering effort, not turnkey |
| Leonardo.ai | https://leonardo.ai/ | Yes (generic) | Credit based | Not panorama-native | General model, prompt "equirectangular" | Paid plans commercial (unverified) | No | Seams unreliable, not recommended |

**Recommendation: Blockade Labs Skybox AI, Essential tier, generate the 10 skies in one month, cancel.**
Only tool with a built-in no-ground skydome preset, stylized presets, cubemap export and an explicit commercial license.
Every Flux LoRA is FLUX.1-dev non-commercial; sdxl-360-diffusion (MIT) is the fallback if you want zero cost and own a GPU.

Example prompt, wilderness storm (style preset Fantasy, skydome / no ground enabled, 8K, Model 3):

```
apocalyptic wilderness storm sky, dark ash-grey storm clouds, jagged lightning strikes,
blood-orange lightning glow on cloud undersides, swirling volcanic ash, ominous green-tinted
overcast light, low-hanging thunderheads, painterly fantasy RPG skybox, seamless 360 sky dome,
no ground, no horizon structures

Negative: ground, terrain, buildings, characters, text, watermark, trees, mountains, blurry seams
```

## 3. OSRS region-ID to named-area datasets

| Name | URL | Format | Coverage | License | Verdict |
|---|---|---|---|---|---|
| 117HD `areas.json` | https://github.com/117HD/RLHD/blob/master/src/main/resources/rs117/hd/scene/areas.json | JSON array, 1206 objects: `name` + `aabbs` (world-tile boxes, 866), `regions` (16-bit region IDs, 232), `regionBoxes` (ID ranges, 79), `areas` (nested refs, 80) | MORYTANIA, WILDERNESS (= HIGH/MID_HIGH/MID/LOW), KHARIDIAN_DESERT, TIRANNWN (= MAINLAND + PRIFDDINAS_REGION), KOUREND, VARLAMORE, KARAMJA, FREMENNIK_PROVINCE all present. No top-level ASGARNIA / MISTHALIN / KANDARIN | BSD-2-Clause (verified) | Primary source. Big areas use `aabbs`, so convert tile box to region IDs: `rx = x >> 6`, `ry = y >> 6`, `id = (rx << 8) \| ry` |
| 117HD `environments.json` | https://github.com/117HD/RLHD/blob/master/src/main/resources/rs117/hd/scene/environments.json | JSON array, 210 objects: `area`, `fogColor`, `fogDepth`, `ambientColor`, `directionalColor`, `sunAngles`, ground fog, wind | Priority-ordered area to lighting/fog table, e.g. `{"area":"MORYTANIA","fogColor":"#1e314b",...}` | BSD-2-Clause | Exact analogue of "sky theme per area"; copy the structure, swap colors for a sky preset ID |
| 117HD `Area.java` | https://github.com/117HD/RLHD/blob/master/src/main/java/rs117/hd/scene/areas/Area.java | Java, generated from areas.json; consumed by `AreaManager` / `EnvironmentManager` | Mirrors areas.json | BSD-2-Clause | Use the JSON, not the enum |
| RuneLite core `skybox.txt` | https://github.com/runelite/runelite/blob/master/runelite-client/src/main/resources/net/runelite/client/plugins/skybox/skybox.txt (local copy: `ref/runelite/...` and `ref/cj/...`) | Line DSL: `bounds rx1 ry1 rx2 ry2`, `#RRGGBB` color, `b<N>` blend radius, `r x y` / `R x1 y1 x2 y2` paint regions, `c` / `C` paint chunks relative to last region. 1159 lines, 510 paint directives | Whole overworld + dungeons, color only; area names only as sparse `//` comments | BSD-2-Clause | Great worked example of region-driven sky color with blending; no structured names |
| RuneLite `Skybox.java` / `SkyboxPlugin.java` | https://github.com/runelite/runelite/tree/master/runelite-client/src/main/java/net/runelite/client/plugins/skybox | Java parser for skybox.txt into chunk to color map | Same | BSD-2-Clause | Pattern to copy for a `.txt` sky-theme map |
| OSRS Wiki map `basemaps.json` | https://maps.runescape.wiki/osrs/versions/2026-08-12_a/basemaps.json | JSON `{mapId, name, bounds, center}` | ~180 floor/dungeon maps keyed by mapId, not region ID | Wiki CC BY-NC-SA (unverified for this endpoint) | Wrong key space and granularity |
| Explv's Map | https://github.com/Explv/Explv.github.io | Vite/JS app | No regions/areas JSON located (unverified) | No LICENSE seen | Needs clone + grep |
| mejrs `layers_osrs` | https://github.com/mejrs/layers_osrs | Folders `locations/-1`, `mapsquares/-1`, `nomove/-1`; likely map tiles | Unknown (unverified) | Unknown | Unconfirmed |
| slaytostay/region-locker | https://github.com/slaytostay/region-locker | RuneLite plugin | Named-region list not confirmed (unverified) | Unverified | Unconfirmed |
| runelite `KourendLibraryPlugin.java` | https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/kourendlibrary/KourendLibraryPlugin.java | Java constant | One region (6459) | BSD-2-Clause | Not a dataset |
| osrsreboxed-db | https://github.com/0xNeffarion/osrsreboxed-db | JSON | Items / monsters only | MIT (unverified) | Wrong domain |
| DinkPlugin `::dinkregion` | https://github.com/pajlads/DinkPlugin | Chat command | Spot check only (Prifddinas = 12894/12895/13150/13151) | BSD-2-Clause | Not a dataset |

Local note: `ref/pr558` and `ref/pr655` contain only the 117HD skybox-feature diffs (GLSL + config), not areas.json / environments.json.

## Summary

1. Sky assets: ambientCG (CC0, API) + OpenGameArt Cloudy Skyboxes (CC0, 512 px faces ready) + NASA Deep Star Maps (PD) cover day, stylized and night. Skip Unity / Fab / NoEmotion / sIBL.
2. AI generation: Blockade Labs Skybox AI, one month of Essential, skydome preset. Flux LoRAs are all non-commercial. sdxl-360-diffusion (MIT) is the free fallback.
3. Area mapping: pull 117HD areas.json + environments.json (BSD-2), convert aabbs to region IDs, write our own `area -> sky preset` table modelled on environments.json. RuneLite skybox.txt shows the blend-radius trick.
4. Nothing found that maps region IDs to continent names out of the box; the 117HD JSON is the closest and needs a small conversion script.
