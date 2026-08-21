from sqlmodel import Session
from database import engine, create_db_and_tables
from models import Place

heritage_data = [
    {
        "name": "Shri Dudheshwar Nath Mandir",
        "latitude": 28.6655,
        "longitude": 77.4246,
        "category": "heritage",
        "distance_meters": 4500,
        "type": "historic_temple"
    },
    {
        "name": "Ghanta Ghar (Clock Tower)",
        "latitude": 28.6625,
        "longitude": 77.4278,
        "category": "heritage",
        "distance_meters": 3800,
        "type": "monument"
    },
    {
        "name": "Pracheen Shiv Mandir (Lal Kuan)",
        "latitude": 28.6290,
        "longitude": 77.4480,
        "category": "heritage",
        "distance_meters": 500,
        "type": "temple"
    },
    {
        "name": "Shri Siddh Pith Balaji Mandir",
        "latitude": 28.6362,
        "longitude": 77.4470,
        "category": "heritage",
        "distance_meters": 400,
        "type": "temple"
    },
    {
        "name": "Bhairavnath Mandir (Chipyana)",
        "latitude": 28.6180,
        "longitude": 77.4380,
        "category": "heritage",
        "distance_meters": 2100,
        "type": "temple"
    },
    {
        "name": "Dasna Fort Ruins",
        "latitude": 28.6750,
        "longitude": 77.5250,
        "category": "heritage",
        "distance_meters": 8500,
        "type": "ruins"
    },
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
