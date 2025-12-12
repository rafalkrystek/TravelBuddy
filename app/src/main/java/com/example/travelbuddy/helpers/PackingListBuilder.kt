package com.example.travelbuddy.helpers

object PackingListBuilder {
    fun addBasicItems(list: MutableList<String>) {
        list.addAll(listOf(
            "Paszport", "Dowód osobisty", "Bilety", "Rezerwacje",
            "Gotówka", "Karty płatnicze", "Telefon", "Ładowarka do telefonu",
            "Powerbank", "Apteczka pierwszej pomocy", "Leki osobiste"
        ))
    }
    
    fun addUnderwear(list: MutableList<String>, count: Int) {
        list.add("Majtki x$count")
        list.add("Skarpetki x$count")
    }
    
    fun addClothesForClimate(
        list: MutableList<String>,
        climateType: String,
        avgTemp: Double,
        isBackpack: Boolean,
        tripDays: Int,
        packingDays: Int,
        gender: String
    ) {
        when (climateType) {
            "BARDZO_GORĄCO", "GORĄCO" -> {
                val tshirtCount = if (isBackpack) packingDays else tripDays
                val shortsCount = if (isBackpack) minOf(3, packingDays) else (tripDays / 2 + 1).coerceAtLeast(2)
                list.addAll(listOf(
                    "Koszulki/bluzki letnie x$tshirtCount",
                    "Krótkie spodenki x$shortsCount",
                    if (!isBackpack) "Lekkie spodnie długie x1 (na wieczór/klimatyzację)" else "",
                    "Sandały/klapki x1",
                    "Lekkie buty sportowe x1",
                    "Okulary przeciwsłoneczne x1",
                    "Krem z filtrem SPF 50+ x1",
                    "Kapelusz/czapka z daszkiem x1",
                    "Lekka koszula z długim rękawem x1"
                ).filter { it.isNotEmpty() })
                if (gender == "Kobieta") list.add("Lekkie sukienki/spódnice x2")
            }
            "CIEPŁO" -> {
                val tshirtCount = if (isBackpack) packingDays else tripDays
                val shortsCount = if (isBackpack) minOf(2, packingDays) else (tripDays / 3 + 1).coerceAtLeast(1)
                val pantsCount = if (isBackpack) minOf(2, packingDays) else (tripDays / 2 + 1).coerceAtLeast(2)
                list.addAll(listOf(
                    "Koszulki x$tshirtCount",
                    "Krótkie spodenki x$shortsCount",
                    "Spodnie długie x$pantsCount",
                    "Lekka bluza/sweter x1",
                    "Lekka kurtka wiosenna x1",
                    "Buty sportowe x1",
                    if (!isBackpack) "Sandały x1" else "",
                    "Okulary przeciwsłoneczne x1",
                    "Krem z filtrem SPF 30 x1"
                ).filter { it.isNotEmpty() })
            }
            "CHŁODNO" -> {
                val tshirtCount = if (isBackpack) packingDays else tripDays
                val longSleeveCount = if (isBackpack) minOf(2, packingDays) else (tripDays / 3 + 1).coerceAtLeast(2)
                val pantsCount = if (isBackpack) minOf(2, packingDays) else (tripDays / 2 + 1).coerceAtLeast(2)
                list.addAll(listOf(
                    "Koszulki x$tshirtCount",
                    "Koszule/bluzki długi rękaw x$longSleeveCount",
                    "Spodnie długie x$pantsCount",
                    "Sweter/bluza x2",
                    "Kurtka przejściowa x1",
                    "Buty sportowe x1",
                    if (!isBackpack) "Buty na zmianę x1" else "",
                    "Parasol składany x1",
                    "Lekka czapka x1"
                ).filter { it.isNotEmpty() })
            }
            "ZIMNO" -> {
                val tshirtCount = if (isBackpack) packingDays else tripDays
                val sweaterCount = if (isBackpack) minOf(2, packingDays) else (tripDays / 3 + 1).coerceAtLeast(2)
                val pantsCount = if (isBackpack) minOf(2, packingDays) else (tripDays / 2 + 1).coerceAtLeast(2)
                list.addAll(listOf(
                    "Koszulki/podkoszulki x$tshirtCount",
                    "Swetry/bluzy ciepłe x$sweaterCount",
                    "Spodnie długie x$pantsCount",
                    "Ciepła kurtka x1",
                    "Czapka x1",
                    "Rękawiczki lekkie x1",
                    "Szalik/komin x1",
                    "Ciepłe buty x1",
                    if (!isBackpack) "Buty na zmianę x1" else ""
                ).filter { it.isNotEmpty() })
            }
            "BARDZO_ZIMNO" -> {
                val tshirtCount = if (isBackpack) packingDays else tripDays
                val sweaterCount = if (isBackpack) minOf(3, packingDays) else (tripDays / 2 + 1).coerceAtLeast(3)
                val pantsCount = if (isBackpack) minOf(2, packingDays) else (tripDays / 2 + 1).coerceAtLeast(2)
                list.addAll(listOf(
                    "Bielizna termoaktywna x2",
                    "Koszulki/podkoszulki x$tshirtCount",
                    "Grube swetry/bluzy x$sweaterCount",
                    "Spodnie długie ciepłe x$pantsCount",
                    "Kurtka zimowa x1",
                    "Ciepła czapka zimowa x1",
                    "Rękawiczki zimowe x1",
                    "Szalik gruby x1",
                    "Buty zimowe ocieplane x1",
                    if (!isBackpack) "Buty na zmianę x1" else ""
                ).filter { it.isNotEmpty() })
            }
        }
    }
    
