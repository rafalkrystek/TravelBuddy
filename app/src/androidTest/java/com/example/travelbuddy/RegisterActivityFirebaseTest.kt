package com.example.travelbuddy

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.google.firebase.auth.FirebaseAuth
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
class RegisterActivityFirebaseTest {

    @get:Rule
    val activityRule = ActivityTestRule(RegisterActivity::class.java, false, false)

    private lateinit var auth: FirebaseAuth
    private val testEmailPrefix = "test_${System.currentTimeMillis()}"

    @Before
    fun setup() {
        auth = FirebaseAuth.getInstance()
        auth.signOut()
    }

    @After
    fun tearDown() {
        auth.currentUser?.delete()
        auth.signOut()
    }

    @Test
    fun testRegistrationFormFieldsAreVisible() {
        activityRule.launchActivity(null)

        onView(withId(R.id.firstNameEditText))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.lastNameEditText))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.emailEditText))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.passwordEditText))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.confirmPasswordEditText))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.termsCheckBox))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.registerButton))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testRegistrationWithValidData() {
        activityRule.launchActivity(null)

        val testEmail = "${testEmailPrefix}_valid@test.com"
        val testPassword = "testpass123"

        onView(withId(R.id.firstNameEditText))
            .perform(typeText("Test"), closeSoftKeyboard())
        
        onView(withId(R.id.lastNameEditText))
            .perform(typeText("User"), closeSoftKeyboard())
        
        onView(withId(R.id.emailEditText))
            .perform(typeText(testEmail), closeSoftKeyboard())
        
        onView(withId(R.id.passwordEditText))
            .perform(typeText(testPassword), closeSoftKeyboard())
        
        onView(withId(R.id.confirmPasswordEditText))
            .perform(typeText(testPassword), closeSoftKeyboard())
        
        onView(withId(R.id.termsCheckBox))
            .perform(click())
        
        onView(withId(R.id.registerButton))
            .perform(click())

        val latch = CountDownLatch(1)
        var registrationSuccess = false

        Thread {
            var attempts = 0
            while (attempts < 30 && auth.currentUser == null) {
                Thread.sleep(1000)
                attempts++
            }
            registrationSuccess = auth.currentUser != null && 
                auth.currentUser?.email == testEmail
            latch.countDown()
        }.start()

        val completed = latch.await(35, TimeUnit.SECONDS)
        
        assertTrue("Registration should complete within timeout", completed)
        assertTrue("User should be registered in Firebase", registrationSuccess)
        
        assertEquals("User email should match", testEmail, auth.currentUser?.email)
    }

    @Test
    fun testRegistrationWithInvalidEmail() {
        activityRule.launchActivity(null)

        val invalidEmail = "invalid-email"
        val testPassword = "testpass123"

        onView(withId(R.id.firstNameEditText))
            .perform(typeText("Test"), closeSoftKeyboard())
        
        onView(withId(R.id.lastNameEditText))
            .perform(typeText("User"), closeSoftKeyboard())
        
        onView(withId(R.id.emailEditText))
            .perform(typeText(invalidEmail), closeSoftKeyboard())
        
        onView(withId(R.id.passwordEditText))
            .perform(typeText(testPassword), closeSoftKeyboard())
        
        onView(withId(R.id.confirmPasswordEditText))
            .perform(typeText(testPassword), closeSoftKeyboard())
        
        onView(withId(R.id.termsCheckBox))
            .perform(click())
        
        onView(withId(R.id.registerButton))
            .perform(click())

        Thread.sleep(2000)

        assertNull("User should not be created with invalid email", auth.currentUser)
    }

    @Test
    fun testRegistrationWithMismatchedPasswords() {
        activityRule.launchActivity(null)

        val testEmail = "${testEmailPrefix}_mismatch@test.com"
        val testPassword = "testpass123"
        val differentPassword = "different123"

        onView(withId(R.id.firstNameEditText))
            .perform(typeText("Test"), closeSoftKeyboard())
        
        onView(withId(R.id.lastNameEditText))
            .perform(typeText("User"), closeSoftKeyboard())
        
        onView(withId(R.id.emailEditText))
            .perform(typeText(testEmail), closeSoftKeyboard())
        
        onView(withId(R.id.passwordEditText))
            .perform(typeText(testPassword), closeSoftKeyboard())
        
        onView(withId(R.id.confirmPasswordEditText))
            .perform(typeText(differentPassword), closeSoftKeyboard())
        
        onView(withId(R.id.termsCheckBox))
            .perform(click())
        
        onView(withId(R.id.registerButton))
            .perform(click())

        Thread.sleep(2000)

        assertNull("User should not be created with mismatched passwords", auth.currentUser)
    }

    @Test
    fun testRegistrationWithoutTermsAccepted() {
        activityRule.launchActivity(null)

        val testEmail = "${testEmailPrefix}_noterms@test.com"
        val testPassword = "testpass123"

        onView(withId(R.id.firstNameEditText))
            .perform(typeText("Test"), closeSoftKeyboard())
        
        onView(withId(R.id.lastNameEditText))
            .perform(typeText("User"), closeSoftKeyboard())
        
        onView(withId(R.id.emailEditText))
            .perform(typeText(testEmail), closeSoftKeyboard())
        
        onView(withId(R.id.passwordEditText))
            .perform(typeText(testPassword), closeSoftKeyboard())
        
        onView(withId(R.id.confirmPasswordEditText))
            .perform(typeText(testPassword), closeSoftKeyboard())
        
        
        onView(withId(R.id.registerButton))
            .perform(click())

        Thread.sleep(2000)

        assertNull("User should not be created without accepting terms", auth.currentUser)
    }

    @Test
    fun testRegistrationWithShortPassword() {
        activityRule.launchActivity(null)

        val testEmail = "${testEmailPrefix}_shortpass@test.com"
        val shortPassword = "12345" 

        onView(withId(R.id.firstNameEditText))
            .perform(typeText("Test"), closeSoftKeyboard())
        
        onView(withId(R.id.lastNameEditText))
            .perform(typeText("User"), closeSoftKeyboard())
        
        onView(withId(R.id.emailEditText))
            .perform(typeText(testEmail), closeSoftKeyboard())
        
        onView(withId(R.id.passwordEditText))
            .perform(typeText(shortPassword), closeSoftKeyboard())
        
        onView(withId(R.id.confirmPasswordEditText))
            .perform(typeText(shortPassword), closeSoftKeyboard())
        
        onView(withId(R.id.termsCheckBox))
            .perform(click())
        
        onView(withId(R.id.registerButton))
            .perform(click())

        Thread.sleep(2000)

        assertNull("User should not be created with password shorter than 6 characters", auth.currentUser)
    }
}

