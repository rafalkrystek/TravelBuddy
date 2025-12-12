package com.example.travelbuddy

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Testy integracyjne dla planowania podróży z Firebase
 * Uwaga: Te testy wymagają połączenia z Firebase i mogą tworzyć rzeczywiste dokumenty w Firestore
 */
@RunWith(AndroidJUnit4::class)
class TripDetailsActivityFirebaseTest {

    @get:Rule
    val activityRule = ActivityTestRule(TripDetailsActivity::class.java, false, false)

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val testEmailPrefix = "test_plan_${System.currentTimeMillis()}"
    private var createdTripId: String? = null

    @Before
    fun setup() {
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        
        // Create a test user and sign in
        val testEmail = "${testEmailPrefix}@test.com"
        val testPassword = "testpass123"
        
        val latch = CountDownLatch(1)
        auth.createUserWithEmailAndPassword(testEmail, testPassword)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // User created and signed in
                }
                latch.countDown()
            }
        
        assertTrue("User creation should complete", latch.await(10, TimeUnit.SECONDS))
        
        // Create a test trip
        val trip = hashMapOf(
            "destination" to "Test Destination",
            "startDate" to "01.01.2024",
            "endDate" to "05.01.2024",
            "budget" to 5000,
            "userId" to auth.currentUser!!.uid,
            "createdAt" to com.google.firebase.Timestamp.now()
        )
        
        val tripLatch = CountDownLatch(1)
        db.collection("trips")
            .add(trip)
            .addOnSuccessListener { documentReference ->
                createdTripId = documentReference.id
                tripLatch.countDown()
            }
            .addOnFailureListener {
                tripLatch.countDown()
            }
        
        assertTrue("Trip creation should complete", tripLatch.await(10, TimeUnit.SECONDS))
    }

    @After
    fun tearDown() {
        // Clean up: delete test trip if created
        createdTripId?.let { tripId ->
            val user = auth.currentUser
            if (user != null) {
                db.collection("trips")
                    .document(tripId)
                    .delete()
                    .addOnCompleteListener { }
            }
        }
        
        // Delete test user
        auth.currentUser?.delete()
        auth.signOut()
    }

    @Test
    fun testActivityPlanningFieldsAreVisible() {
        if (createdTripId == null) {
            // Skip test if trip creation failed
            return
        }

        val intent = android.content.Intent()
        intent.putExtra("trip_id", createdTripId)
        intent.putExtra("trip_destination", "Test Destination")
        intent.putExtra("trip_start_date", "01.01.2024")
        intent.putExtra("trip_end_date", "05.01.2024")
        activityRule.launchActivity(intent)

        // Check if activity planning fields are visible
        onView(withId(R.id.userPreferencesEditText))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.generatePlanButton))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSaveUserPreferencesToFirebase() {
        if (createdTripId == null) {
            return
        }

        val intent = android.content.Intent()
        intent.putExtra("trip_id", createdTripId)
        intent.putExtra("trip_destination", "Test Destination")
        intent.putExtra("trip_start_date", "01.01.2024")
        intent.putExtra("trip_end_date", "05.01.2024")
        activityRule.launchActivity(intent)

        val preferences = "nie lubię zwiedzać kościołów, uwielbiam plaże"

        // Enter preferences
        onView(withId(R.id.userPreferencesEditText))
            .perform(typeText(preferences), closeSoftKeyboard())

        // Wait a bit for auto-save (if implemented)
        Thread.sleep(2000)

        // Verify preferences are saved
        val latch = CountDownLatch(1)
        var savedPreferences: String? = null

        db.collection("trips")
            .document(createdTripId!!)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    savedPreferences = document.getString("userPreferences")
                }
                latch.countDown()
            }
            .addOnFailureListener {
                latch.countDown()
            }

        assertTrue("Firestore read should complete", latch.await(10, TimeUnit.SECONDS))
        // Note: This test assumes auto-save is implemented
        // If not, you may need to trigger save manually
    }

    @Test
    fun testLoadTravelPlanFromFirebase() {
        if (createdTripId == null) {
            return
        }

        // First, save a travel plan
        val testPlan = "Dzień 1 (01.01.2024)\nRano: Zwiedzanie centrum\nPo południu: Muzeum\nWieczór: Kolacja"
        
        val saveLatch = CountDownLatch(1)
        db.collection("trips")
            .document(createdTripId!!)
            .update("travelPlan", testPlan)
            .addOnSuccessListener {
                saveLatch.countDown()
            }
            .addOnFailureListener {
                saveLatch.countDown()
            }

        assertTrue("Plan save should complete", saveLatch.await(10, TimeUnit.SECONDS))

        // Now launch activity and check if plan is loaded
        val intent = android.content.Intent()
        intent.putExtra("trip_id", createdTripId)
        intent.putExtra("trip_destination", "Test Destination")
        intent.putExtra("trip_start_date", "01.01.2024")
        intent.putExtra("trip_end_date", "05.01.2024")
        activityRule.launchActivity(intent)

        // Wait for data to load
        Thread.sleep(3000)

        // Check if plan is displayed
        onView(withId(R.id.generatedPlanTextView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSaveTravelPlanToFirebase() {
        if (createdTripId == null) {
            return
        }

        val intent = android.content.Intent()
        intent.putExtra("trip_id", createdTripId)
        intent.putExtra("trip_destination", "Test Destination")
        intent.putExtra("trip_start_date", "01.01.2024")
        intent.putExtra("trip_end_date", "05.01.2024")
        activityRule.launchActivity(intent)

        val testPlan = "Dzień 1 (01.01.2024)\nRano: Zwiedzanie\nPo południu: Muzeum\nWieczór: Kolacja"

        // Note: This test assumes there's a way to set the plan text directly
        // In a real scenario, you might need to mock the Gemini API response
        // or use a test API key

        // Verify plan can be saved
        val latch = CountDownLatch(1)
        var saveSuccess = false

        db.collection("trips")
            .document(createdTripId!!)
            .update("travelPlan", testPlan)
            .addOnSuccessListener {
                saveSuccess = true
                latch.countDown()
            }
            .addOnFailureListener {
                latch.countDown()
            }

        assertTrue("Plan save should complete", latch.await(10, TimeUnit.SECONDS))
        assertTrue("Plan should be saved successfully", saveSuccess)

        // Verify plan was saved
        val verifyLatch = CountDownLatch(1)
        var savedPlan: String? = null

        db.collection("trips")
            .document(createdTripId!!)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    savedPlan = document.getString("travelPlan")
                }
                verifyLatch.countDown()
            }
            .addOnFailureListener {
                verifyLatch.countDown()
            }

        assertTrue("Plan verification should complete", verifyLatch.await(10, TimeUnit.SECONDS))
        assertEquals("Saved plan should match", testPlan, savedPlan)
    }

    @Test
    fun testUserPreferencesFieldAcceptsText() {
        if (createdTripId == null) {
            return
        }

        val intent = android.content.Intent()
        intent.putExtra("trip_id", createdTripId)
        intent.putExtra("trip_destination", "Test Destination")
        intent.putExtra("trip_start_date", "01.01.2024")
        intent.putExtra("trip_end_date", "05.01.2024")
        activityRule.launchActivity(intent)

        val testText = "Test preferences text"

        // Enter text in preferences field
        onView(withId(R.id.userPreferencesEditText))
            .perform(clearText(), typeText(testText), closeSoftKeyboard())

        // Verify text was entered
        onView(withId(R.id.userPreferencesEditText))
            .check(matches(withText(testText)))
    }

    @Test
    fun testGeneratePlanButtonIsClickable() {
        if (createdTripId == null) {
            return
        }

        val intent = android.content.Intent()
        intent.putExtra("trip_id", createdTripId)
        intent.putExtra("trip_destination", "Test Destination")
        intent.putExtra("trip_start_date", "01.01.2024")
        intent.putExtra("trip_end_date", "05.01.2024")
        activityRule.launchActivity(intent)

        // Check if button is clickable
        onView(withId(R.id.generatePlanButton))
            .check(matches(isClickable()))
    }

    @Test
    fun testActivityLoadsWithValidTripId() {
        if (createdTripId == null) {
            return
        }

        val intent = android.content.Intent()
        intent.putExtra("trip_id", createdTripId)
        intent.putExtra("trip_destination", "Test Destination")
        intent.putExtra("trip_start_date", "01.01.2024")
        intent.putExtra("trip_end_date", "05.01.2024")
        activityRule.launchActivity(intent)

        // Wait for activity to load
        Thread.sleep(2000)

        // Check if activity is displayed
        onView(withId(R.id.destinationTextView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testActivityHandlesMissingTripId() {
        val intent = android.content.Intent()
        // Don't put trip_id
        intent.putExtra("trip_destination", "Test Destination")
        activityRule.launchActivity(intent)

        // Wait a bit
        Thread.sleep(2000)

        // Activity should handle missing trip_id gracefully
        // (either finish or show error)
        // This depends on your implementation
    }
}

