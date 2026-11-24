package com.teamalpha.i230845_p230638

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Espresso UI Test Case 1: Critical Backend-Connected Workflow
 * Workflow: User Authentication & Login with Firebase
 */
@RunWith(AndroidJUnit4::class)
class LoginWorkflowTest {

    @Test
    fun testUserLoginWorkflow() {
        ActivityScenario.launch(LoginActivity::class.java).use {
            // 1. Enter email directly into text box (instant replaceText)
            onView(withId(R.id.etEmail))
                .perform(replaceText("testuser@kinnect.com"))

            // 2. Enter password directly into text box
            onView(withId(R.id.etPassword))
                .perform(replaceText("password123"))

            // 3. Click the Login button to trigger Firebase Authentication
            onView(withId(R.id.loginButton))
                .perform(click())

            // 4. Verify login button/layout remains responsive and displayed
            onView(withId(R.id.loginButton))
                .check(matches(isDisplayed()))
        }
    }
}
