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

/**
 * Testy integracyjne dla rejestracji Firebase
 * Uwaga: Te testy wymagają połączenia z Firebase i mogą tworzyć rzeczywiste konta użytkowników
 */
@RunWith(AndroidJUnit4::class)
class RegisterActivityFirebaseTest {

    @get:Rule
    val activityRule = ActivityTestRule(RegisterActivity::class.java, false, false)

    private lateinit var auth: FirebaseAuth
    private val testEmailPrefix = "test_${System.currentTimeMillis()}"

    @Before
    fun setup() {
        auth = FirebaseAuth.getInstance()
        // Sign out any existing user
        auth.signOut()
    }

    @After
    fun tearDown() {
        // Clean up: delete test user if created
        auth.currentUser?.delete()
        auth.signOut()
    }

    @Test
    fun testRegistrationFormFieldsAreVisible() {
        activityRule.launchActivity(null)

        // Check if all form fields are visible
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

        // Fill in the registration form
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
        
        // Click register button
        onView(withId(R.id.registerButton))
            .perform(click())

        // Wait for Firebase registration to complete
        val latch = CountDownLatch(1)
        var registrationSuccess = false

        // Check if user was created (with timeout)
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
        
        // Verify user email
        assertEquals("User email should match", testEmail, auth.currentUser?.email)
    }

    @Test
    fun testRegistrationWithInvalidEmail() {
        activityRule.launchActivity(null)

        val invalidEmail = "invalid-email"
        val testPassword = "testpass123"

        // Fill in the registration form with invalid email
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
        
        // Click register button
        onView(withId(R.id.registerButton))
            .perform(click())

        // Wait a bit to see if error is shown
        Thread.sleep(2000)

        // Check if error message is displayed on email field
        // Note: This depends on how the error is displayed in the UI
        // The validation should prevent Firebase call
        assertNull("User should not be created with invalid email", auth.currentUser)
    }

    @Test
    fun testRegistrationWithMismatchedPasswords() {
        activityRule.launchActivity(null)

        val testEmail = "${testEmailPrefix}_mismatch@test.com"
        val testPassword = "testpass123"
        val differentPassword = "different123"

        // Fill in the registration form with mismatched passwords
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
        
        // Click register button
        onView(withId(R.id.registerButton))
            .perform(click())

        // Wait a bit to see if error is shown
        Thread.sleep(2000)

        // Validation should prevent Firebase call
        assertNull("User should not be created with mismatched passwords", auth.currentUser)
    }

    @Test
    fun testRegistrationWithoutTermsAccepted() {
        activityRule.launchActivity(null)

        val testEmail = "${testEmailPrefix}_noterms@test.com"
        val testPassword = "testpass123"

        // Fill in the registration form without accepting terms
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
        
        // Don't check terms checkbox
        
        // Click register button
        onView(withId(R.id.registerButton))
            .perform(click())

        // Wait a bit to see if error is shown
        Thread.sleep(2000)

        // Validation should prevent Firebase call
        assertNull("User should not be created without accepting terms", auth.currentUser)
    }

    @Test
    fun testRegistrationWithShortPassword() {
        activityRule.launchActivity(null)

        val testEmail = "${testEmailPrefix}_shortpass@test.com"
        val shortPassword = "12345" // Less than 6 characters

        // Fill in the registration form with short password
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
        
        // Click register button
        onView(withId(R.id.registerButton))
            .perform(click())

        // Wait a bit to see if error is shown
        Thread.sleep(2000)

        // Validation should prevent Firebase call
        assertNull("User should not be created with password shorter than 6 characters", auth.currentUser)
    }
}

