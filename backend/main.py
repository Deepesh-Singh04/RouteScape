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
    
    async with httpx.AsyncClient() as client:
        response = await client.post(url, data={"data": query}, timeout=20.0)
        osm_data = response.json()
        
    heritage_sites = []
    for element in osm_data.get("elements", []):
        tags = element.get("tags", {})
        if "name" not in tags:
            continue
            
        element_lat = element.get("lat") or element.get("center", {}).get("lat")
        element_lon = element.get("lon") or element.get("center", {}).get("lon")
        
        heritage_sites.append(PlaceDto(
            id=element["id"],
            name=tags["name"],
            latitude=element_lat,
            longitude=element_lon,
            category="heritage",
            type=tags.get("historic")
        ))
        
    return DynamicDataResponse(heritage_sites=heritage_sites)