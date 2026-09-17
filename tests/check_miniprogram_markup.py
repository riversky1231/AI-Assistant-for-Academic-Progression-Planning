"""Check WXML structure without third-party dependencies; not a WeChat compiler."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1] / 'miniprogram'
count = 0
for file in root.rglob('*.wxml'):
    source = file.read_text(encoding='utf-8')
    # Expressions may contain XML operators; validate the surrounding markup.
    source = re.sub(r'\{\{.*?\}\}', 'expression', source, flags=re.S)
    # WXML permits boolean attributes such as `loading` and `wx:else`.
    source = re.sub(r'\s(wx:else|loading)(?=\s|/?>)(?!\s*=)', r' \1="true"', source)
    try:
        ET.fromstring('<root xmlns:wx="urn:wx">' + source + '</root>')
    except ET.ParseError as error:
        raise SystemExit(f'{file.relative_to(root)}: {error}') from error
    count += 1
print(f'WXML structure passed: {count} templates')
