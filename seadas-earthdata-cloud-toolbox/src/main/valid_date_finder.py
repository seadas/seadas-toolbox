import requests
import datetime
import re
from dateutil import parser

class MissionDateRangeFinder:
    def __init__(self):
        # Known mission dates for common missions (as a fallback)
        self.known_missions = {
            "seawifs": {"start": "1997-09-01", "end": "2010-12-31"},
            "modis_aqua": {"start": "2002-05-04", "end": None},  # Still operational
            "modis_terra": {"start": "1999-12-18", "end": None},  # Still operational
            "viirs": {"start": "2011-10-28", "end": None},  # Still operational
            "landsat_8": {"start": "2013-02-11", "end": None},  # Still operational
            "pace": {"start": "2024-02-08", "end": None},  # Recently launched
            "hawkeye": {"start": "2024-01-01", "end": None},  # HAWKEYE mission
            "merged_s3_olci": {"start": "2016-02-16", "end": None},  # Sentinel-3 OLCI merged product
            "sentinel_3_olci": {"start": "2016-02-16", "end": None},  # Original Sentinel-3 OLCI
            "sentinel_3a_olci": {"start": "2016-02-16", "end": None},  # Sentinel-3A OLCI
            "sentinel_3b_olci": {"start": "2018-04-25", "end": None},  # Sentinel-3B OLCI
            # Add more as needed
        }

        # Suspicious date patterns (likely defaults)
        self.suspicious_dates = [
            "1960-01-01", "1970-01-01", "1900-01-01", "2099-12-31", "2100-12-31"
        ]

        # Earliest possible satellite data date (first Earth observation satellite)
        self.earliest_possible_date = datetime.datetime(1960, 1, 1)

        # Future cutoff (missions can't have end dates too far in the future)
        self.future_cutoff = datetime.datetime.now() + datetime.timedelta(days=365)

    def normalize_mission_name(self, mission_name):
        """Convert mission name to a standardized format for lookup"""
        return re.sub(r'[^a-zA-Z0-9]', '_', mission_name.lower())

    def is_suspicious_date(self, date_str):
        """Check if a date looks like a placeholder/default value"""
        if not date_str:
            return True

        try:
            date = parser.parse(date_str)

            # Check if it's in our list of known suspicious dates
            if any(date_str.startswith(sus_date) for sus_date in self.suspicious_dates):
                return True

            # Check if date is before satellites existed or too far in future
            if date < self.earliest_possible_date or date > self.future_cutoff:
                return True

            return False
        except:
            return True

    def check_mission_status(self, mission_name):
        """Check if a mission is new, upcoming, or in commissioning phase"""
        # Dictionary of missions that are new, upcoming, or in commissioning
        new_missions = {
            "pace": {
                "status": "commissioning",
                "launch_date": "2024-02-08",
                "expected_data_availability": "Mid-2024",
                "description": "Plankton, Aerosol, Cloud, ocean Ecosystem mission"
            },
            "swot": {
                "status": "operational",
                "launch_date": "2022-12-16",
                "expected_data_availability": "Available now (limited)",
                "description": "Surface Water and Ocean Topography mission"
            },
            "nisar": {
                "status": "upcoming",
                "launch_date": "2024 (planned)",
                "expected_data_availability": "TBD",
                "description": "NASA-ISRO Synthetic Aperture Radar mission"
            },
            "hawkeye": {
                "status": "operational",
                "launch_date": "2024-01-01",
                "expected_data_availability": "Available now",
                "description": "High-resolution Atmospheric Wind and Kinetic Energy Experiment"
            },
            "merged_s3_olci": {
                "status": "operational",
                "launch_date": "2016-02-16",  # Sentinel-3A launch date
                "expected_data_availability": "Available now",
                "description": "Merged data from Sentinel-3A and 3B OLCI instruments"
            }
            # Add other new missions as they are announced/launched
        }

        normalized_name = self.normalize_mission_name(mission_name)

        # Check if this is a new/upcoming mission
        for mission_key, info in new_missions.items():
            if mission_key in normalized_name or normalized_name in mission_key:
                return info

        return None

    def check_merged_product(self, mission_name):
        """Handle special case for merged products that combine multiple missions"""
        merged_products = {
            "merged_s3_olci": {
                "component_missions": ["sentinel_3a_olci", "sentinel_3b_olci"],
                "start_date": "2016-02-16",  # Earliest component (S3A) launch
                "description": "Merged Sentinel-3 OLCI product combining S3A and S3B data"
            },
            # Add other merged products as needed
        }

        normalized_name = self.normalize_mission_name(mission_name)

        for product_key, info in merged_products.items():
            if product_key in normalized_name or normalized_name in product_key:
                return info

        return None

    def query_cmr_api(self, mission_name):
        """Query NASA's Common Metadata Repository API"""
        url = "https://cmr.earthdata.nasa.gov/search/collections.json"
        params = {
            "keyword": mission_name,
            "page_size": 100
        }

        try:
            response = requests.get(url, params=params, timeout=10)
            if response.status_code != 200:
                return None, None

            collections = response.json().get('feed', {}).get('entry', [])

            start_dates = []
            end_dates = []

            for collection in collections:
                time_start = collection.get('time_start')
                time_end = collection.get('time_end')

                if time_start and not self.is_suspicious_date(time_start):
                    start_dates.append(time_start)

                if time_end and not self.is_suspicious_date(time_end):
                    end_dates.append(time_end)

            earliest = min(start_dates) if start_dates else None
            latest = max(end_dates) if end_dates else None

            return earliest, latest
        except Exception as e:
            print(f"CMR API error: {e}")
            return None, None

    def query_worldview_api(self, mission_name):
        """Query NASA Worldview API for layer information"""
        url = "https://worldview.earthdata.nasa.gov/config/wv.json"

        try:
            response = requests.get(url, timeout=10)
            if response.status_code != 200:
                return None, None

            data = response.json()

            start_dates = []
            end_dates = []

            # Look for layers matching the mission name
            for layer_id, layer_info in data.get('layers', {}).items():
                if mission_name.lower() in layer_id.lower():
                    start_date = layer_info.get('startDate')
                    end_date = layer_info.get('endDate')

                    if start_date and not self.is_suspicious_date(start_date):
                        start_dates.append(start_date)

                    if end_date and not self.is_suspicious_date(end_date):
                        end_dates.append(end_date)

            earliest = min(start_dates) if start_dates else None
            latest = max(end_dates) if end_dates else None

            return earliest, latest
        except Exception as e:
            print(f"Worldview API error: {e}")
            return None, None

    def get_mission_date_range(self, mission_name):
        """Get mission date range using multiple methods and validation"""
        normalized_name = self.normalize_mission_name(mission_name)

        # First check if we have known dates for this mission
        if normalized_name in self.known_missions:
            return (
                self.known_missions[normalized_name]["start"],
                self.known_missions[normalized_name]["end"] or "present"
            )

        # Check if this is a merged product
        merged_info = self.check_merged_product(mission_name)
        if merged_info:
            return (merged_info["start_date"], "present")

        # Check if this is a new/upcoming mission
        mission_status = self.check_mission_status(mission_name)
        if mission_status:
            if mission_status["status"] == "upcoming":
                return (None, None)  # No data available yet
            elif mission_status["status"] == "commissioning":
                return (mission_status["launch_date"], "present")  # Limited or no data yet
            elif mission_status["status"] == "operational":
                return (mission_status["launch_date"], "present")

        # Try CMR API
        cmr_start, cmr_end = self.query_cmr_api(mission_name)

        # Try Worldview API
        wv_start, wv_end = self.query_worldview_api(mission_name)

        # Combine and validate results
        start_candidates = [d for d in [cmr_start, wv_start] if d]
        end_candidates = [d for d in [cmr_end, wv_end] if d]

        if start_candidates:
            start_date = min(start_candidates)
        else:
            start_date = None

        if end_candidates:
            end_date = max(end_candidates)
        else:
            # If we have a start date but no end date, the mission might still be active
            if start_candidates:
                end_date = "present"
            else:
                end_date = None

        return start_date, end_date

# Example usage
finder = MissionDateRangeFinder()

# Test with Merged S3 OLCI
merged_s3_start, merged_s3_end = finder.get_mission_date_range("merged_s3_olci")
print(f"Merged S3 OLCI dates: {merged_s3_start} to {merged_s3_end}")