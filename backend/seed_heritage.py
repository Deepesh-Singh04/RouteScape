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
    {"name": "Bairam Khan's Tomb", "latitude": 28.5915, "longitude": 77.2450, "category": "heritage", "type": "tomb"},
    {"name": "Tomb of Abdul Rahim Khan-I-Khana", "latitude": 28.5885, "longitude": 77.2435, "category": "heritage", "type": "tomb"},
    {"name": "Tomb of Qutbuddin Bakhtiar Kaki", "latitude": 28.5225, "longitude": 77.1785, "category": "heritage", "type": "tomb"},
    {"name": "Hauz-i-Shamsi", "latitude": 28.5156, "longitude": 77.1764, "category": "heritage", "type": "historic_reservoir"},
    {"name": "Jahapanah City Walls", "latitude": 28.5385, "longitude": 77.2058, "category": "heritage", "type": "ruins"},
    {"name": "Tomb of Sultan Ghari", "latitude": 28.5284, "longitude": 77.1353, "category": "heritage", "type": "tomb"},
    {"name": "Tomb of Bu Halima", "latitude": 28.5935, "longitude": 77.2465, "category": "heritage", "type": "tomb"},
    {"name": "Nila Gumbad", "latitude": 28.5940, "longitude": 77.2480, "category": "heritage", "type": "tomb"},
    {"name": "Arab Ki Sarai", "latitude": 28.5930, "longitude": 77.2470, "category": "heritage", "type": "historic_caravanserai"},
    {"name": "Afsarwala Tomb and Mosque", "latitude": 28.5932, "longitude": 77.2472, "category": "heritage", "type": "mosque_tomb"},
    {"name": "Nai-ka-Kot", "latitude": 28.5140, "longitude": 77.2050, "category": "heritage", "type": "fort_ruins"},
    {"name": "Adilabad Fort", "latitude": 28.5085, "longitude": 77.2655, "category": "heritage", "type": "fort_ruins"},
    {"name": "Tomb of Ghiyasuddin Tughlaq", "latitude": 28.5135, "longitude": 77.2605, "category": "heritage", "type": "tomb"},
    {"name": "Nai Ka Gumbad", "latitude": 28.5945, "longitude": 77.2205, "category": "heritage", "type": "tomb"},
    {"name": "Shahpur Jat Fort Ruins", "latitude": 28.5490, "longitude": 77.2140, "category": "heritage", "type": "ruins"},
    {"name": "Chunnamal Haveli", "latitude": 28.6560, "longitude": 77.2285, "category": "heritage", "type": "residence"},
    {"name": "Namak Haram Ki Haveli", "latitude": 28.6580, "longitude": 77.2300, "category": "heritage", "type": "residence"},
    {"name": "Bagh-i-Alam Ka Gumbad", "latitude": 28.5565, "longitude": 77.2045, "category": "heritage", "type": "tomb"},
    {"name": "Kali Masjid (Nizamuddin)", "latitude": 28.5910, "longitude": 77.2400, "category": "heritage", "type": "mosque"},
    {"name": "Kalu Sarai Masjid", "latitude": 28.5410, "longitude": 77.1980, "category": "heritage", "type": "mosque_ruins"},
    {"name": "Kharera Village Ruins", "latitude": 28.5500, "longitude": 77.2000, "category": "heritage", "type": "ruins"},
    {"name": "Tomb of Mubarak Shah", "latitude": 28.5710, "longitude": 77.2215, "category": "heritage", "type": "tomb"},
    {"name": "Darya Khan's Tomb", "latitude": 28.5740, "longitude": 77.2180, "category": "heritage", "type": "tomb"},
    {"name": "Bade Khaan Tomb", "latitude": 28.5725, "longitude": 77.2205, "category": "heritage", "type": "tomb"},
    {"name": "Chote Khaan Tomb", "latitude": 28.5720, "longitude": 77.2210, "category": "heritage", "type": "tomb"},
    {"name": "Kale Khan Ka Gumbad", "latitude": 28.5700, "longitude": 77.2220, "category": "heritage", "type": "tomb"},
    {"name": "Bhure Khan Ka Gumbad", "latitude": 28.5730, "longitude": 77.2190, "category": "heritage", "type": "tomb"},
    {"name": "Tomb of Hasan Khan Suri", "latitude": 28.5938, "longitude": 77.2468, "category": "heritage", "type": "tomb"},
    {"name": "Qudsia Bagh", "latitude": 28.6655, "longitude": 77.2255, "category": "heritage", "type": "historic_garden"},
    {"name": "Nicholson Cemetery", "latitude": 28.6665, "longitude": 77.2245, "category": "heritage", "type": "cemetery"},
    {"name": "Lothian Cemetery", "latitude": 28.6610, "longitude": 77.2290, "category": "heritage", "type": "cemetery"},
    {"name": "Flagstaff Tower", "latitude": 28.6815, "longitude": 77.2110, "category": "heritage", "type": "tower"},
    {"name": "Pir Ghaib (Observatory)", "latitude": 28.6730, "longitude": 77.2120, "category": "heritage", "type": "observatory"},
    {"name": "Chauburji Marg Ruins", "latitude": 28.6740, "longitude": 77.2140, "category": "heritage", "type": "monument"},
    {"name": "Tomb of Shah Alam I", "latitude": 28.5220, "longitude": 77.1775, "category": "heritage", "type": "tomb"},
    {"name": "Khan Shahid's Tomb (Balban's Son)", "latitude": 28.5185, "longitude": 77.1875, "category": "heritage", "type": "tomb"},
    {"name": "Lal Gumbad", "latitude": 28.5350, "longitude": 77.2125, "category": "heritage", "type": "tomb"},
    {"name": "Basilica of Our Lady of Graces (Sardhana)", "latitude": 29.1478, "longitude": 77.6186, "category": "heritage", "type": "historic_church"},
    {"name": "Begum Samru's Palace (Sardhana)", "latitude": 29.1465, "longitude": 77.6170, "category": "heritage", "type": "palace"},
    {"name": "Barnawa Laksha Griha", "latitude": 29.1200, "longitude": 77.4300, "category": "heritage", "type": "ancient_site"},
    {"name": "Pura Mahadeva Temple", "latitude": 29.1500, "longitude": 77.4500, "category": "heritage", "type": "historic_temple"},
    {"name": "Augharnath Temple", "latitude": 29.0065, "longitude": 77.6970, "category": "heritage", "type": "historic_temple"},
    {"name": "Shahpir Mausoleum", "latitude": 28.9800, "longitude": 77.7000, "category": "heritage", "type": "tomb"},
    {"name": "Surajpur Historic Bird Sanctuary", "latitude": 28.5150, "longitude": 77.4600, "category": "heritage", "type": "nature_reserve"},
    {"name": "Tomb of Sheikh Kabiruddin", "latitude": 28.5300, "longitude": 77.2100, "category": "heritage", "type": "tomb"},
    {"name": "Baradari of Sikandar Lodi", "latitude": 28.5960, "longitude": 77.2195, "category": "heritage", "type": "pavilion"}
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