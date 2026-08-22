from sqlmodel import Session
from database import engine, create_db_and_tables
from models import Place

# 45 Lesser-Known Heritage & Tourism Sites across Delhi/NCR & UP
heritage_data = [
    {"name": "Dasna Fort Ruins", "latitude": 28.6750, "longitude": 77.5250, "category": "heritage", "type": "ruins"},
    {"name": "Shri Dudheshwar Nath Mandir", "latitude": 28.6655, "longitude": 77.4246, "category": "heritage", "type": "historic_temple"},
    {"name": "Ghanta Ghar (Clock Tower)", "latitude": 28.6625, "longitude": 77.4278, "category": "heritage", "type": "monument"},
    {"name": "Bhuli Bhatiyari Ka Mahal", "latitude": 28.6427, "longitude": 77.1994, "category": "heritage", "type": "hunting_lodge"},
    {"name": "Chor Minar", "latitude": 28.5444, "longitude": 77.1950, "category": "heritage", "type": "tower"},
    {"name": "Jahaz Mahal", "latitude": 28.5165, "longitude": 77.1812, "category": "heritage", "type": "palace"},
    {"name": "Adham Khan's Tomb", "latitude": 28.5236, "longitude": 77.1828, "category": "heritage", "type": "tomb"},
    {"name": "Jamali Kamali Mosque & Tomb", "latitude": 28.5190, "longitude": 77.1870, "category": "heritage", "type": "mosque_tomb"},
    {"name": "Zafar Mahal", "latitude": 28.5204, "longitude": 77.1770, "category": "heritage", "type": "palace"},
    {"name": "Rajon Ki Baoli", "latitude": 28.5186, "longitude": 77.1866, "category": "heritage", "type": "stepwell"},
    {"name": "Gandhak Ki Baoli", "latitude": 28.5209, "longitude": 77.1834, "category": "heritage", "type": "stepwell"},
    {"name": "Agrasen Ki Baoli", "latitude": 28.6261, "longitude": 77.2250, "category": "heritage", "type": "stepwell"},
    {"name": "Mutiny Memorial", "latitude": 28.6703, "longitude": 77.2104, "category": "heritage", "type": "memorial"},
    {"name": "Khirki Mosque", "latitude": 28.5307, "longitude": 77.2195, "category": "heritage", "type": "mosque"},
    {"name": "Tomb of Balban", "latitude": 28.5187, "longitude": 77.1873, "category": "heritage", "type": "tomb"},
    {"name": "Ashokan Pillar (Hindu Rao)", "latitude": 28.6721, "longitude": 77.2131, "category": "heritage", "type": "pillar"},
    {"name": "Qila Rai Pithora", "latitude": 28.5255, "longitude": 77.1896, "category": "heritage", "type": "fort"},
    {"name": "Bara Gumbad", "latitude": 28.5941, "longitude": 77.2199, "category": "heritage", "type": "tomb"},
    {"name": "Sheesh Mahal (Shalimar Bagh)", "latitude": 28.7186, "longitude": 77.1614, "category": "heritage", "type": "palace"},
    {"name": "Dadi Poti's Tomb", "latitude": 28.5560, "longitude": 77.1951, "category": "heritage", "type": "tomb"},
    {"name": "Barakhamba Monument", "latitude": 28.5906, "longitude": 77.2405, "category": "heritage", "type": "tomb"},
    {"name": "Tomb of Sikandar Lodi", "latitude": 28.5954, "longitude": 77.2189, "category": "heritage", "type": "tomb"},
    {"name": "Isa Khan's Tomb", "latitude": 28.5925, "longitude": 77.2483, "category": "heritage", "type": "tomb"},
    {"name": "Nili Masjid", "latitude": 28.5448, "longitude": 77.1993, "category": "heritage", "type": "mosque"},
    {"name": "Idgah of Hauz Khas", "latitude": 28.5458, "longitude": 77.1947, "category": "heritage", "type": "mosque"},
    {"name": "Tomb of Feroz Shah Tughlaq", "latitude": 28.5471, "longitude": 77.1940, "category": "heritage", "type": "tomb"},
    {"name": "Chausath Khamba", "latitude": 28.5912, "longitude": 77.2427, "category": "heritage", "type": "tomb"},
    {"name": "Ataga Khan's Tomb", "latitude": 28.5921, "longitude": 77.2428, "category": "heritage", "type": "tomb"},
    {"name": "Bahlol Lodi's Tomb", "latitude": 28.5833, "longitude": 77.2281, "category": "heritage", "type": "tomb"},
    {"name": "Satpula", "latitude": 28.5283, "longitude": 77.2201, "category": "heritage", "type": "bridge_dam"},
    {"name": "Tomb of Imam Zamin", "latitude": 28.5242, "longitude": 77.1852, "category": "heritage", "type": "tomb"},
    {"name": "Alai Minar", "latitude": 28.5259, "longitude": 77.1837, "category": "heritage", "type": "tower"},
    {"name": "Moth Ki Masjid", "latitude": 28.5574, "longitude": 77.2185, "category": "heritage", "type": "mosque"},
    {"name": "Zeenat-ul-Masajid", "latitude": 28.6441, "longitude": 77.2458, "category": "heritage", "type": "mosque"},
    {"name": "Fatehpuri Masjid", "latitude": 28.6582, "longitude": 77.2215, "category": "heritage", "type": "mosque"},
    {"name": "Tomb of Roshanara Begum", "latitude": 28.6811, "longitude": 77.1963, "category": "heritage", "type": "tomb"},
    {"name": "Razia Sultan's Tomb", "latitude": 28.6436, "longitude": 77.2346, "category": "heritage", "type": "tomb"},
    {"name": "Mirza Ghalib ki Haveli", "latitude": 28.6521, "longitude": 77.2307, "category": "heritage", "type": "residence"},
    {"name": "Hastsal Minar", "latitude": 28.6293, "longitude": 77.0543, "category": "heritage", "type": "tower"},
    {"name": "Tomb of Azim Khan", "latitude": 28.5133, "longitude": 77.1843, "category": "heritage", "type": "tomb"},
    {"name": "Bijay Mandal", "latitude": 28.5414, "longitude": 77.2030, "category": "heritage", "type": "palace_ruins"},
    {"name": "Begumpur Mosque", "latitude": 28.5398, "longitude": 77.2052, "category": "heritage", "type": "mosque"},
    {"name": "Hijron Ka Khanqah", "latitude": 28.5222, "longitude": 77.1819, "category": "heritage", "type": "monument"},
    {"name": "Loharehri Baoli", "latitude": 28.6225, "longitude": 77.0142, "category": "heritage", "type": "stepwell"},
    {"name": "Bairam Khan's Tomb", "latitude": 28.5915, "longitude": 77.2450, "category": "heritage", "type": "tomb"}
]

def seed_data():
    create_db_and_tables()
    with Session(engine) as session:
        # Clear existing heritage sites to avoid duplicates
        from sqlmodel import select
        existing_sites = session.exec(select(Place).where(Place.category == "heritage")).all()
        for site in existing_sites:
            session.delete(site)
            
        # Insert the massive list of new gems
        for item in heritage_data:
            place = Place(**item)
            session.add(place)
        session.commit()
        
    print(f"Successfully seeded {len(heritage_data)} hidden heritage sites into the database!")
        
if __name__ == "__main__":
    seed_data()