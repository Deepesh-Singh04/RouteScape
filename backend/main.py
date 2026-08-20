from fastapi import FastAPI, Depends, HTTPException
from sqlmodel import Session, select
from database import create_db_and_tables, get_session
from models import Place, PlaceCreate

app = FastAPI()

@app.on_event("startup")
def on_startup():
    create_db_and_tables()

@app.post("/places/", response_model=Place)
def create_place(place: PlaceCreate, session: Session = Depends(get_session)):
    db_place = Place.from_orm(place)
    session.add(db_place)
    session.commit()
    session.refresh(db_place)
    return db_place

@app.get("/places/", response_model=list[Place])
def read_places(category: str = None, session: Session = Depends(get_session)):
    query = select(Place)
    if category:
        query = query.where(Place.category == category)
    places = session.exec(query).all()
    return places

@app.get("/home-data/")
def get_home_data(session: Session = Depends(get_session)):
    transit_query = select(Place).where(Place.category == "transit")
    heritage_query = select(Place).where(Place.category == "heritage")
    
    transit_options = session.exec(transit_query).all()
    heritage_sites = session.exec(heritage_query).all()
    
    return {
        "transit_options": transit_options,
        "heritage_sites": heritage_sites
    }
