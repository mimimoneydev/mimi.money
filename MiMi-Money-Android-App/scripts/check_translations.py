#!/usr/bin/env python3
import os
import glob
import re
import xml.etree.ElementTree as ET

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ROOT = os.path.join(PROJECT_ROOT, 'app', 'src', 'main', 'res')
BASE_FILE = os.path.join(ROOT, 'values', 'strings.xml')


def discover_locales():
    locales = []
    for path in glob.glob(os.path.join(ROOT, 'values-*')):
        qualifier = os.path.basename(path)
        match = re.fullmatch(r'values-([a-z]{2}(?:-r[A-Z]{2})?)', qualifier)
        if match:
            locales.append(match.group(1))
    return sorted(locales)


def parse_strings(file_path):
    keys = []
    try:
        tree = ET.parse(file_path)
        root = tree.getroot()
        for child in root:
            if child.tag in ('string',):
                if child.get('translatable') == 'false':
                    continue
                name = child.get('name')
                if name:
                    keys.append(name)
            elif child.tag in ('plurals',):
                # Treat plurals as translatable resources too
                name = child.get('name')
                if name:
                    keys.append(name)
    except ET.ParseError as e:
        print(f"WARN: Failed to parse {file_path}: {e}")
    return keys


def collect_base_keys():
    return set(parse_strings(BASE_FILE))


def collect_locale_keys(locale):
    dir_path = os.path.join(ROOT, f'values-{locale}')
    keys = []
    for fp in glob.glob(os.path.join(dir_path, '*.xml')):
        keys.extend(parse_strings(fp))
    return set(keys)


def main():
    base_keys = collect_base_keys()
    print(f"Base keys: {len(base_keys)} from {BASE_FILE}")

    lines = []
    all_missing = {}
    for loc in discover_locales():
        loc_keys = collect_locale_keys(loc)
        missing = sorted(list(base_keys - loc_keys))
        all_missing[loc] = missing
        print(f"Locale {loc}: present={len(loc_keys)} missing={len(missing)}")
        if missing:
            lines.append(f"=== Missing in {loc} ({len(missing)}) ===")
            lines.extend(missing)
    out_dir = os.path.join(PROJECT_ROOT, 'app', 'build')
    os.makedirs(out_dir, exist_ok=True)
    out_path = os.path.join(out_dir, 'translation-report.txt')
    with open(out_path, 'w', encoding='utf-8') as f:
        if lines:
            f.write('\n'.join(lines) + '\n')
        else:
            f.write('All locales up-to-date. No missing keys.\n')
    print(f"Wrote report to {out_path}")


if __name__ == '__main__':
    main()
