# Privacy Policy — 知天 (Zhitian)

**Last updated: May 3, 2026**

## Overview

知天 ("the App") is designed to provide weather forecasts combined with your calendar events. This privacy policy explains what data we access, how we use it, and your rights.

## Data We Access

### 1. Location Data
- **What:** Your device's approximate or precise location.
- **Why:** To provide weather forecasts for your current location.
- **How:** We use the Amap (高德) Location SDK to determine your position via GPS, cell towers, and WiFi. Your coordinates are then sent to QWeather (和风天气) API to retrieve weather data.
- **Third parties receiving location data:**
  - Amap (高德): processes location signals to determine coordinates and city name
  - QWeather (和风天气): receives coordinates to return weather data
- **Your control:** You can deny location permission at any time. The app will allow you to manually select a city in Settings.

### 2. Calendar Data
- **What:** Event titles, times, and dates from your device's calendar.
- **Why:** To display your upcoming events alongside weather information, helping you plan your day.
- **How:** Calendar data is read locally on your device. It is never transmitted to any server.
- **Your control:** Calendar access is optional. You can deny this permission and the app will function normally without showing calendar events.

### 3. City Search Queries
- **What:** City names you type in the search box.
- **Why:** To find the geographic coordinates of the city you want to view weather for.
- **How:** Search queries are sent to Open-Meteo's Geocoding API.

## Data We Do NOT Collect

- We do **not** collect personal information (name, email, phone number).
- We do **not** use analytics or tracking SDKs.
- We do **not** display advertisements.
- We do **not** store your location data on any server we operate.

## Data Storage

- Weather data is cached locally on your device (using an on-device database) to reduce network requests and enable offline access. This cache is automatically cleared after 24 hours.
- Your city preferences and settings are stored locally on your device.
- App-created calendar events are stored locally in an on-device database.
- No data is stored on external servers operated by us.

## Third-Party Services

| Service | Purpose | Privacy Policy |
|---------|---------|---------------|
| QWeather (和风天气) | Weather data (15-day forecast, hourly, warnings, AQI, life indices), city search | https://www.qweather.com/en/privacy |
| Amap (高德) Location SDK | Device positioning (GPS + cell tower + WiFi) | https://lbs.amap.com/pages/privacy/ |
| Open-Meteo | Backup weather data source | https://open-meteo.com/en/terms |

## Children's Privacy

The App does not knowingly collect data from children under 13. The App does not require any personal information to function.

## Changes to This Policy

We may update this privacy policy from time to time. Changes will be reflected in the "Last updated" date above.

## Contact

If you have questions about this privacy policy, please contact us via the GitHub repository:
**https://github.com/louise-mars/zhitian**
