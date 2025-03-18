#!/usr/bin/env python3

import argparse
import csv
from datetime import datetime
import os

parser = argparse.ArgumentParser(
    prog='csv-to-xliff',
    description='Convert a CSV file of translations to an Xliff file for import into Xcode'
)

parser.add_argument('csv', help='The CSV file to convert')
default_dir = "translations-" + datetime.today().strftime('%Y-%m-%d')
parser.add_argument('-o', '--output', default=default_dir, help='The output directory to write the Xliff files to')
args = parser.parse_args()

def make_xliff(lang, translations):
    text = '''<?xml version="1.0" encoding="UTF-8"?>
<xliff xmlns="urn:oasis:names:tc:xliff:document:1.2" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" version="1.2" xsi:schemaLocation="urn:oasis:names:tc:xliff:document:1.2 http://docs.oasis-open.org/xliff/v1.2/os/xliff-core-1.2-strict.xsd">
  <file original="iosApp/Localizable.xcstrings" source-language="en" target-language="''' + lang + '''" datatype="plaintext">
    <header>
      <tool tool-id="com.apple.dt.xcode" tool-name="Xcode" tool-version="16.2" build-num="16C5032a" />
    </header>
    <body>\n'''
    for en, translation in translations.items():
        text += '      <trans-unit id="' + en + '" xml:space="preserve">\n        <source>' + en + '</source>\n        <target state="translated">' + translation + '</target>\n      </trans-unit>\n'
    text += '''    </body>
  </file>
</xliff>'''
    return text


langs = []
translations = {}

with open(args.csv, newline='') as csvfile:
    reader = csv.reader(csvfile, delimiter=',')

    for row in reader:
        if reader.line_num == 1:
            langs = row[1:]
            for lang in langs:
                translations[lang] = {}
            print(langs)
        else:
            en = row[0]
            for i, trans in enumerate(row[1:]):
                lang = langs[i]
                translations[lang][en] = trans

dir = args.output
os.makedirs(dir, exist_ok=True)

for lang, translation in translations.items():
    filename = dir + '/' + lang + '.xliff'
    with open(filename, 'w') as file:
        file.write(make_xliff(lang, translation))
        print("wrote " + filename)