    fun addActivityItems(list: MutableList<String>, activities: Set<String>) {
        if (activities.contains("Plażowanie") || activities.contains("Sporty wodne")) {
            list.addAll(listOf("Strój kąpielowy x1", "Ręcznik plażowy x1"))
            if (!list.any { it.contains("Okulary przeciwsłoneczne") }) list.add("Okulary przeciwsłoneczne x1")
        }
        if (activities.contains("Góry i trekking")) {
            list.addAll(listOf("Buty trekkingowe x1", "Plecak trekkingowy x1", "Butelka na wodę x1"))
        }
        if (activities.contains("Nurkowanie")) {
            list.addAll(listOf("Maska do nurkowania x1", "Płetwy x1", "Fajka do nurkowania x1"))
        }
        if (activities.contains("Narty") || activities.contains("Snowboard")) {
            list.addAll(listOf("Kask narciarski x1", "Gogle narciarskie x1", "Rękawice narciarskie x1"))
        }
    }
    
    fun addToiletries(list: MutableList<String>) {
        list.addAll(listOf(
            "Szczoteczka do zębów x1", "Pasta do zębów x1", "Dezodorant x1",
            "Szampon x1", "Żel pod prysznic x1", "Ręcznik x1"
        ))
    }
    
    fun addWomenItems(list: MutableList<String>, isBackpack: Boolean, packingDays: Int, tripDays: Int) {
        val braCount = if (isBackpack) minOf(2, packingDays) else (tripDays / 2 + 1).coerceAtLeast(2)
        list.addAll(listOf(
            "Kosmetyczka x1", "Odżywka do włosów x1", "Krem do twarzy x1",
            "Szminka x1", "Staniki x$braCount"
        ))
    }
    
    fun addChildrenItems(
        list: MutableList<String>,
        childrenCount: Int,
        isBackpack: Boolean,
        tripDays: Int,
        climateType: String
    ) {
        for (i in 1..childrenCount) {
            val childPackingDays = if (isBackpack) minOf(4, tripDays) else tripDays
            val childUnderwear = if (isBackpack) childPackingDays + 1 else tripDays + 2
            val childTshirtCount = if (isBackpack) childPackingDays else tripDays
            val childPantsCount = if (isBackpack) minOf(2, childPackingDays) else (tripDays / 2 + 1).coerceAtLeast(2)
            
            list.addAll(listOf(
                "Majtki dla dziecka $i x$childUnderwear",
                "Skarpetki dla dziecka $i x$childUnderwear",
                "Koszulki dla dziecka $i x$childTshirtCount"
            ))
            
            when (climateType) {
                "BARDZO_GORĄCO", "GORĄCO" -> {
                    list.addAll(listOf(
                        "Krótkie spodenki dla dziecka $i x$childPantsCount",
                        "Sandały dla dziecka $i x1",
                        "Kapelusz/czapka dla dziecka $i x1"
                    ))
                }
                "CIEPŁO", "CHŁODNO" -> {
                    list.addAll(listOf(
                        "Spodnie dla dziecka $i x$childPantsCount",
                        "Bluza dla dziecka $i x1",
                        "Buty dla dziecka $i x1"
                    ))
                }
                "ZIMNO", "BARDZO_ZIMNO" -> {
                    list.addAll(listOf(
                        "Ciepłe spodnie dla dziecka $i x$childPantsCount",
                        "Ciepła kurtka dla dziecka $i x1",
                        "Czapka zimowa dla dziecka $i x1",
                        "Rękawiczki dla dziecka $i x1",
                        "Ciepłe buty dla dziecka $i x1"
                    ))
                }
            }
        }
        list.addAll(listOf(
            "Zabawki dla dzieci", "Pieluchy (jeśli potrzebne)",
            "Chusteczki nawilżane x2", "Krem z filtrem dla dzieci x1", "Apteczka dla dzieci x1"
        ))
    }
}

