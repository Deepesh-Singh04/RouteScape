# RouteScape 🗺️

**An Applied AI Project**

## Overview
RouteScape is an interactive Android application designed to bridge the gap between local transit options and heritage site discovery. Built as an applied AI project, this application demonstrates how artificial intelligence assistants can be leveraged to accelerate full-stack development, moving from concept to functional software rapidly. 

## Tech Stack
**Frontend (Android):**
*   **Kotlin & Jetpack Compose:** For the user interface.
*   **OSMDroid:** Open-source map rendering engine.
*   **CartoDB Positron:** Map tile servers for a clean aesthetic.
*   **Coroutines & Retrofit:** For background tasks and network calls.

**Backend (REST API):**
*   **Python 3**
*   **FastAPI:** Web framework for the API.
*   **Uvicorn:** Web server for Python.

## Features
*   **Interactive Map:** A smooth, multi-touch enabled map using CartoDB tile servers for reliable rendering.
*   **Live GPS Tracking:** Native Android location services integration to drop a live blue dot on your physical location and snap the camera to your coordinates.
*   **Dynamic Place Discovery:** A horizontally scrolling interface that pulls live data for transit options and heritage sites.
*   **Tap-to-Route Panning:** Tapping any location card automatically draws a colored route on the map and animates the camera to the destination.
*   **Edge-Swipe Navigation:** Custom gesture detection that allows seamless map panning while reserving the screen edge for a sliding navigation menu.

## Technical Workflow
1.  The Android frontend sends a request, along with the user's live coordinates, to the Python FastAPI backend.
2.  The backend processes this request and returns a structured response containing anchor coordinates, transit nodes, and heritage sites within a specific radius.
3.  The Android app reads this response and dynamically populates the screen with location cards.
4.  When a user interacts with a card, the app passes the coordinates to the map, which clears previous routes, draws a new path, and animates the camera to the chosen destination.

## Setup & Installation

### 1. Run the Backend (Python)
1. Ensure Python 3 is installed on your computer.
2. Install the required dependencies:
   ```bash
   pip install fastapi uvicorn
4. Navigate to the backend directory:
   ```bash
   cd backend
5. Start the server:
   ```bash
   python main.py
6. The server will run on port 8000.
### 2. Run the Frontend (Android Studio)

1. Open the `android-app` folder in Android Studio.
2. Ensure your `compileSdk` is set to `37` in your `app/build.gradle.kts` file.
3. Launch an Android Emulator (API 24+) and click **Run**. 

> **Note:** The app expects the backend to be running on the emulator's local network bridge (`http://10.0.2.2:8000/`), so it will connect automatically.

---
**Author:** Deepesh Singh, Deependra Pratap Singh.
   
