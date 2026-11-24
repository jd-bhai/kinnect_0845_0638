package com.teamalpha.i230845_p230638

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Espresso UI Test Case 2: Critical Backend-Connected Workflow
 * Workflow: Real-time Chat Messaging via Firebase Realtime Database
 */
@RunWith(AndroidJUnit4::class)
class ChatWorkflowTest {

    @Test
    fun testSendMessageWorkflow() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, ChatActivity::class.java).apply {
            putExtra("RECEIVER_UID", "test_receiver_uid")
            putExtra("RECEIVER_NAME", "Test Receiver")
        }

        ActivityScenario.launch<ChatActivity>(intent).use {
            // 1. Type message text in chat input box
            onView(withId(R.id.etMessage))
                .perform(replaceText("Hello from Espresso Test!"))

            // 2. Click Send button to write message to Firebase Realtime Database
            onView(withId(R.id.btnSend))
                .perform(click())

            // 3. Verify message input box is cleared after sending
            onView(withId(R.id.etMessage))
                .check(matches(withText("")))

            // 4. Verify message scroll container is displayed
            onView(withId(R.id.scrollViewChat))
                .check(matches(isDisplayed()))
        }
    }
}
