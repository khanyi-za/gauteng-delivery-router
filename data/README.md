# Map data

The road network comes from OpenStreetMap. The map file is large, so it is not kept in Git.
Everything in this folder except this README is ignored.

The ingester expects the file at `data/gauteng.osm.pbf`
(configurable with `gautenglogistics.ingest.osm-file`).

## Option 1: BBBike (easiest)

1. Go to https://extract.bbbike.org
2. Format: **Protocolbuffer (PBF)**
3. Draw a box around Gauteng, roughly:
   - south-west: `-27.35, 27.10`
   - north-east: `-25.10, 29.10`
4. Enter your email and submit. You'll be emailed a download link.
5. Save the file as `data/gauteng.osm.pbf`.

## Option 2: Geofabrik + osmium

Geofabrik publishes South Africa (including Lesotho) as one file. Cut it down to Gauteng with osmium:

```sh
brew install osmium-tool
curl -L -o data/south-africa.osm.pbf \
  https://download.geofabrik.de/africa/south-africa-latest.osm.pbf
osmium extract --bbox 27.10,-27.35,29.10,-25.10 \
  data/south-africa.osm.pbf -o data/gauteng.osm.pbf
```

osmium takes the box as `lon,lat` (west,south,east,north).

## Licence

OpenStreetMap data is © OpenStreetMap contributors and is available under the
Open Database License (ODbL): https://www.openstreetmap.org/copyright
