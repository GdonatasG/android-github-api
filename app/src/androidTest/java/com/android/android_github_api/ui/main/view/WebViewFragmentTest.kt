package com.android.android_github_api.ui.main.view

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.web.assertion.WebViewAssertions.webMatches
import androidx.test.espresso.web.model.Atoms.getCurrentUrl
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.filters.MediumTest
import com.android.android_github_api.R
import com.android.android_github_api.launchFragmentInHiltContainer
import com.android.android_github_api.ui.main.TestCustomFragmentFactory
import com.google.common.truth.Truth
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import javax.inject.Inject


@MediumTest
@HiltAndroidTest
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class WebViewFragmentTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Inject
    lateinit var testFragmentFactory: TestCustomFragmentFactory

    private lateinit var mockNavController: NavController

    private lateinit var navHostController: TestNavHostController

    @Before
    fun setup() {
        hiltRule.inject()
        mockNavController = Mockito.mock(NavController::class.java)
        navHostController = TestNavHostController(ApplicationProvider.getApplicationContext())
        navHostController.setGraph(R.navigation.nav_graph)
        navHostController.setCurrentDestination(R.id.webViewFragment)
    }

    private fun launch(
        url: String,
        title: String? = null,
        navHostController: TestNavHostController? = null,
    ) {
        launchFragmentInHiltContainer<WebViewFragment>(
            fragmentArgs = bundleOf(Pair("url", url), Pair("title", title)),
            fragmentFactory = testFragmentFactory,
            navHostController = navHostController
        ) {
            if (navHostController == null) {
                Navigation.setViewNavController(this.requireView(), mockNavController)
            }
        }
    }


    @Test
    fun pressBackButton_popBackStack() {
        launch(url = "url")

        pressBack()

        verify(mockNavController).popBackStack()
    }

    @Test
    fun passedTitleInFragmentArguments_setsLabelOfCurrentDestination() {
        launch(url = "url", title = "title", navHostController)
        Truth.assertThat(navHostController.currentDestination?.label).isEqualTo("title")
    }

    @Test
    fun noTitleInFragmentArguments_labelOfCurrentDestinationIsEmpty() {
        launch(url = "url", navHostController = navHostController)
        Truth.assertThat(navHostController.currentDestination?.label).isEqualTo("")
    }

    @Test
    fun webView_correctUrlFromFragmentArgs() {
        val testUrl = "https://www.google.com/"

        launch(url = testUrl)
        onWebView(withId(R.id.web_view)).forceJavascriptEnabled()
            .check(webMatches(getCurrentUrl(), Matchers.`is`(testUrl)))
    }


}