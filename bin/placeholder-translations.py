#!/usr/bin/env python3
"""
Google Sheets has a `GOOGLETRANSLATE` function that simplifies small-batch translations.
This script automates a little bit of the glue involved in using Google Sheets for creating placeholder translations.
It will automatically find strings (but not plurals) that need placeholder translations, create the Google Sheets formula calls, and copy them.
Afterwards, it will read the clipboard, decode the structure, and write the placeholder translations as needs review directly into Localizable.xcstrings.
All you need to do is paste, wait, and copy.
"""

import json
import subprocess
import time

with open("iosApp/iosApp/Localizable.xcstrings", mode="r", encoding="utf-8") as f:
    xcstrings = json.load(f)

target_locales = ["es", "fr", "ht", "pt-BR", "vi", "zh-Hans-CN", "zh-Hant-TW"]
missing_keys = dict()
for key, string_info in xcstrings["strings"].items():
    if "localizations" not in string_info:
        missing_keys[key] = key
    else:
        localizations = string_info["localizations"]
        if len(localizations) == 1 and "variations" in localizations["en"].keys():
            print(f"Cannot create placeholders for plural translation with key {repr(key)}; plurals are complicated.")

if len(missing_keys) == 0:
    print("Couldn’t find any strings that need placeholder translations.")
    exit()

print("Found", len(missing_keys), "strings that need placeholder translations.")

sheet_header = ["key", "en"] + target_locales
sheet_rows = [[key, missing_keys[key]] + [f'=GOOGLETRANSLATE("{missing_keys[key]}", "en", "{target}")' for target in target_locales] for key in sorted(missing_keys.keys())]
sheet_out = [sheet_header] + sheet_rows

subprocess.run("pbcopy", input="\n".join("\t".join(row) for row in sheet_out), text=True)

print("Open your Google Sheets spreadsheet, paste, wait for all the translations to load, and then copy.")
print("DO NOT PASTE HERE!")
garbage = input("Just press Enter and this script will read your clipboard.")

if garbage != "":
    while True:
        garbage_time = time.monotonic()
        print()
        input("You pasted. Press Enter once your terminal has finished pasting.")
        if time.monotonic() - garbage_time > 0.1:
            break

paste = subprocess.run("pbpaste", capture_output=True, text=True)
sheet_in = [line.strip().split("\t") for line in paste.stdout.split("\n") if line.strip() != ""]

sheet_in_header = sheet_in[0]

for body_row in sheet_in[1:]:
    key = body_row[0]
    localizations = dict()
    for col in range(2, len(body_row)):
        localization = {"stringUnit": {"state": "needs_review", "value": body_row[col]}}
        localizations[sheet_in_header[col]] = localization
    xcstrings["strings"][key]["localizations"] = localizations
    print("Adding placeholder translations for key", repr(key))

with open("iosApp/iosApp/Localizable.xcstrings", mode="w", encoding="utf-8") as f:
    json.dump(xcstrings, f, ensure_ascii=False, indent=2, separators=(",", " : "))

print("Running Android localization conversion")
subprocess.run(["./gradlew", "--quiet", ":androidApp:convertIosLocalization"], check=True)
