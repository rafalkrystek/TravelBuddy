package com.example.travelbuddy

import org.junit.Test
import org.junit.Assert.*
import org.json.JSONArray
import org.json.JSONObject

class RestaurantFinderTest {

    @Test
    fun testParsingRestaurantJSONResponseWithValidData() {
        val jsonResponse = """
        {
            "status": "OK",
            "results": [
                {
                    "name": "Restauracja Testowa",
                    "rating": 4.5,
                    "geometry": {
                        "location": {
                            "lat": 52.2297,
                            "lng": 21.0122
                        }
                    },
                    "vicinity": "ul. Testowa 1, Warszawa",
                    "place_id": "test_place_id_1"
                },
                {
                    "name": "Dobra Restauracja",
                    "rating": 4.8,
                    "geometry": {
                        "location": {
                            "lat": 52.2300,
                            "lng": 21.0130
                        }
                    },
                    "vicinity": "ul. Dobra 2, Warszawa",
                    "place_id": "test_place_id_2"
                }
            ]
        }
        """.trimIndent()

        val json = JSONObject(jsonResponse)
        val status = json.getString("status")
        assertEquals("OK", status)

        val results = json.getJSONArray("results")
        assertEquals(2, results.length())

        val firstRestaurant = results.getJSONObject(0)
        assertEquals("Restauracja Testowa", firstRestaurant.getString("name"))
        assertEquals(4.5, firstRestaurant.getDouble("rating"), 0.01)
        assertEquals("test_place_id_1", firstRestaurant.getString("place_id"))
    }

    @Test
    fun testFilteringRestaurantsByRatingAtLeast4() {
        val restaurants = listOf(
            createTestRestaurant("Restauracja 1", 4.5),
            createTestRestaurant("Restauracja 2", 3.5),
            createTestRestaurant("Restauracja 3", 4.0),
            createTestRestaurant("Restauracja 4", 4.8),
            createTestRestaurant("Restauracja 5", 3.9),
            createTestRestaurant("Restauracja 6", 0.0) 
        )

        val filtered = restaurants.filter { it.rating == 0.0 || it.rating >= 4.0 }
        
        assertEquals(4, filtered.size)
        assertTrue(filtered.any { it.name == "Restauracja 1" && it.rating == 4.5 })
        assertTrue(filtered.any { it.name == "Restauracja 3" && it.rating == 4.0 })
        assertTrue(filtered.any { it.name == "Restauracja 4" && it.rating == 4.8 })
        assertTrue(filtered.any { it.name == "Restauracja 6" && it.rating == 0.0 })
        assertFalse(filtered.any { it.name == "Restauracja 2" })
        assertFalse(filtered.any { it.name == "Restauracja 5" })
    }

    @Test
    fun testFilteringRestaurantsExcludesRatingBelow4() {
        val restaurants = listOf(
            createTestRestaurant("Dobra", 4.1),
            createTestRestaurant("Słaba", 3.5),
            createTestRestaurant("Średnia", 3.9)
        )

        val filtered = restaurants.filter { it.rating == 0.0 || it.rating >= 4.0 }
        
        assertEquals(1, filtered.size)
        assertEquals("Dobra", filtered[0].name)
    }

    @Test
    fun testParsingRestaurantWithMissingRatingIncludesIt() {
        val jsonResponse = """
        {
            "status": "OK",
            "results": [
                {
                    "name": "Nowa Restauracja",
                    "geometry": {
                        "location": {
                            "lat": 52.2297,
                            "lng": 21.0122
                        }
                    },
                    "vicinity": "ul. Nowa 1",
                    "place_id": "new_place_id"
                }
            ]
        }
        """.trimIndent()

        val json = JSONObject(jsonResponse)
        val results = json.getJSONArray("results")
        val restaurant = results.getJSONObject(0)
        
        val rating = restaurant.optDouble("rating", 0.0)
        assertEquals(0.0, rating, 0.01)
        assertTrue(rating == 0.0 || rating >= 4.0)
    }

    @Test
    fun testRadiusOptionsMapping() {
        val radiusOptions = listOf(
            "500 m" to 500,
            "1 km" to 1000,
            "2 km" to 2000,
            "5 km" to 5000,
            "10 km" to 10000,
            "25 km" to 25000
        )

        assertEquals(500, radiusOptions.find { it.first == "500 m" }?.second)
        assertEquals(1000, radiusOptions.find { it.first == "1 km" }?.second)
        assertEquals(5000, radiusOptions.find { it.first == "5 km" }?.second)
        assertEquals(25000, radiusOptions.find { it.first == "25 km" }?.second)
    }

    @Test
    fun testParsingRestaurantWithFormattedAddress() {
        val jsonResponse = """
        {
            "status": "OK",
            "results": [
                {
                    "name": "Restauracja",
                    "rating": 4.5,
                    "geometry": {
                        "location": {
                            "lat": 52.2297,
                            "lng": 21.0122
                        }
                    },
                    "formatted_address": "ul. Długa 10, 00-001 Warszawa, Polska",
                    "place_id": "test_id"
                }
            ]
        }
        """.trimIndent()

        val json = JSONObject(jsonResponse)
        val results = json.getJSONArray("results")
        val restaurant = results.getJSONObject(0)
        
        val address = restaurant.optString("formatted_address", restaurant.optString("vicinity", "Brak adresu"))
        assertEquals("ul. Długa 10, 00-001 Warszawa, Polska", address)
    }

