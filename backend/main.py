from fastapi import FastAPI, Query
from fastapi.responses import JSONResponse
import uvicorn

app = FastAPI(title="RouteScape MVP API - NSUT Edition")

MVP_DATA = {
    "anchor_location": {
        "name": "NSUT Main Gate",
        "coordinates": {"lat": 28.60882, "lng": 77.03588}
    },
    "radius_km": 3.0,
    "transit_options": [
        {
            "id": "t1",
            "type": "metro",
            "name": "Dwarka Mor Metro",
            "distance_meters": 1000,
            "eta_mins": 4,
            "status": "Blue Line Active",
            "coordinates": {"lat": 28.6195, "lng": 77.0332}
        },
        {
            "id": "t2",
            "type": "auto",
            "name": "NSUT E-Rickshaw Stand",
            "distance_meters": 50,
            "eta_mins": 1,
            "status": "Available",
            "coordinates": {"lat": 28.6095, "lng": 77.0350}
        }
    ],
    "heritage_sites": [
        {
            "id": "p1",
            "name": "Dwarka Baoli",
            "category": "Heritage Monument",
            "distance_meters": 1500,
            "entry_fee_inr": 0,
            "coordinates": {"lat": 28.6025, "lng": 77.0380}
        },
        {
            "id": "p2",
            "name": "ISKCON Temple Dwarka",
            "category": "Cultural Site",
            "distance_meters": 2800,
            "entry_fee_inr": 0,
            "coordinates": {"lat": 28.5950, "lng": 77.0450}
        }
    ]
}

@app.get("/api/v1/explore")
async def get_explore_data(
    lat: float = Query(default=28.60882, description="User latitude"),
    lng: float = Query(default=77.03588, description="User longitude")
):
    response_data = dict(MVP_DATA)
    response_data["anchor_location"] = {
        "name": "Current Location",
        "coordinates": {"lat": lat, "lng": lng}
    }
    return JSONResponse(content=response_data)

if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)