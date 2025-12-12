package com.example.travelbuddy

/**
 * Wspólna mapa krajów i ich miast używana w całej aplikacji.
 * Używana do cascading dropdown w formularzu podróży oraz filtrowania miast w pogodzie.
 */
object CountryCitiesData {
    
    val citiesByCountry: Map<String, List<String>> = mapOf(
        // Europa Środkowa
        "Polska" to listOf("Warszawa", "Kraków", "Gdańsk", "Wrocław", "Poznań", "Łódź", "Katowice", "Lublin", "Białystok", "Szczecin", "Gdynia", "Bydgoszcz", "Zakopane", "Sopot"),
        "Niemcy" to listOf("Berlin", "Monachium", "Hamburg", "Frankfurt", "Kolonia", "Drezno", "Stuttgart", "Düsseldorf", "Hanower", "Lipsk", "Norymberga", "Brema"),
        "Austria" to listOf("Wiedeń", "Salzburg", "Innsbruck", "Graz", "Linz", "Klagenfurt", "Villach", "Wels"),
        "Szwajcaria" to listOf("Zurych", "Genewa", "Berno", "Bazylea", "Lozanna", "Lucerna", "St. Gallen", "Lugano"),
        "Czechy" to listOf("Praga", "Brno", "Ostrawa", "Pilzno", "Liberec", "Ołomuniec", "České Budějovice", "Karlowe Wary"),
        "Słowacja" to listOf("Bratysława", "Koszyce", "Preszów", "Żylina", "Nitra", "Bańska Bystrzyca", "Trnawa", "Trenczyn"),
        "Węgry" to listOf("Budapeszt", "Debreczyn", "Szeged", "Miszkolc", "Pécs", "Győr", "Nyíregyháza", "Kecskemét"),
        
        // Bałkany
        "Albania" to listOf("Tirana", "Durrës", "Vlorë", "Shkodër", "Korçë", "Elbasan", "Sarandë", "Berat", "Gjirokastër", "Pogradec"),
        "Chorwacja" to listOf("Zagrzeb", "Split", "Dubrownik", "Rijeka", "Zadar", "Pula", "Osijek", "Šibenik", "Rovinj", "Trogir", "Hvar"),
        "Czarnogóra" to listOf("Podgorica", "Nikšić", "Pljevlja", "Bijelo Polje", "Cetinje", "Bar", "Kotor", "Budva", "Herceg Novi", "Ulcinj"),
        "Bośnia i Hercegowina" to listOf("Sarajewo", "Banja Luka", "Tuzla", "Mostar", "Zenica", "Bihać", "Brčko", "Trebinje"),
        "Serbia" to listOf("Belgrad", "Nowy Sad", "Niš", "Kragujevac", "Subotica", "Zrenjanin", "Pančevo", "Čačak"),
        "Słowenia" to listOf("Lublana", "Maribor", "Celje", "Kranj", "Velenje", "Koper", "Nova Gorica", "Bled", "Piran"),
        "Macedonia Północna" to listOf("Skopje", "Bitola", "Kumanovo", "Prilep", "Tetovo", "Ohrid", "Veles", "Štip"),
        "Kosowo" to listOf("Prisztina", "Prizren", "Peć", "Mitrovica", "Gjakova", "Ferizaj", "Gjilan"),
        "Bułgaria" to listOf("Sofia", "Płowdiw", "Warna", "Burgas", "Ruse", "Stara Zagora", "Płewen", "Słoneczny Brzeg"),
        "Rumunia" to listOf("Bukareszt", "Kluż-Napoka", "Timișoara", "Jassy", "Konstanca", "Krajowa", "Braszów", "Sybin"),
        
        // Europa Południowa
        "Grecja" to listOf("Ateny", "Saloniki", "Patras", "Heraklion", "Larisa", "Wolos", "Rodos", "Korfu", "Santoryn", "Mykonos", "Chania"),
        "Turcja" to listOf("Stambuł", "Ankara", "Izmir", "Bursa", "Antalya", "Adana", "Konya", "Gaziantep", "Mersin", "Kapadocja", "Bodrum", "Fethiye"),
        "Włochy" to listOf("Rzym", "Mediolan", "Neapol", "Turyn", "Palermo", "Genua", "Bologna", "Florencja", "Wenecja", "Werona", "Piza", "Amalfi", "Sycylia", "Sardynia"),
        "Hiszpania" to listOf("Madryt", "Barcelona", "Walencja", "Sewilla", "Bilbao", "Málaga", "Murcja", "Palma de Mallorca", "Granada", "Alicante", "Kordoba", "San Sebastian"),
        "Portugalia" to listOf("Lizbona", "Porto", "Braga", "Coimbra", "Faro", "Évora", "Funchal", "Sintra", "Cascais", "Algarve"),
        "Malta" to listOf("Valletta", "Sliema", "St. Julian's", "Mdina", "Gozo", "Marsaxlokk"),
        "Cypr" to listOf("Nikozja", "Limassol", "Larnaka", "Pafos", "Ayia Napa", "Protaras"),
        
        // Europa Zachodnia
        "Francja" to listOf("Paryż", "Marsylia", "Lyon", "Tuluza", "Nicea", "Nantes", "Strasburg", "Bordeaux", "Lille", "Montpellier", "Cannes", "Monako"),
        "Wielka Brytania" to listOf("Londyn", "Birmingham", "Manchester", "Glasgow", "Liverpool", "Leeds", "Edynburg", "Bristol", "Cardiff", "Belfast", "Oxford", "Cambridge"),
        "Irlandia" to listOf("Dublin", "Cork", "Limerick", "Galway", "Waterford", "Drogheda", "Killarney", "Kilkenny"),
        "Belgia" to listOf("Bruksela", "Antwerpia", "Gandawa", "Brugia", "Liège", "Charleroi", "Namur", "Leuven"),
        "Holandia" to listOf("Amsterdam", "Rotterdam", "Haga", "Utrecht", "Eindhoven", "Groningen", "Maastricht", "Delft"),
        "Luksemburg" to listOf("Luksemburg", "Esch-sur-Alzette", "Differdange", "Dudelange"),
        
        // Europa Północna
        "Dania" to listOf("Kopenhaga", "Aarhus", "Odense", "Aalborg", "Esbjerg", "Roskilde"),
        "Szwecja" to listOf("Sztokholm", "Göteborg", "Malmö", "Uppsala", "Västerås", "Örebro", "Linköping"),
        "Norwegia" to listOf("Oslo", "Bergen", "Trondheim", "Stavanger", "Drammen", "Tromsø", "Kristiansand"),
        "Finlandia" to listOf("Helsinki", "Espoo", "Tampere", "Vantaa", "Oulu", "Turku", "Rovaniemi"),
        "Islandia" to listOf("Reykjavik", "Akureyri", "Keflavik", "Selfoss", "Ísafjörður"),
        "Estonia" to listOf("Tallinn", "Tartu", "Narwa", "Pärnu", "Kohtla-Järve"),
        "Łotwa" to listOf("Ryga", "Daugavpils", "Liepāja", "Jelgava", "Jūrmala"),
        "Litwa" to listOf("Wilno", "Kowno", "Kłajpeda", "Szawle", "Poniewież", "Troki"),
        
        // Europa Wschodnia
        "Ukraina" to listOf("Kijów", "Lwów", "Odessa", "Charków", "Dniepr", "Zaporoże"),
        "Białoruś" to listOf("Mińsk", "Homel", "Mohylew", "Witebsk", "Grodno", "Brześć"),
        "Mołdawia" to listOf("Kiszyniów", "Tyraspol", "Bielce", "Bendery"),
        "Rosja" to listOf("Moskwa", "Sankt Petersburg", "Kazań", "Soczi", "Kaliningrad"),
        
        // Azja
        "Japonia" to listOf("Tokio", "Osaka", "Kioto", "Jokohama", "Nagoja", "Sapporo", "Kobe", "Fukuoka", "Hiroszima", "Nara"),
        "Chiny" to listOf("Pekin", "Szanghaj", "Hongkong", "Shenzhen", "Kanton", "Chengdu", "Xi'an", "Hangzhou"),
        "Korea Południowa" to listOf("Seul", "Pusan", "Inczon", "Daegu", "Daejon", "Gwangju"),
        "Tajlandia" to listOf("Bangkok", "Chiang Mai", "Phuket", "Pattaya", "Krabi", "Koh Samui", "Hua Hin"),
        "Wietnam" to listOf("Hanoi", "Ho Chi Minh", "Da Nang", "Hoi An", "Nha Trang", "Ha Long Bay"),
        "Indonezja" to listOf("Dżakarta", "Bali", "Yogyakarta", "Surabaya", "Bandung", "Lombok"),
        "Malezja" to listOf("Kuala Lumpur", "George Town", "Malakka", "Johor Bahru", "Langkawi", "Kuching"),
        "Singapur" to listOf("Singapur"),
        "Indie" to listOf("Delhi", "Mumbai", "Bangalore", "Kalkuta", "Chennai", "Goa", "Jaipur", "Agra"),
        "Zjednoczone Emiraty Arabskie" to listOf("Dubaj", "Abu Dhabi", "Szardża", "Adżman"),
        "Izrael" to listOf("Tel Awiw", "Jerozolima", "Hajfa", "Ejlat", "Nazaret"),
        
        // Ameryka Północna
        "Stany Zjednoczone" to listOf("Nowy Jork", "Los Angeles", "Chicago", "Houston", "Phoenix", "Filadelfia", "San Antonio", "San Diego", "Dallas", "San Francisco", "Miami", "Las Vegas", "Boston", "Seattle", "Waszyngton"),
        "Kanada" to listOf("Toronto", "Montreal", "Vancouver", "Calgary", "Edmonton", "Ottawa", "Quebec", "Winnipeg"),
        "Meksyk" to listOf("Meksyk", "Cancún", "Guadalajara", "Monterrey", "Tijuana", "Playa del Carmen", "Puerto Vallarta"),
        
        // Ameryka Południowa
        "Brazylia" to listOf("Rio de Janeiro", "São Paulo", "Salvador", "Brasília", "Fortaleza", "Belo Horizonte", "Manaus"),
        "Argentyna" to listOf("Buenos Aires", "Córdoba", "Rosario", "Mendoza", "Mar del Plata", "Ushuaia"),
        "Chile" to listOf("Santiago", "Valparaíso", "Concepción", "Viña del Mar", "Punta Arenas"),
        "Peru" to listOf("Lima", "Cusco", "Arequipa", "Trujillo", "Machu Picchu"),
        "Kolumbia" to listOf("Bogota", "Medellín", "Cali", "Barranquilla", "Cartagena"),
        
        // Afryka
        "Egipt" to listOf("Kair", "Aleksandria", "Luksor", "Asuan", "Hurghada", "Sharm el-Sheikh", "Giza"),
        "Maroko" to listOf("Casablanca", "Marrakesz", "Fez", "Tanger", "Agadir", "Rabat"),
        "Tunezja" to listOf("Tunis", "Sousse", "Sfax", "Hammamet", "Djerba"),
        "RPA" to listOf("Kapsztad", "Johannesburg", "Durban", "Pretoria", "Port Elizabeth"),
        "Kenia" to listOf("Nairobi", "Mombasa", "Kisumu", "Nakuru"),
        "Tanzania" to listOf("Dar es Salaam", "Zanzibar", "Arusha", "Mwanza"),
        
        // Oceania
        "Australia" to listOf("Sydney", "Melbourne", "Brisbane", "Perth", "Adelaide", "Gold Coast", "Cairns", "Darwin"),
        "Nowa Zelandia" to listOf("Auckland", "Wellington", "Christchurch", "Queenstown", "Rotorua", "Dunedin")
    )
    
    /**
     * Pobierz listę miast dla danego kraju
     */
    fun getCitiesForCountry(country: String): List<String> {
        return citiesByCountry[country] ?: emptyList()
    }
    
    /**
     * Pobierz listę wszystkich krajów z dostępnymi miastami
     */
    fun getCountriesWithCities(): List<String> {
        return citiesByCountry.keys.sorted()
    }
    
    /**
     * Sprawdź czy kraj ma zdefiniowane miasta
     */
    fun hasDefinedCities(country: String): Boolean {
        return citiesByCountry.containsKey(country) && citiesByCountry[country]?.isNotEmpty() == true
    }
}

