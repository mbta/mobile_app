#!/usr/bin/env python3

import json

with open('iosApp/iosApp/Localizable.xcstrings', 'rt') as f:
    xcstrings_data: dict[str, dict] = json.load(f)

en_strings_needs_review: list[str] = []
for key, string_data in xcstrings_data['strings'].items():
    localizations = string_data['localizations']
    if 'stringUnit' in localizations['es']:
        if any(x['stringUnit']['state'] == 'needs_review' for x in localizations.values()):
            if 'en' in localizations:
                en_string = localizations['en']['stringUnit']['value']
            else:
                en_string = key
            en_strings_needs_review.append(en_string)
    elif 'variations' in localizations['es']:
        en_variations_plural: dict = localizations['en']['variations']['plural']
        en_strings = [x['stringUnit']['value'] for x in en_variations_plural.values()]
        en_strings_needs_review.extend(en_strings)
    else:
        raise ValueError(f"neither stringUnit nor variations found at key {key} in {localizations}")

en_words_needs_review = sum(len(x.split()) for x in en_strings_needs_review)
print(f"Translations that need review:")
print(f"{len(en_strings_needs_review)} strings, {en_words_needs_review} English words")
