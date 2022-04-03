package com.android.android_github_api.util.matchers

import android.view.View
import androidx.test.espresso.PerformException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.util.HumanReadables
import androidx.test.espresso.util.TreeIterables
import org.hamcrest.Matcher
import java.util.concurrent.TimeoutException
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed

import androidx.test.espresso.NoMatchingViewException

import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.matcher.ViewMatchers
import java.lang.AssertionError


fun isNotCompletelyDisplayed(): ViewAssertion {
    return ViewAssertion { view, _ ->
        if (view != null && isCompletelyDisplayed().matches(view)) {
            throw AssertionError(
                "View is present in the hierarchy and Displayed Completely: "
                        + HumanReadables.describe(view)
            )
        }
    }
}


/**
 * Perform action of waiting for a specific view id to be displayed.
 * @param viewId The id of the view to wait for.
 * @param millis The timeout of until when to wait for.
 */
fun waitDisplayed(viewId: Int, millis: Long): ViewAction? {
    return object : ViewAction {
        override fun getConstraints(): Matcher<View> {
            return isRoot()
        }

        override fun getDescription(): String {
            return "wait for a specific view with id <$viewId> has been displayed during $millis millis."
        }

        override fun perform(uiController: UiController, view: View?) {
            uiController.loopMainThreadUntilIdle()
            val startTime = System.currentTimeMillis()
            val endTime = startTime + millis
            val matchId: Matcher<View> = withId(viewId)
            val matchDisplayed: Matcher<View> = isDisplayed()
            do {
                for (child in TreeIterables.breadthFirstViewTraversal(view)) {
                    if (matchId.matches(child) && matchDisplayed.matches(child)) {
                        return
                    }
                }
                uiController.loopMainThreadForAtLeast(50)
            } while (System.currentTimeMillis() < endTime)
            throw PerformException.Builder()
                .withActionDescription(this.description)
                .withViewDescription(HumanReadables.describe(view))
                .withCause(TimeoutException())
                .build()
        }
    }
}