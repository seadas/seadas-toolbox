import requests
import datetime
import re
import sys
import json
import os
from dateutil import parser
from pathlib import Path

BASE_DIR = os.path.join(
    os.getcwd(),  # Current working directory
    "seadas-toolbox", "seadas-earthdata-cloud-toolbox",
    "src", "main", "resources", "json-files"
)
class MissionDateRangeFinder:
    def __init__(self):
        self.known_missions = {
            "seawifs": {"start": "1997-09-01", "end": "2010-12-31"},
            "modis_aqua": {"start": "2002-05-04", "end": None},
            "modis_terra": {"start": "1999-12-18", "end": None},
            "viirs": {"start": "2011-10-28", "end": None},
            "landsat_8": {"start": "2013-02-11", "end": None},
            "pace": {"start": "2024-02-08", "end": None},
            "hawkeye": {"start": "2024-01-01", "end": None},
            "merged_s3_olci": {"start": "2016-02-16", "end": None},
            "sentinel_3_olci": {"start": "2016-02-16", "end": None},
            "sentinel_3a_olci": {"start": "2016-02-16", "end": None},
            "sentinel_3b_olci": {"start": "2018-04-25", "end": None},
        }
        self.suspicious_dates = ["1960-01-01", "1970-01-01", "1900-01-01", "2099-12-31", "2100-12-31"]
        self.earliest_possible_date = datetime.datetime(1960, 1, 1)
        self.future_cutoff = datetime.datetime.now() + datetime.timedelta(days=365)

    def normalize_mission_name(self, mission_name):
        return re.sub(r'[^a-zA-Z0-9]', '_', mission_name.lower())

    def is_suspicious_date(self, date_str):
        if not date_str:
            return True
        try:
            date = parser.parse(date_str)
            if any(date_str.startswith(sus) for sus in self.suspicious_dates):
                return True
            if date < self.earliest_possible_date or date > self.future_cutoff:
                return True
            return False
        except:
            return True

    def query_cmr_api(self, mission_name):
        url = "https://cmr.earthdata.nasa.gov/search/collections.json"
        params = {"keyword": mission_name, "page_size": 100}
        try:
            response = requests.get(url, params=params, timeout=10)
            if response.status_code != 200:
                return None, None
            collections = response.json().get('feed', {}).get('entry', [])
            start_dates, end_dates = [], []
            for c in collections:
                s, e = c.get('time_start'), c.get('time_end')
                if s and not self.is_suspicious_date(s):
                    start_dates.append(s)
                if e and not self.is_suspicious_date(e):
                    end_dates.append(e)
            return min(start_dates) if start_dates else None, max(end_dates) if end_dates else None
        except Exception as e:
            print(f"[CMR ERROR] {mission_name}: {e}")
            return None, None

    def get_mission_date_range(self, mission_name):
        key = self.normalize_mission_name(mission_name)
        if key in self.known_missions:
            return self.known_missions[key]["start"], self.known_missions[key]["end"] or "present"
        cmr_start, cmr_end = self.query_cmr_api(mission_name)
        return cmr_start, cmr_end or "present"

    def generate_json(self, mission_names, output_file="mission_date_ranges.json"):
        result = {}
        for name in mission_names:
            start, end = self.get_mission_date_range(name)
            if start:
                result[name] = {"start": start, "end": end}
        with open(output_file, "w", encoding="utf-8") as f:
            json.dump(result, f, indent=2)
        print(f" Saved {len(result)} mission ranges to {output_file}")


def read_mission_list(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        return [line.strip() for line in f if line.strip()]


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python generate_mission_date_ranges.py mission_names.txt")
        sys.exit(1)

    input_file = sys.argv[1]
    mission_list = read_mission_list(input_file)

    output_path = Path("src/main/resources/json-files/mission_date_ranges.json")
    output_path = os.path.join(BASE_DIR, f"mission_date_ranges.json")
#    output_path.parent.mkdir(parents=True, exist_ok=True)

    finder = MissionDateRangeFinder()
    finder.generate_json(mission_list, output_file=str(output_path))
