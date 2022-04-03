package com.android.android_github_api.ui.main.view

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeDown
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.MediumTest
import com.android.android_github_api.R
import com.android.android_github_api.data.model.github.GithubUser
import com.android.android_github_api.launchFragmentInHiltContainer
import com.android.android_github_api.ui.main.TestCustomFragmentFactory
import com.android.android_github_api.ui.main.viewmodel.HomeViewModel
import com.android.android_github_api.util.ViewModelUtil
import com.android.android_github_api.util.matchers.SwipeRefreshLayoutMatchers
import com.android.android_github_api.util.matchers.ToastMatcher.Companion.onToast
import com.android.android_github_api.utils.Event
import com.android.android_github_api.utils.Resource
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import javax.inject.Inject


@MediumTest
@HiltAndroidTest
@ExperimentalCoroutinesApi
class HomeFragmentTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Inject
    lateinit var testFragmentFactory: TestCustomFragmentFactory

    private lateinit var mockViewModel: HomeViewModel

    private lateinit var mockNavController: NavController

    private var githubUserData = MutableLiveData<Event<Resource<GithubUser>>>()
    private var isRefreshing = MutableLiveData<Boolean>()
    private fun setIsRefreshing(value: Boolean) {
        isRefreshing.postValue(value)
    }

    @Before
    fun setup() {
        hiltRule.inject()
        mockNavController = Mockito.mock(NavController::class.java)
        mockViewModel = Mockito.mock(HomeViewModel::class.java)
        Mockito.`when`(mockViewModel.getGithubUser).thenReturn(githubUserData)
        Mockito.`when`(mockViewModel.isRefreshing).thenReturn(isRefreshing)
        testFragmentFactory.viewModelFactory = ViewModelUtil.createFor(mockViewModel)
        launchFragmentInHiltContainer<HomeFragment>(
            fragmentFactory = testFragmentFactory
        ) {
            Navigation.setViewNavController(requireView(), mockNavController)
        }
    }

    @After
    fun teardown() {
        githubUserData = MutableLiveData<Event<Resource<GithubUser>>>()
        setIsRefreshing(false)
    }

    private fun loadedGithubUserScenario(githubUser: GithubUser) {
        githubUserData.postValue(Event(Resource.success(data = githubUser)))
    }

    private fun errorGithubUserScenarioWithPreviousData(githubUser: GithubUser) {
        githubUserData.postValue(Event(Resource.error(data = githubUser, message = "error")))
    }

    private fun initialErrorGithubUserScenario() {
        githubUserData.postValue(Event(Resource.error(data = null, message = "error")))
    }

    private fun loadingGithubUserScenarioWithPreviousData(githubUser: GithubUser) {
        githubUserData.postValue(Event(Resource.loading(data = githubUser)))
    }

    private fun initialLoadingGithubUserScenario() {
        githubUserData.postValue(Event(Resource.loading(data = null)))
    }

    @Test
    fun viewModel_setsConcreteGithubNameAndThenCallsLoadGithubUser() {
        val inOrder = Mockito.inOrder(mockViewModel)

        inOrder.verify(mockViewModel).setName("GdonatasG")
        inOrder.verify(mockViewModel).loadGithubUser()
    }

    @Test
    fun githubUserObserver_initialLoadingGithubUserScenario_visibleOnlyLoadingLayout() {
        initialLoadingGithubUserScenario()
        onView(withId(R.id.ll_loading))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        onView(withId(R.id.sr_loaded))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.ll_main_error))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun githubUserObserver_loadedGithubUserScenario_visibleOnlyLoadedLayout() {
        loadedGithubUserScenario(GithubUser("", "", "", 1))
        onView(withId(R.id.ll_loading))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.sr_loaded))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        onView(withId(R.id.ll_main_error))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun githubUserObserver_loadedGithubUserScenario_changesReposButtonText() {
        val user = GithubUser("", "", "", 5)
        loadedGithubUserScenario(user)
        onView(withId(R.id.btn_search_repos))
            .check(matches(withText("View repos (${user.public_repos_count})")))
    }

    @Test
    fun githubUserObserver_initialErrorGithubUserScenario_visibleOnlyInitialErrorLayout() {
        initialErrorGithubUserScenario()
        onView(withId(R.id.ll_loading))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.sr_loaded))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.ll_main_error))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
    }

    @Test
    fun githubUserObserver_initialErrorGithubUserScenario_clickMainRetryButton_callsLoadGithubUser() {
        // to reset previous viewModel calls that occurred on fragment creation (such as initial user loading)
        Mockito.clearInvocations(mockViewModel)

        initialErrorGithubUserScenario()
        onView(withId(R.id.btn_main_retry)).perform(click())

        Mockito.verify(mockViewModel).loadGithubUser()
    }

    @Test
    fun githubUserObserver_loadingGithubUserScenarioWithPreviousData_stillVisibleOnlyLoadedLayout() {
        loadingGithubUserScenarioWithPreviousData(githubUser = GithubUser("", "", "", 1))
        onView(withId(R.id.ll_loading))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.sr_loaded))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        onView(withId(R.id.ll_main_error))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun githubUserObserver_errorGithubUserScenarioWithPreviousData_stillVisibleOnlyLoadedLayout() {
        errorGithubUserScenarioWithPreviousData(githubUser = GithubUser("", "", "", 1))
        onView(withId(R.id.ll_loading))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.sr_loaded))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        onView(withId(R.id.ll_main_error))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun clickSearchReposButton_navigatesToSearchFragmentWithConcreteGithubNameFromViewModel() {
        loadedGithubUserScenario(GithubUser("", "", "", 1))
        Mockito.`when`(mockViewModel.getName).thenReturn("test")
        onView(withId(R.id.btn_search_repos)).perform(click())


        Mockito.verify(mockNavController).navigate(
            HomeFragmentDirections.actionHomeFragmentToSearchFragment(
                githubName = "test"
            )
        )
    }

    @Test
    fun clickProfileButton_navigatesToWebViewFragmentWithConcreteUrlAndTitle() {
        loadedGithubUserScenario(GithubUser("test", "", "", 1))
        Mockito.`when`(mockViewModel.getName).thenReturn("test")
        onView(withId(R.id.btn_profile)).perform(click())

        Mockito.verify(mockNavController).navigate(
            HomeFragmentDirections.actionHomeFragmentToWebViewFragment(
                url = "https://github.com/test/",
                title = "test profile"
            )
        )

    }

    @Test
    fun isRefreshingObserver_whenTrue_makesSwipeRefreshLayoutIsRefreshing() {
        loadedGithubUserScenario(GithubUser("test", "", "", 1))
        onView(withId(R.id.sr_loaded)).check(matches(SwipeRefreshLayoutMatchers.isNotRefreshing()))
        setIsRefreshing(true)
        onView(withId(R.id.sr_loaded)).check(matches(SwipeRefreshLayoutMatchers.isRefreshing()))
    }

    @Test
    fun swipeRefresh_callsLoadGithubUserWithIsRefreshTrue() {
        Mockito.clearInvocations(mockViewModel)

        loadedGithubUserScenario(GithubUser("test", "", "", 1))
        onView(withId(R.id.sr_loaded)).perform(swipeDown())

        Mockito.verify(mockViewModel).loadGithubUser(isRefresh = true)
    }

    @Test
    fun githubUserObserver_errorGithubUserScenarioWithPreviousData_turnsOffSwipeRefreshLayoutRefreshing() {
        val user = GithubUser("test", "", "", 1)
        loadedGithubUserScenario(user)
        onView(withId(R.id.sr_loaded)).perform(swipeDown())
        errorGithubUserScenarioWithPreviousData(user)
        onView(withId(R.id.sr_loaded)).check(matches(SwipeRefreshLayoutMatchers.isNotRefreshing()))
    }

    @Test
    fun githubUserObserver_errorGithubUserScenarioWithPreviousData_showsToastMessageWhenSwipeRefreshIsRefreshing() {
        loadedGithubUserScenario(GithubUser("test", "", "", 1))
        onView(withId(R.id.sr_loaded)).perform(swipeDown())
        errorGithubUserScenarioWithPreviousData(githubUser = GithubUser("", "", "", 1))
        onToast("error").check(matches(isDisplayed()))
    }

}