package com.example.travelbuddy

import androidx.test.ext.junit.runners.AndroidJUnit4
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
 * Testy integracyjne dla logowania Firebase
 * Uwaga: Te testy wymagają połączenia z Firebase i mogą używać rzeczywistych kont użytkowników
 */
@RunWith(AndroidJUnit4::class)
class LoginActivityFirebaseTest {

    @get:Rule
    val activityRule = ActivityTestRule(LoginActivity::class.java, false, false)

    private lateinit var auth: FirebaseAuth
    private val testEmailPrefix = "test_login_${System.currentTimeMillis()}"

    @Before
    fun setup() {
        auth = FirebaseAuth.getInstance()
        // Sign out any existing user
        auth.signOut()
    }

    @After
    fun tearDown() {
        // Clean up: sign out after tests
        auth.signOut()
    }

    @Test
    fun testLoginFormFieldsAreVisible() {
        activityRule.launchActivity(null)

        // Check if all form fields are visible
        onView(withId(R.id.emailEditText))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.passwordEditText))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.signInButton))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testLoginWithEmptyEmailShowsError() {
        activityRule.launchActivity(null)

        // Try to login with empty email
        onView(withId(R.id.passwordEditText))
            .perform(typeText("password123"), closeSoftKeyboard())
        
        onView(withId(R.id.signInButton))
            .perform(click())

        // Wait a bit to see if error is shown
        Thread.sleep(1000)

        // Email field should have error
        onView(withId(R.id.emailEditText))
            .check(matches(hasErrorText("Email is required")))
    }

    @Test
    fun testLoginWithEmptyPasswordShowsError() {
        activityRule.launchActivity(null)

        // Try to login with empty password
        onView(withId(R.id.emailEditText))
            .perform(typeText("test@example.com"), closeSoftKeyboard())
        
        onView(withId(R.id.signInButton))
            .perform(click())

        // Wait a bit to see if error is shown
        Thread.sleep(1000)

        // Password field should have error
        onView(withId(R.id.passwordEditText))
            .check(matches(hasErrorText("Password is required")))
    }

    @Test
    fun testLoginWithInvalidEmailFormat() {
        activityRule.launchActivity(null)

        // Try to login with invalid email format
        onView(withId(R.id.emailEditText))
            .perform(typeText("invalid-email"), closeSoftKeyboard())
        
        onView(withId(R.id.passwordEditText))
            .perform(typeText("password123"), closeSoftKeyboard())
        
        onView(withId(R.id.signInButton))
            .perform(click())

        // Wait a bit to see if error is shown
        Thread.sleep(1000)

        // Should not navigate to dashboard with invalid email
        // Note: This depends on validation implementation
        assertNull("User should not be logged in with invalid email", auth.currentUser)
    }

    @Test
    fun testLoginWithValidCredentials() {
        // First, create a test user
        val testEmail = "${testEmailPrefix}_valid@test.com"
        val testPassword = "testpass123"

        val createLatch = CountDownLatch(1)
        auth.createUserWithEmailAndPassword(testEmail, testPassword)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    auth.signOut()
                }
                createLatch.countDown()
            }
        
        assertTrue("User creation should complete", createLatch.await(10, TimeUnit.SECONDS))

        // Now try to login
        activityRule.launchActivity(null)
        
        onView(withId(R.id.emailEditText))
            .perform(typeText(testEmail), closeSoftKeyboard())
        
        onView(withId(R.id.passwordEditText))
            .perform(typeText(testPassword), closeSoftKeyboard())
        
        onView(withId(R.id.signInButton))
            .perform(click())

        // Wait for Firebase authentication
        val loginLatch = CountDownLatch(1)
        var loginSuccess = false

        Thread {
            var attempts = 0
            while (attempts < 30 && auth.currentUser == null) {
                Thread.sleep(1000)
                attempts++
            }
            loginSuccess = auth.currentUser != null && auth.currentUser?.email == testEmail
            loginLatch.countDown()
        }.start()

        assertTrue("Login should complete within timeout", loginLatch.await(35, TimeUnit.SECONDS))
        assertTrue("User should be logged in with valid credentials", loginSuccess)
        assertEquals("User email should match", testEmail, auth.currentUser?.email)

        // Clean up: delete test user
        auth.currentUser?.delete()
    }

    @Test
    fun testLoginWithNonExistentUser() {
        activityRule.launchActivity(null)

        val nonExistentEmail = "${testEmailPrefix}_nonexistent@test.com"
        val password = "password123"

        // Try to login with non-existent user
        onView(withId(R.id.emailEditText))
            .perform(typeText(nonExistentEmail), closeSoftKeyboard())
        
        onView(withId(R.id.passwordEditText))
            .perform(typeText(password), closeSoftKeyboard())
        
        onView(withId(R.id.signInButton))
            .perform(click())

        // Wait for Firebase authentication attempt
        Thread.sleep(3000)

        // User should not be logged in
        assertNull("User should not be logged in with non-existent credentials", auth.currentUser)
    }

    @Test
    fun testLoginWithWrongPassword() {
        activityRule.launchActivity(null)

        // First, create a test user
        val testEmail = "${testEmailPrefix}_wrongpass@test.com"
        val correctPassword = "correctpass123"
        val wrongPassword = "wrongpass123"

        // Create user first
        val createLatch = CountDownLatch(1)
        auth.createUserWithEmailAndPassword(testEmail, correctPassword)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    auth.signOut()
                }
                createLatch.countDown()
            }
        
        assertTrue("User creation should complete", createLatch.await(10, TimeUnit.SECONDS))

        // Now try to login with wrong password
        activityRule.launchActivity(null)
        
        onView(withId(R.id.emailEditText))
            .perform(typeText(testEmail), closeSoftKeyboard())
        
        onView(withId(R.id.passwordEditText))
            .perform(typeText(wrongPassword), closeSoftKeyboard())
        
        onView(withId(R.id.signInButton))
            .perform(click())

        // Wait for Firebase authentication attempt
        Thread.sleep(3000)

        // User should not be logged in
        assertNull("User should not be logged in with wrong password", auth.currentUser)

        // Clean up: delete test user
        auth.signInWithEmailAndPassword(testEmail, correctPassword)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    auth.currentUser?.delete()
                }
            }
    }
}

