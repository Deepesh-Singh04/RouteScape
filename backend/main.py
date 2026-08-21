import httpx
from fastapi import FastAPI
from typing import List, Optional
from pydantic import BaseModel

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
    except Exception as e:
        print(f"Overpass API Error: {e}")
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
        except Exception as item_error:
            print(f"Skipping malformed element: {item_error}")
            continue
            
    return DynamicDataResponse(heritage_sites=heritage_sites)