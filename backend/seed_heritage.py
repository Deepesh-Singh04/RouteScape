from sqlmodel import Session
from database import engine, create_db_and_tables
from models import Place

heritage_data = [
    {
        "name": "Qutub Minar",
        "latitude": 28.5244,
        "longitude": 77.1855,
        "category": "heritage",
        "distance_meters": 15000
    },
    {
        "name": "Red Fort",
        "latitude": 28.6562,
        "longitude": 77.2410,
        "category": "heritage",
        "distance_meters": 22000
    },
    {
        "name": "Humayun's Tomb",
        "latitude": 28.5933,
        "longitude": 77.2507,
        "category": "heritage",
        "distance_meters": 18000
    },
    {
        "name": "India Gate",
        "latitude": 28.6129,
        "longitude": 77.2295,
        "category": "heritage",
        "distance_meters": 17000
    }
]

def seed_data():
    create_db_and_tables()
    with Session(engine) as session:
        for item in heritage_data:
            place = Place(**item)
            session.add(place)
        session.commit()
        
if __name__ == "__main__":
    seed_data()
