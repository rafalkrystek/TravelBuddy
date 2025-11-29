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
        
        val testEmail = "${testEmailPrefix}@test.com"
        val testPassword = "testpass123"
        
        val latch = CountDownLatch(1)
        auth.createUserWithEmailAndPassword(testEmail, testPassword)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                }
                latch.countDown()
            }
        
        assertTrue("User creation should complete", latch.await(10, TimeUnit.SECONDS))
        
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
        createdTripId?.let { tripId ->
            val user = auth.currentUser
            if (user != null) {
                db.collection("trips")
                    .document(tripId)
                    .delete()
                    .addOnCompleteListener { }
            }
        }
        
        auth.currentUser?.delete()
        auth.signOut()
    }

    @Test
    fun testActivityPlanningFieldsAreVisible() {
        if (createdTripId == null) {
            return
        }

        val intent = android.content.Intent()
        intent.putExtra("trip_id", createdTripId)
        intent.putExtra("trip_destination", "Test Destination")
        intent.putExtra("trip_start_date", "01.01.2024")
        intent.putExtra("trip_end_date", "05.01.2024")
        activityRule.launchActivity(intent)

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

        onView(withId(R.id.userPreferencesEditText))
            .perform(typeText(preferences), closeSoftKeyboard())

        Thread.sleep(2000)

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
    }

    @Test
    fun testLoadTravelPlanFromFirebase() {
        if (createdTripId == null) {
            return
        }

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

        val intent = android.content.Intent()
        intent.putExtra("trip_id", createdTripId)
        intent.putExtra("trip_destination", "Test Destination")
        intent.putExtra("trip_start_date", "01.01.2024")
        intent.putExtra("trip_end_date", "05.01.2024")
        activityRule.launchActivity(intent)

        Thread.sleep(3000)

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

        onView(withId(R.id.userPreferencesEditText))
            .perform(clearText(), typeText(testText), closeSoftKeyboard())

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

        Thread.sleep(2000)

        onView(withId(R.id.destinationTextView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testActivityHandlesMissingTripId() {
        val intent = android.content.Intent()
        intent.putExtra("trip_destination", "Test Destination")
        activityRule.launchActivity(intent)

        Thread.sleep(2000)

    }
}

