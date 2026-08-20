# RouteScape

**An Applied AI Project**

## Overview
RouteScape is an interactive Android application designed to bridge the gap between local transit options and heritage site discovery. Built as an applied AI project, this application demonstrates how artificial intelligence assistants can be leveraged to accelerate full-stack development, moving from concept to functional software rapidly.

## Tech Stack
**Frontend (Android):**
*   **Kotlin & Jetpack Compose:** For the user interface.
*   **OSMDroid:** Open-source map rendering engine.
*   **CartoDB:** Map tile servers for a clean aesthetic (Voyager for Light Mode, Dark Matter for Dark Mode).
*   **Coroutines & Retrofit:** For background tasks and network calls.
*   **OSRM & Nominatim APIs:** Integrated for street-level routing and live geocoding/search.

**Backend (REST API):**
*   **Python 3**
*   **FastAPI:** Web framework for the API.
*   **Uvicorn:** Web server for Python.
*   **SQLModel & SQLite:** Lightweight database ORM for querying structured transit and heritage data.
*   **Render:** Cloud platform for live API hosting.

## Features
*   **Interactive Map:** A smooth, multi-touch enabled map using CartoDB tile servers for reliable rendering.
*   **Live GPS Tracking:** Native Android location services integration to drop a live blue dot on your physical location and snap the camera to your coordinates.
*   **Dynamic Place Discovery:** A horizontally scrolling interface that pulls live data for transit options and heritage sites. The database is seeded with hyper-local, lesser-known historical hidden gems.
*   **True Street-Level Routing:** Tapping any location card or searching a destination automatically queries OSRM, draws a street-following polyline with shadow casing on the map, and calculates real-world distance and ETA.
*   **Live Search Autocomplete:** Floating search bar with debounced Nominatim API integration for finding specific addresses globally.
*   **Adaptive Theming:** Material 3 semantic colors and dynamic tile-switching that transitions seamlessly between a low-glare Light Mode and a high-contrast Dark Mode based on system settings.
*   **Edge-Swipe Navigation:** Custom gesture detection that allows seamless map panning while reserving the screen edge for a sliding navigation menu.

## Technical Workflow
1.  The Android frontend sends a request, along with the user's live coordinates, to the Python FastAPI backend (now hosted on Render).
2.  The backend processes this request, querying the SQLite database, and returns a structured response containing transit nodes and heritage sites within a specific radius.
3.  The Android app reads this response and dynamically populates the screen with location cards.
4.  When a user interacts with a card or search suggestion, the app fetches routing geometry from OSRM, clears previous routes, draws a new path, and animates the camera to the chosen destination.

## Setup & Installation

### 1. Backend API (Python)

**Option A: Cloud Deployment (Current Setup)**
The backend is currently deployed and hosted live on Render. 
*   **Live URL:** `https://routescape-backend.onrender.com/`
*   Render automatically handles port assignment and serves the API over secure HTTPS.
*   The SQLite database is pre-seeded with hyper-local heritage sites.

**Option B: Local Development (For testing only)**
If you wish to run the server locally on your own machine:
1. Install dependencies: `pip install fastapi uvicorn sqlmodel`
2. Navigate to the backend directory: `cd backend`
3. Seed the local database: `python seed_heritage.py`
4. Start the server: `uvicorn main:app --reload`
5. The local server will run on `http://127.0.0.1:8000`.

### 2. Frontend App (Android Studio)
1. Open the `android-app` folder in Android Studio.
2. Ensure your `compileSdk` is set to `37` in your `app/build.gradle.kts` file.
3. Open `ApiService.kt`.
    * To use the live cloud data, set `BASE_URL = "https://routescape-backend.onrender.com/"`
    * To use a local testing server, set `BASE_URL = "http://10.0.2.2:8000/"`
4. Launch an Android Emulator or connect a physical device and click **Run**.
Author: Deepesh Singh, Deependra Pratap Singh.
