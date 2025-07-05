import sqlite3
import csv
import os

def init_stop(stations_csv, station_points_csv, db_path):
    # Remove existing DB if exists
    if os.path.exists(db_path):
        os.remove(db_path)

    conn = sqlite3.connect(db_path)
    cur = conn.cursor()

    # Create stations table
    cur.execute("""
        CREATE TABLE stations (
            UniqueId TEXT PRIMARY KEY,
            Name TEXT,
            FareZones TEXT,
            HubNaptanCode TEXT,
            Wifi TEXT,
            OutsideStationUniqueId TEXT,
            BlueBadgeCarParking TEXT,
            BlueBadgeCarParkSpaces TEXT,
            TaxiRanksOutsideStation TEXT,
            MainBusInterchange TEXT,
            PierInterchange TEXT,
            NationalRailInterchange TEXT,
            AirportInterchange TEXT,
            EmiratesAirLineInterchange TEXT
        )
    """)

    # Create station_points table
    cur.execute("""
        CREATE TABLE station_points (
            UniqueId TEXT PRIMARY KEY,
            StationUniqueId TEXT,
            AreaName TEXT,
            AreaId TEXT,
            Level INTEGER,
            Lat REAL,
            Lon REAL,
            FriendlyName TEXT,
            FOREIGN KEY (StationUniqueId) REFERENCES stations(UniqueId)
        )
    """)

    # Insert stations
    with open(stations_csv, newline='', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        rows = [
            (
                row['UniqueId'], row['Name'], row['FareZones'], row['HubNaptanCode'], row['Wifi'],
                row['OutsideStationUniqueId'], row['BlueBadgeCarParking'], row['BlueBadgeCarParkSpaces'],
                row['TaxiRanksOutsideStation'], row['MainBusInterchange'], row['PierInterchange'],
                row['NationalRailInterchange'], row['AirportInterchange'], row['EmiratesAirLineInterchange']
            )
            for row in reader
        ]
        cur.executemany("""
            INSERT INTO stations VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, rows)

    # Insert station_points
    with open(station_points_csv, newline='', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        rows = [
            (
                row['UniqueId'], row['StationUniqueId'], row['AreaName'], row['AreaId'], int(row['Level']),
                float(row['Lat']), float(row['Lon']), row['FriendlyName']
            )
            for row in reader
        ]
        cur.executemany("""
            INSERT INTO station_points VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """, rows)

    conn.commit()
    conn.close()

init_stations(
    "tfl-stationdata-detailed/Stations.csv",
    "tfl-stationdata-detailed/StationPoints.csv",
    "stations.sqlite"
)

def build_stop_list(db_path):
    """
    stopList = {
        "_stationID_": {
            "location":{
                "lat": "_lat_",
                "lng": "_lng_"
            },
            "name": {
                "en": "_name_",
                "zh": "_name_ch_"
            }
            }
        }, ...
    }
    """

    stop_list = {}
    conn = sqlite3.connect(db_path)
    cur = conn.cursor()
    cur.execute("""
        SELECT s.UniqueId, s.Name, sp.Lat, sp.Lon
        FROM stations s
        JOIN station_points sp ON s.UniqueId = sp.StationUniqueId
        WHERE sp.Level = 0
    """)
    for station_id, name, lat, lng in cur.fetchall():
        stop_list[station_id] = {
            "location": {
                "lat": lat,
                "lng": lng
            },
            "name": {
                "en": name,
                "zh": "NAME_CH_PLACEHOLDER"
            }
        }
    conn.close()
    return stop_list

# Example usage:
print(build_stop_list("stations.sqlite"))

