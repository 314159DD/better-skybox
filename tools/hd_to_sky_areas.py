"""Build sky_areas.json from 117 HD's areas.json + environments.json (BSD-2-Clause, github.com/117HD/RLHD).

Usage: python tools/hd_to_sky_areas.py            (expects ref/117hd/areas.json and environments.json)

environments.json is 117 HD's priority-ordered "which lighting applies here" table; we keep that order,
attach a cubemap from THEMES where we have one, and carry the fog colour over. Areas listed in EXTRA have
no environment entry but still deserve their own sky; they go after the environment entries.
Every entry gets flat geometry: aabbs as [x1, y1, x2, y2, plane1, plane2] in world tiles, regions as ids,
regionBoxes as [rx1, ry1, rx2, ry2, plane1, plane2] in region coordinates (117 HD gives the two corner region ids). Nested 117 HD areas are expanded.
"""
import json
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
HD = os.path.join(ROOT, "ref", "117hd")
OUT = os.path.join(ROOT, "src", "main", "resources", "com", "gpuskybox", "sky_areas.json")

SNOW = "snow_field_puresky"
MOUNTAIN = "drakensberg_solitary_mountain_puresky"
OVERCAST = "kloofendal_overcast_puresky"
MISTY = "kloofendal_28d_misty_puresky"
CLEAR = "syferfontein_18d_clear_puresky"
STORM = "wasteland_clouds_puresky"
PARTLY = "kloofendal_48d_partly_cloudy_puresky"
HAZY = "industrial_sunset_puresky"
DUSK = "qwantani_dusk_1_puresky"
MOONRISE = "qwantani_moonrise_puresky"

# 117 HD area name -> cubemap folder. Everything else falls back to the Cubemap setting.
THEMES = {
    "WILDERNESS_HIGH": STORM, "WILDERNESS_MID": STORM, "WILDERNESS_LOW": STORM,
    "Daimon's Crater - Bounty Hunter": STORM, "LMS_ARENA_WILD_VARROCK": STORM,
    "FROZEN_WASTE_PLATEAU": SNOW, "GIELINOR_SNOWY_NORTHERN_REGION": SNOW, "RELLEKKA_HUNTER_REGION": SNOW,
    "RELLEKKA_HUNTER_ICEBERG": SNOW, "ICEBERG": SNOW, "FREMENNIK_ISLES_FAR_NORTH": SNOW, "FREMENNIK_ISLES_NORTH": SNOW,
    "JATIZSO": SNOW, "MOUNTAIN_CAMP": SNOW, "MOUNTAIN_CAMP_ENTRY_PATH": SNOW, "ZEAH_SNOWY_NORTHERN_REGION": SNOW,
    "WINTERTODT_ARENA": SNOW, "VARLAMORE_SNOWY_MOUNTAINS_UPPER": SNOW, "VARLAMORE_SNOWY_MOUNTAINS_LOWER": SNOW,
    "WHITE_WOLF_MOUNTAIN_ENVIRONMENT": SNOW, "GRIMSTONE": SNOW, "UNGAEL": SNOW,
    "FREMENNIK_PROVINCE": MOUNTAIN, "WATERBIRTH_ISLAND": MOUNTAIN, "ISLAND_OF_STONE": MOUNTAIN, "NEITIZNOT": MOUNTAIN,
    "FREMENNIK_ISLES_MIDDLE": MOUNTAIN, "WYRMSCRAIG": MOUNTAIN,
    "MORYTANIA": OVERCAST, "VER_SINHAZA": OVERCAST, "DARKMEYER": OVERCAST, "MEIYERDITCH": OVERCAST, "BARROWS": OVERCAST,
    "DRAYNOR_MANOR": OVERCAST, "DRAYNOR_MANOR_FOREST": OVERCAST, "MISTHALIN_MYSTERY_MANOR_REGION": OVERCAST,
    "TEMPOROSS_COVE": OVERCAST, "THE_STORM_TEMPOR": OVERCAST, "THE_STORM_TEMPOR_EDGE": OVERCAST,
    "SOTE_LLETYA_ON_FIRE": OVERCAST, "SOTE_LLETYA_MOSTLY_DONE_BURNING": OVERCAST,
    "TAR_SWAMP": MISTY, "POISON_WASTE": MISTY, "TIRANNWN_MAINLAND": MISTY, "PRIFDDINAS": MISTY,
    "KEBOS_LOWLANDS": MISTY, "THE_STRANGLEWOOD": MISTY, "ENCHANTED_VALLEY": MISTY,
    "KHARIDIAN_DESERT": CLEAR, "KHARIDIAN_DESERT_MID": CLEAR, "KHARIDIAN_DESERT_DEEP": CLEAR,
    "AUBURNVALE": HAZY, "LOVAKENGJ": HAZY,
    "ARCEUUS": DUSK, "ZANARIS": MOONRISE, "EVIL_BOB_ISLAND": PARTLY,
}

DEEP_BLUE = "ambientcg_dayskyhdri055b"
PALE_MORNING = "ambientcg_morningskyhdri013b"