    @Test
    fun testParsingRestaurantWithVicinityWhenFormattedAddressMissing() {
        val jsonResponse = """
        {
            "status": "OK",
            "results": [
                {
                    "name": "Restauracja",
                    "rating": 4.5,
                    "geometry": {
                        "location": {
                            "lat": 52.2297,
                            "lng": 21.0122
                        }
                    },
                    "vicinity": "ul. Krótka 5, Warszawa",
                    "place_id": "test_id"
                }
            ]
        }
        """.trimIndent()

        val json = JSONObject(jsonResponse)
        val results = json.getJSONArray("results")
        val restaurant = results.getJSONObject(0)
        
        val address = restaurant.optString("formatted_address", restaurant.optString("vicinity", "Brak adresu"))
        assertEquals("ul. Krótka 5, Warszawa", address)
    }

    @Test
    fun testParsingRestaurantWithNoAddress() {
        val jsonResponse = """
        {
            "status": "OK",
            "results": [
                {
                    "name": "Restauracja",
                    "rating": 4.5,
                    "geometry": {
                        "location": {
                            "lat": 52.2297,
                            "lng": 21.0122
                        }
                    },
                    "place_id": "test_id"
                }
            ]
        }
        """.trimIndent()

        val json = JSONObject(jsonResponse)
        val results = json.getJSONArray("results")
        val restaurant = results.getJSONObject(0)
        
        val address = restaurant.optString("formatted_address", restaurant.optString("vicinity", "Brak adresu"))
        assertEquals("Brak adresu", address)
    }

    @Test
    fun testAPIErrorStatusRequestDenied() {
        val jsonResponse = """
        {
            "status": "REQUEST_DENIED",
            "error_message": "This API project is not authorized to use this API."
        }
        """.trimIndent()

        val json = JSONObject(jsonResponse)
        val status = json.getString("status")
        assertEquals("REQUEST_DENIED", status)
        
        val errorMessage = json.optString("error_message", "Unknown error")
        assertTrue(errorMessage.contains("not authorized"))
    }

    @Test
    fun testAPIErrorStatusZeroResults() {
        val jsonResponse = """
        {
            "status": "ZERO_RESULTS"
        }
        """.trimIndent()

        val json = JSONObject(jsonResponse)
        val status = json.getString("status")
        assertEquals("ZERO_RESULTS", status)
        
        assertTrue(status == "ZERO_RESULTS")
    }

    @Test
    fun testURLEncodingForCuisineTypeWithSpecialCharacters() {
        val cuisineType = "Włoska"
        val query = "$cuisineType restaurant"
        
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        assertTrue(encoded.contains("W%C5%82oska") || encoded.contains("Włoska"))
    }

    @Test
    fun testRestaurantDataStructure() {
        val restaurant = RestaurantFinderActivity.Restaurant(
            name = "Test Restaurant",
            address = "Test Address",
            rating = 4.5,
            lat = 52.2297,
            lng = 21.0122,
            placeId = "test_id_123"
        )

        assertEquals("Test Restaurant", restaurant.name)
        assertEquals("Test Address", restaurant.address)
        assertEquals(4.5, restaurant.rating, 0.01)
        assertEquals(52.2297, restaurant.lat, 0.0001)
        assertEquals(21.0122, restaurant.lng, 0.0001)
        assertEquals("test_id_123", restaurant.placeId)
    }

    @Test
    fun testFilteringMultipleRestaurantsWithVariousRatings() {
        val restaurants = listOf(
            createTestRestaurant("Excellent", 4.9),
            createTestRestaurant("Very Good", 4.5),
            createTestRestaurant("Good", 4.0),
            createTestRestaurant("Average", 3.9),
            createTestRestaurant("Poor", 2.5),
            createTestRestaurant("New", 0.0)
        )

        val filtered = restaurants.filter { it.rating == 0.0 || it.rating >= 4.0 }
        
        assertEquals(4, filtered.size)
        val names = filtered.map { it.name }
        assertTrue(names.contains("Excellent"))
        assertTrue(names.contains("Very Good"))
        assertTrue(names.contains("Good"))
        assertTrue(names.contains("New"))
        assertFalse(names.contains("Average"))
        assertFalse(names.contains("Poor"))
    }

    @Test
    fun testParsingEmptyResultsArray() {
        val jsonResponse = """
        {
            "status": "OK",
            "results": []
        }
        """.trimIndent()

        val json = JSONObject(jsonResponse)
        val status = json.getString("status")
        assertEquals("OK", status)

        val results = json.getJSONArray("results")
        assertEquals(0, results.length())
    }

    @Test
    fun testCuisineTypeValidation() {
        val validCuisineTypes = listOf(
            "Włoska", "Francuska", "Hiszpańska", "Grecka", "Turecka",
            "Chińska", "Japońska", "Tajska", "Indyjska", "Meksykańska",
            "Amerykańska", "Polska", "Wegetariańska", "Wegańska", "Sushi",
            "Pizza", "Fast food", "Seafood", "Steakhouse", "Kawiarnia"
        )

        assertTrue(validCuisineTypes.contains("Sushi"))
        assertTrue(validCuisineTypes.contains("Włoska"))
        assertTrue(validCuisineTypes.contains("Chińska"))
        assertFalse(validCuisineTypes.contains(""))
        assertFalse(validCuisineTypes.contains("Nieistniejąca"))
    }

    private fun createTestRestaurant(name: String, rating: Double): RestaurantFinderActivity.Restaurant {
        return RestaurantFinderActivity.Restaurant(
            name = name,
            address = "Test Address",
            rating = rating,
            lat = 52.2297,
            lng = 21.0122,
            placeId = "test_$name"
        )
    }
}

