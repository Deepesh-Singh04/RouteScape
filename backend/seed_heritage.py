from sqlmodel import Session
from database import engine, create_db_and_tables
from models import Place

heritage_data = [
    {
        "name": "Loharehri Baoli (Dwarka Stepwell)",
        "latitude": 28.5954,
        "longitude": 77.0423,
        "category": "heritage",
        "distance_meters": 2500,
        "type": "stepwell"
    },
    {
        "name": "Dada Dev Mandir",
        "latitude": 28.5921,
        "longitude": 77.0784,
        "category": "heritage",
        "distance_meters": 4500,
        "type": "temple"
    },
    {
        "name": "Hasthal Minar (Mini Qutub)",
        "latitude": 28.6335,
        "longitude": 77.0601,
        "category": "heritage",
        "distance_meters": 5500,
        "type": "monument"
    },
    {
        "name": "Malcha Mahal",
        "latitude": 28.5982,
        "longitude": 77.1711,
        "category": "heritage",
        "distance_meters": 13500,
        "type": "ruins"
    },
    {
        "name": "Bhuli Bhatiyari Ka Mahal",
        "latitude": 28.6433,
        "longitude": 77.1956,
        "category": "heritage",
        "distance_meters": 16000,
        "type": "monument"
    },
    {
        "name": "Zafar Mahal",
        "latitude": 28.5190,
        "longitude": 77.1754,
        "category": "heritage",
        "distance_meters": 16000,
        "type": "palace"
    },
    {
        "name": "Jahaz Mahal",
        "latitude": 28.5159,
        "longitude": 77.1722,
        "category": "heritage",
        "distance_meters": 16500,
        "type": "monument"
    },
    {
        "name": "Chor Minar (Tower of Thieves)",
        "latitude": 28.5447,
        "longitude": 77.2007,
        "category": "heritage",
        "distance_meters": 17000,
        "type": "monument"
    }
]

def seed_data():
    create_db_and_tables()
    with Session(engine) as session:
        # Clear existing heritage sites to avoid duplicates
        from sqlmodel import select
        existing_sites = session.exec(select(Place).where(Place.category == "heritage")).all()
        for site in existing_sites:
            session.delete(site)
            
        # Insert the new hidden gems
        for item in heritage_data:
            place = Place(**item)
            session.add(place)
        session.commit()
        
if __name__ == "__main__":
    seed_data()