# Areas without an environment entry, in priority order, appended after the environment table. Either a name
# from areas.json, or our own world-tile boxes [x1, y1, x2, y2] for the lands 117 HD has no top-level area for.
# Islands and small lands first, continents last; anything still unmapped keeps the Cubemap setting.
EXTRA = [
    ("WILDERNESS", STORM, None), ("KARAMJA", PARTLY, None), ("KOUREND", MOUNTAIN, None), ("VARLAMORE", CLEAR, None),
    ("Entrana", CLEAR, [[2800, 3328, 2879, 3391]]),
    ("Crandor", HAZY, [[2816, 3264, 2879, 3327]]),
    ("Ape Atoll", PARTLY, [[2688, 2688, 2815, 2815]]),
    ("Lunar Isle", MOONRISE, [[2048, 3840, 2175, 3967]]),
    ("Miscellania", MOUNTAIN, [[2496, 3840, 2623, 3903]]),
    ("Isle of Souls", OVERCAST, [[2080, 2880, 2303, 3071]]),
    ("Corsair Cove", CLEAR, [[2496, 2816, 2623, 2879]]),
    ("Void Knights Outpost", OVERCAST, [[2624, 2560, 2687, 2623]]),
    ("Fossil Island", MISTY, [[3584, 3648, 3839, 3903]]),
    ("Piscatoris", MISTY, [[2304, 3584, 2431, 3711]]),
    ("Feldip Hills", PARTLY, [[2432, 2880, 2700, 3071]]),
    ("Al Kharid", CLEAR, [[3264, 3136, 3327, 3330]]),
    ("Misthalin", PARTLY, [[3066, 3136, 3455, 3519]]),
    ("Asgarnia", DEEP_BLUE, [[2816, 3136, 3065, 3583]]),
    ("Kandarin", PALE_MORNING, [[2432, 3072, 2815, 3583]]),
]

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


def load(name):
    s = open(os.path.join(HD, name), encoding="utf-8").read()
    s = re.sub(r"//[^\n]*", "", s)
    s = re.sub(r"/\*.*?\*/", "", s, flags=re.S)
    s = re.sub(r",(\s*[\]}])", r"\1", s)
    return json.loads(s)


def norm_aabb(b):
    if len(b) == 2:
        x1, y1, x2, y2, p1, p2 = b[0], b[1], b[0], b[1], 0, 3
    elif len(b) == 3:
        x1, y1, x2, y2, p1, p2 = b[0], b[1], b[0], b[1], b[2], b[2]
    elif len(b) == 4:
        x1, y1, x2, y2, p1, p2 = b[0], b[1], b[2], b[3], 0, 3
    elif len(b) == 5:
        x1, y1, x2, y2, p1, p2 = b[0], b[1], b[2], b[3], b[4], b[4]
    else:
        x1, y1, p1, x2, y2, p2 = b
    return [min(x1, x2), min(y1, y2), max(x1, x2), max(y1, y2), min(p1, p2), max(p1, p2)]


def norm_region_box(b):
    """117 HD: [cornerId1, cornerId2] plus optional plane or plane range."""
    x1, y1, x2, y2 = b[0] >> 8, b[0] & 0xFF, b[1] >> 8, b[1] & 0xFF
    p1 = b[2] if len(b) > 2 else 0
    p2 = b[3] if len(b) > 3 else (b[2] if len(b) > 2 else 3)
    return [min(x1, x2), min(y1, y2), max(x1, x2), max(y1, y2), min(p1, p2), max(p1, p2)]


def flatten(areas, name, seen=None):
    """aabbs, regions, regionBoxes of an area including nested areas."""
    seen = seen or set()
    a = areas.get(name)
    if a is None or name in seen:
        return [], [], []
    seen.add(name)
    aabbs = [norm_aabb(b) for b in a.get("aabbs", [])]
    regions = list(a.get("regions", []))
    boxes = [norm_region_box(b) for b in a.get("regionBoxes", [])]
    for child in a.get("areas", []):
        ca, cr, cb = flatten(areas, child, seen)
        aabbs += ca
        regions += cr
        boxes += cb
    return aabbs, regions, boxes


def entry(areas, name, sky, fog, boxes=None):
    if boxes is None:
        aabbs, regions, rboxes = flatten(areas, name)
    else:
        aabbs, regions, rboxes = [norm_aabb(b) for b in boxes], [], []
    if not (aabbs or regions or rboxes):
        return None
    e = {"name": name}
    if sky:
        e["sky"] = sky
    for key, table in (("skyDawn", DAWN_THEMES), ("skyDusk", DUSK_THEMES), ("skyNight", NIGHT_THEMES)):
        if name in table:
            e[key] = table[name]
    if fog:
        e["fog"] = fog
    if aabbs:
        e["aabbs"] = aabbs
    if regions:
        e["regions"] = regions
    if rboxes:
        e["regionBoxes"] = rboxes
    return e


def main():
    areas = {a["name"]: a for a in load("areas.json")}
    out = []
    for env in load("environments.json"):
        name = env.get("area")
        if not name or env.get("isUnderwater") or env.get("isPohTheme"):
            continue
        fog = env.get("fogColor")
        if isinstance(fog, list):  # a few entries use [r, g, b] floats
            fog = "#%02x%02x%02x" % tuple(int(round(c * 255)) for c in fog[:3])
        sky = THEMES.get(name)
        if not sky and not fog:
            continue
        e = entry(areas, name, sky, fog)
        if e and env.get("lightningEffects"):
            e["lightning"] = True
        if e:
            out.append(e)
    for name, sky, boxes in EXTRA:
        e = entry(areas, name, sky, None, boxes)
        if e:
            out.append(e)
    unknown = [n for n in THEMES if n not in areas]
    if unknown:
        print("THEMES names missing in areas.json:", unknown)
    with open(OUT, "w", encoding="utf-8", newline="\n") as f:
        f.write("[\n" + ",\n".join(json.dumps(e, separators=(",", ":")) for e in out) + "\n]\n")
    print("wrote", len(out), "areas,", sum(1 for e in out if "sky" in e), "with a sky, to", OUT)


if __name__ == "__main__":
    main()
