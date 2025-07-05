import sqlite3
import csv
import os

def init_tube_stops(stations_csv, station_points_csv, db_path):
    conn = sqlite3.connect(db_path)
    cur = conn.cursor()

    # Drop existing tables if they exist
    cur.execute("DROP TABLE IF EXISTS tube_stations")
    cur.execute("DROP TABLE IF EXISTS tube_station_points")

    # Create stations table
    cur.execute("""
        CREATE TABLE tube_stations (
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

    # Create tube_station_points table
    cur.execute("""
        CREATE TABLE tube_station_points (
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

    # Insert tube stations
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
        cur.executemany("INSERT INTO tube_stations VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", rows)

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
            INSERT INTO tube_station_points VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """, rows)

    conn.commit()
    conn.close()

def init_bus_routes(bus_stops_csv, bus_routes_csv, db_path):
    conn = sqlite3.connect(db_path)
    cur = conn.cursor()

    # Drop bus_routes table if it exists
    cur.execute("DROP TABLE IF EXISTS bus_stops")
    cur.execute("DROP TABLE IF EXISTS bus_routes")

    # Create bus_routes table
    cur.execute("""
        CREATE TABLE bus_stops (
            Stop_Code_LBSL TEXT,
            Bus_Stop_Code TEXT,
            Naptan_Atco TEXT,
            Stop_Name TEXT,
            Location_Easting REAL,
            Location_Northing REAL,
            Heading REAL,
            Stop_Area TEXT,
            Virtual_Bus_Stop TEXT,
            PRIMARY KEY (Stop_Code_LBSL, Bus_Stop_Code)
        )
    """)

    # Insert bus routes
    with open(bus_stops_csv, newline='', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        rows = [
            (
            row['Stop_Code_LBSL'], row['Bus_Stop_Code'], row['Naptan_Atco'], row['Stop_Name'],
            row['Location_Easting'], row['Location_Northing'], row['Heading'],
            row['Stop_Area'], row['Virtual_Bus_Stop']
            )
            for row in reader
        ]

        cur.executemany("INSERT INTO bus_stops (Stop_Code_LBSL, Bus_Stop_Code, Naptan_Atco, Stop_Name, Location_Easting, Location_Northing, Heading, Stop_Area, Virtual_Bus_Stop) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", rows)

    cur.execute("""
        CREATE TABLE bus_routes (
        Route TEXT,
        Run TEXT,
        Sequence INTEGER,
        Stop_Code_LBSL TEXT,
        Bus_Stop_Code TEXT,
        Naptan_Atco TEXT,
        Stop_Name TEXT,
        Location_Easting REAL,
        Location_Northing REAL,
        Heading TEXT,
        Virtual_Bus_Stop TEXT
        )
    """)

    # Insert bus routes from bus_routes.csv
    with open(bus_routes_csv, newline='', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        rows = [
            (
                row['Route'], row['Run'], int(row['Sequence']), row['Stop_Code_LBSL'], row['Bus_Stop_Code'], row['Naptan_Atco'], row['Stop_Name'], float(row['Location_Easting']), float(row['Location_Northing']), (row['Heading']), row['Virtual_Bus_Stop']
            )
            for row in reader
        ]
        cur.executemany("INSERT INTO bus_routes (Route, Run, Sequence, Stop_Code_LBSL, Bus_Stop_Code, Naptan_Atco, Stop_Name, Location_Easting, Location_Northing, Heading, Virtual_Bus_Stop) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", rows)

    conn.commit()
    conn.close()

init_tube_stops(
    "tfl_downloaded_data/Stations.csv",
    "tfl_downloaded_data/StationPoints.csv",
    "db.sqlite"
)

init_bus_routes(
    "tfl_downloaded_data/bus-stops.csv",
    "tfl_downloaded_data/bus-sequences.csv",
    "db.sqlite")

def build_tube_stop_list_csv_from_db(db_path):
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

