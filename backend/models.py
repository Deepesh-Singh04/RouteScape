from sqlmodel import SQLModel, Field
from typing import Optional

class PlaceBase(SQLModel):
    name: str
    latitude: float
    longitude: float
    category: str 
    type: Optional[str] = None
    status: Optional[str] = None
    distance_meters: Optional[int] = None

class Place(PlaceBase, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)

class PlaceCreate(PlaceBase):
    pass
