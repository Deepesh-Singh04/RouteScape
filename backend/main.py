import httpx
import math
from fastapi import FastAPI
from typing import List, Optional
from pydantic import BaseModel
from sqlmodel import Session, select
from database import engine
from models import Place

app = FastAPI()

class PlaceDto(BaseModel):
    id: int
    name: str
    latitude: float
    longitude: float
    category: str
    type: Optional[str] = None
    distance_meters: Optional[int] = None

class DynamicDataResponse(BaseModel):
    transit_options: List[PlaceDto] = []
    heritage_sites: List[PlaceDto] = []

@app.get("/home-data/", response_model=DynamicDataResponse)
def get_home_data(lat: Optional[float] = None, lon: Optional[float] = None, radius: int = 25000):
    with Session(engine) as session:
        places = session.exec(select(Place)).all()
        
    heritage_sites = []
    for p in places:
        # If the Android app sends GPS coordinates, calculate the real distance
        if lat is not None and lon is not None:
            actual_distance = calculate_distance(lat, lon, p.latitude, p.longitude)
            
            # Skip this place if it is outside our 25km search radius
            if actual_distance > radius:
                continue
        else:
            # Fallback if GPS fails
            actual_distance = p.distance_meters 
            
        heritage_sites.append(PlaceDto(
            id=p.id,
            name=p.name,
            latitude=p.latitude,
            longitude=p.longitude,
            category=p.category,
            type=p.type,
            distance_meters=actual_distance
        ))
        
    # Sort the final list so the closest places appear first in the Android UI
    if lat is not None and lon is not None:
        heritage_sites.sort(key=lambda x: x.distance_meters or 999999)
        
    return DynamicDataResponse(heritage_sites=heritage_sites)
    
def calculate_distance(lat1: float, lon1: float, lat2: float, lon2: float) -> int:
    """Calculates the distance in meters between two GPS coordinates."""
    R = 6371000  # Radius of Earth in meters
    phi_1 = math.radians(lat1)
    phi_2 = math.radians(lat2)
    delta_phi = math.radians(lat2 - lat1)
    delta_lambda = math.radians(lon2 - lon1)
    
    a = math.sin(delta_phi / 2.0) ** 2 + \
        math.cos(phi_1) * math.cos(phi_2) * math.sin(delta_lambda / 2.0) ** 2
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
    
    return int(R * c)

@app.get("/explore/", response_model=DynamicDataResponse)
async def get_explore_data(lat: float, lon: float, radius: int = 5000):
    query = f"""
    [out:json];
    (
      node["historic"](around:{radius},{lat},{lon});
      way["historic"](around:{radius},{lat},{lon});
    );
    out center;
    """
    url = "http://overpass-api.de/api/interpreter"
    
    try:
        async with httpx.AsyncClient() as client:
            response = await client.post(url, data={"data": query}, timeout=20.0)
            response.raise_for_status() 
            osm_data = response.json()
    except Exception:
        return DynamicDataResponse(transit_options=[], heritage_sites=[])
        
    heritage_sites = []
    for element in osm_data.get("elements", []):
        try:
            tags = element.get("tags", {})
            if "name" not in tags:
                continue
                
            element_lat = element.get("lat") or element.get("center", {}).get("lat")
            element_lon = element.get("lon") or element.get("center", {}).get("lon")
            
            if element_lat is None or element_lon is None:
                continue
            
            heritage_sites.append(PlaceDto(
                id=element["id"],
                name=tags["name"],
                latitude=float(element_lat),
                longitude=float(element_lon),
                category="heritage",
                type=tags.get("historic", "unknown")
            ))
        except Exception:
            continue
            
    return DynamicDataResponse(heritage_sites=heritage_sites)