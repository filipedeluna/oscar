#!/bin/bash
now=$(date)

python3 -m venv venv
source venv/bin/activate
pip install python-levenshtein numpy pathlib tqdm jellyfish

python3 bulktester.py >"${now}"-results.txt