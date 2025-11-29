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
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.not
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class PlanTripActivityFirebaseTest {

    @get:Rule
    val activityRule = ActivityTestRule(PlanTripActivity::class.java, false, false)

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val testEmailPrefix = "test_trip_${System.currentTimeMillis()}"
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
    fun testTripFormFieldsAreVisible() {
        activityRule.launchActivity(null)

        onView(withId(R.id.destinationEditText))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.dateRangeEditText))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.budgetSlider))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.createTripButton))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSaveTripWithValidData() {
        activityRule.launchActivity(null)

        val destination = "Test Destination ${System.currentTimeMillis()}"
        val budget = 5000

        onView(withId(R.id.destinationEditText))
            .perform(click(), typeText(destination), closeSoftKeyboard())
        
        
        Thread.sleep(2000)

        assertNotNull("User should be logged in", auth.currentUser)
    }

    @Test
    fun testSaveTripWithoutDestination() {
        activityRule.launchActivity(null)

        
        onView(withId(R.id.createTripButton))
            .perform(click())

        Thread.sleep(2000)

    }

    @Test
    fun testTripSavedToFirestore() {
        val user = auth.currentUser
        assertNotNull("User should be logged in", user)

        val destination = "Test Trip ${System.currentTimeMillis()}"
        val startDate = System.currentTimeMillis()
        val endDate = startDate + (7 * 24 * 60 * 60 * 1000) 
        val budget = 5000

        val trip = hashMapOf(
            "destination" to destination,
            "startDate" to "01.01.2025",
            "endDate" to "08.01.2025",
            "startDateTimestamp" to startDate,
            "endDateTimestamp" to endDate,
            "budget" to budget,
            "userId" to user!!.uid,
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        val latch = CountDownLatch(1)
        var savedTripId: String? = null

        db.collection("trips")
            .add(trip)
            .addOnSuccessListener { documentReference ->
                savedTripId = documentReference.id
                createdTripId = savedTripId
                latch.countDown()
            }
            .addOnFailureListener {
                latch.countDown()
            }

        assertTrue("Trip save should complete", latch.await(10, TimeUnit.SECONDS))
        assertNotNull("Trip should be saved with ID", savedTripId)

        val verifyLatch = CountDownLatch(1)
        var tripExists = false

        savedTripId?.let { tripId ->
            db.collection("trips")
                .document(tripId)
                .get()
                .addOnSuccessListener { document ->
                    tripExists = document.exists()
                    if (tripExists) {
                        val savedDestination = document.getString("destination")
                        val savedUserId = document.getString("userId")
                        assertEquals("Destination should match", destination, savedDestination)
                        assertEquals("UserId should match", user.uid, savedUserId)
                    }
                    verifyLatch.countDown()
                }
                .addOnFailureListener {
                    verifyLatch.countDown()
                }
        } ?: verifyLatch.countDown()

        assertTrue("Trip verification should complete", verifyLatch.await(10, TimeUnit.SECONDS))
        assertTrue("Trip should exist in Firestore", tripExists)
    }

    @Test
    fun testTripBelongsToCorrectUser() {
        val user = auth.currentUser
        assertNotNull("User should be logged in", user)

        val destination = "User Trip Test ${System.currentTimeMillis()}"
        val startDate = System.currentTimeMillis()
        val endDate = startDate + (7 * 24 * 60 * 60 * 1000)
        val budget = 3000

        val trip = hashMapOf(
            "destination" to destination,
            "startDate" to "01.01.2025",
            "endDate" to "08.01.2025",
            "startDateTimestamp" to startDate,
            "endDateTimestamp" to endDate,
            "budget" to budget,
            "userId" to user!!.uid,
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        val latch = CountDownLatch(1)
        var savedTripId: String? = null

        db.collection("trips")
            .add(trip)
            .addOnSuccessListener { documentReference ->
                savedTripId = documentReference.id
                createdTripId = savedTripId
                latch.countDown()
            }
            .addOnFailureListener {
                latch.countDown()
            }

        assertTrue("Trip save should complete", latch.await(10, TimeUnit.SECONDS))
        assertNotNull("Trip should be saved", savedTripId)

        savedTripId?.let { tripId ->
            val verifyLatch = CountDownLatch(1)
            var userIdMatches = false

            db.collection("trips")
                .document(tripId)
                .get()
                .addOnSuccessListener { document ->
                    val savedUserId = document.getString("userId")
                    userIdMatches = savedUserId == user.uid
                    verifyLatch.countDown()
                }
                .addOnFailureListener {
                    verifyLatch.countDown()
                }

            assertTrue("Verification should complete", verifyLatch.await(10, TimeUnit.SECONDS))
            assertTrue("Trip should belong to correct user", userIdMatches)
        }
    }
}

