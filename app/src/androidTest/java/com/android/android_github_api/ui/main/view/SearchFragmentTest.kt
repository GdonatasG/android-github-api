package com.android.android_github_api.ui.main.view

import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.MediumTest
import com.android.android_github_api.R
import com.android.android_github_api.data.model.github.GithubRepoSearchData
import com.android.android_github_api.launchFragmentInHiltContainer
import com.android.android_github_api.ui.main.TestCustomFragmentFactory
import com.android.android_github_api.ui.main.viewmodel.SearchViewModel
import com.android.android_github_api.util.GithubReposGenerator
import com.android.android_github_api.util.matchers.RecyclerViewMatcher
import com.android.android_github_api.util.matchers.ToastMatcher
import com.android.android_github_api.util.matchers.isNotCompletelyDisplayed
import com.android.android_github_api.utils.Constants
import com.android.android_github_api.utils.Event
import com.android.android_github_api.utils.Resource
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import javax.inject.Inject

@MediumTest
@HiltAndroidTest
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SearchFragmentTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var testFragmentFactory: TestCustomFragmentFactory

    @Mock
    lateinit var assistedFactory: SearchViewModel.AssistedSearchViewModelFactory

    private lateinit var mockViewModel: SearchViewModel

    private lateinit var mockNavController: NavController

    private val githubSearchData = MutableLiveData<Event<Resource<GithubRepoSearchData>>>()

    private var initialGithubName = "test"


    @Before
    fun setup() {
        hiltRule.inject()
        mockNavController = Mockito.mock(NavController::class.java)
        mockViewModel = Mockito.mock(SearchViewModel::class.java)
        Mockito.`when`(assistedFactory.create(initialGithubName)).thenReturn(mockViewModel)
        Mockito.`when`(mockViewModel.githubSearchData).thenReturn(githubSearchData)
        Mockito.`when`(mockViewModel.getName).thenReturn(initialGithubName)
        // Mockito.`when`(mockViewModel.isRefreshing).thenReturn(isRefreshing)
        testFragmentFactory.searchViewModelAssistedFactory =
            assistedFactory
        launchFragmentInHiltContainer<SearchFragment>(
            fragmentArgs = bundleOf(Pair("githubName", initialGithubName)),
            fragmentFactory = testFragmentFactory
        ) {
            Navigation.setViewNavController(requireView(), mockNavController)
        }
    }

    private fun notSearchedGithubSearchDataScenario() {
        githubSearchData.postValue(Event(Resource.empty()))
    }

    private fun initiallyLoadingGithubSearchDataScenario() {
        githubSearchData.postValue(Event(Resource.loading(data = null)))
    }

    private fun successGithubSearchDataScenario(data: GithubRepoSearchData) {
        githubSearchData.postValue(Event(Resource.success(data)))
    }

    private fun successButEmptyListGithubSearchDataScenario() {
        val data = GithubRepoSearchData(0, arrayListOf())
        githubSearchData.postValue(Event(Resource.success(data)))
    }

    private fun initialErrorGithubSearchDataScenario(error: String = "error") {
        Mockito.`when`(mockViewModel.isLoadingMore).thenReturn(false)
        githubSearchData.postValue(Event(Resource.error(data = null, message = error)))
    }

    private fun loadingMoreErrorGithubSearchDataScenario(data: GithubRepoSearchData) {
        Mockito.`when`(mockViewModel.isLoadingMore).thenReturn(true)
        githubSearchData.postValue(Event(Resource.error(data = data, message = "error")))
    }


    @Test
    fun viewModel_initiallyCallsGetReposWithLoadingMoreFalseArgument() {
        Mockito.verify(mockViewModel).getRepos(loadingMore = false)
    }

    @Test
    fun editTextGithubUser_initiallySetToViewModelName() {
        onView(withId(R.id.et_github_user)).check(matches(withText(initialGithubName)))
    }

    @Test
    fun githubUserSubmitButton_callsViewModelSetNameWithConcreteParam() {
        val newName = "newName"
        onView(withId(R.id.et_github_user)).perform(replaceText(newName))
        onView(withId(R.id.btn_github_user_submit)).perform(click())
        Mockito.verify(mockViewModel).setName(newName)
    }

    @Test
    fun editTextQueryOnChangeDebounce_callsViewModelSetQueryWithConcreteParam() = runBlocking {
        val query = "test"
        onView(withId(R.id.et_query)).perform(typeText(query))
        delay(Constants.DEBOUNCE_TIMEOUT_MS)
        Mockito.verify(mockViewModel).setQuery(query)
    }

    @Test
    fun editTextQueryOnChangeWithoutDebounce_doesNotCallViewModelSetQuery() = runBlocking {
        val query = "test"
        onView(withId(R.id.et_query)).perform(typeText(query))
        Mockito.verify(mockViewModel, Mockito.never()).setQuery(query)
    }

    @Test
    fun githubSearchDataObserver_notSearchedGithubSearchDataScenario_visibleOnlySearchReposTextView() {
        notSearchedGithubSearchDataScenario()
        onView(withId(R.id.tv_search_repos)).check(
            matches(withEffectiveVisibility(Visibility.VISIBLE))
        )
        onView(withId(R.id.ll_loading)).check(
            matches(withEffectiveVisibility(Visibility.GONE))
        )
        onView(withId(R.id.rv_repos)).check(
            matches(withEffectiveVisibility(Visibility.GONE))
        )
        onView(withId(R.id.tv_nothing_found)).check(
            matches(withEffectiveVisibility(Visibility.GONE))
        )
        onView(withId(R.id.ll_main_error)).check(
            matches(withEffectiveVisibility(Visibility.GONE))
        )
    }

    @Test
    fun githubSearchDataObserver_notSearchedGithubSearchDataScenario_releasesLoadingMoreWhenCurrentlyIsLoadingMore() =
        runBlocking {
            Mockito.`when`(mockViewModel.isLoadingMore).thenReturn(true)
            notSearchedGithubSearchDataScenario()

            delay(100)
            Mockito.verify(mockViewModel).releaseLoadingMore()
        }

    @Test
    fun githubSearchDataObserver_notSearchedGithubSearchDataScenario_doesNotReleaseLoadingMoreWhenCurrentlyIsNOTLoadingMore() =
        runBlocking {
            Mockito.`when`(mockViewModel.isLoadingMore).thenReturn(false)
            notSearchedGithubSearchDataScenario()

            delay(100)
            Mockito.verify(mockViewModel, Mockito.never()).releaseLoadingMore()
        }

    @Test
    fun githubSearchDataObserver_initiallyLoadingGithubSearchDataScenario_visibleOnlyMainLoadingLinearLayout() {
        initiallyLoadingGithubSearchDataScenario()
        onView(withId(R.id.tv_search_repos)).check(
            matches(withEffectiveVisibility(Visibility.GONE))
        )
        onView(withId(R.id.ll_loading)).check(
            matches(withEffectiveVisibility(Visibility.VISIBLE))
        )
        onView(withId(R.id.rv_repos)).check(
            matches(withEffectiveVisibility(Visibility.GONE))
        )
        onView(withId(R.id.tv_nothing_found)).check(
            matches(withEffectiveVisibility(Visibility.GONE))
        )
        onView(withId(R.id.ll_main_error)).check(
            matches(withEffectiveVisibility(Visibility.GONE))
        )
    }

    @Test
    fun githubSearchDataObserver_successGithubSearchDataScenario_visibleOnlyReposRecyclerView() {
        val repos = GithubReposGenerator.generate(10)
        val data = GithubRepoSearchData(repos.size, repos)
        successGithubSearchDataScenario(data)
        onView(withId(R.id.tv_search_repos)).check(
            matches(withEffectiveVisibility(Visibility.GONE))
        )
        onView(withId(R.id.ll_loading)).check(
            matches(withEffectiveVisibility(Visibility.GONE))
        )
        onView(withId(R.id.rv_repos)).check(
            matches(withEffectiveVisibility(Visibility.VISIBLE))
        )
        onView(withId(R.id.tv_nothing_found)).check(
            matches(withEffectiveVisibility(Visibility.GONE))
        )
        onView(withId(R.id.ll_main_error)).check(
            matches(withEffectiveVisibility(Visibility.GONE))
        )
    }

    @Test
    fun githubSearchDataObserver_successGithubSearchDataScenario_scrollsToTheStartWhenCurrentPageCounterIs1() {
        val repos = GithubReposGenerator.generate(30)
        val data = GithubRepoSearchData(repos.size, repos)
        successGithubSearchDataScenario(data)

        Mockito.`when`(mockViewModel.currentPageCounter).thenReturn(1)

        val action = RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(20)
        onView(withId(R.id.rv_repos)).perform(action)

        onView(listMatcher().atPosition(0)).check(doesNotExist())

        successGithubSearchDataScenario(data)
        onView(listMatcher().atPosition(0)).check(matches(isDisplayed()))
    }

    @Test
    fun githubSearchDataObserver_successGithubSearchDataScenario_doesNotScrollToTheStartWhenCurrentPageCounterIsGreaterThan1() {
        val repos = GithubReposGenerator.generate(30)
        val data = GithubRepoSearchData(repos.size, repos)
        successGithubSearchDataScenario(data)

        Mockito.`when`(mockViewModel.currentPageCounter).thenReturn(2)

        val action = RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(20)
        onView(withId(R.id.rv_repos)).perform(action)

        onView(listMatcher().atPosition(0)).check(doesNotExist())

        successGithubSearchDataScenario(data)
        onView(listMatcher().atPosition(0)).check(doesNotExist())
    }


    @Test
    fun githubSearchDataObserver_successGithubSearchDataScenario_releasesLoadingMoreWhenCurrentlyLoading() =
        runTest {
            Mockito.`when`(mockViewModel.isLoadingMore).thenReturn(true)

            val repos = GithubReposGenerator.generate(10)
            val data = GithubRepoSearchData(repos.size, repos)
            successGithubSearchDataScenario(data)

            withContext(Dispatchers.Default) {
                delay(500)
            }

            Mockito.verify(mockViewModel).releaseLoadingMore()
        }

    @Test
    fun githubSearchDataObserver_successGithubSearchDataScenario_doesNotReleaseLoadingMoreWhenCurrentlyNOTLoading() =
        runTest {
            Mockito.`when`(mockViewModel.isLoadingMore).thenReturn(false)

            val repos = GithubReposGenerator.generate(10)
            val data = GithubRepoSearchData(repos.size, repos)
            successGithubSearchDataScenario(data)

            withContext(Dispatchers.Default) {
                delay(500)
            }

            Mockito.verify(mockViewModel, Mockito.never()).releaseLoadingMore()
        }


    @Test
    fun githubSearchDataObserver_successButEmptyListGithubSearchDataScenario_visibleOnlyNothingFoundTextView() {
        successButEmptyListGithubSearchDataScenario()
        onView(withId(R.id.tv_search_repos)).check(
            matches(withEffectiveVisibility(Visibility.GONE))
        )
        onView(withId(R.id.ll_loading)).check(
            matches(withEffectiveVisibility(Visibility.GONE))
        )
        onView(withId(R.id.rv_repos)).check(
            matches(withEffectiveVisibility(Visibility.GONE))
        )
        onView(withId(R.id.tv_nothing_found)).check(
            matches(withEffectiveVisibility(Visibility.VISIBLE))
        )
        onView(withId(R.id.ll_main_error)).check(
            matches(withEffectiveVisibility(Visibility.GONE))
        )
    }

    @Test
    fun githubSearchDataObserver_initialErrorGithubSearchDataScenario_visibleOnlyMainErrorLinearLayout() {
        initialErrorGithubSearchDataScenario()
        onView(withId(R.id.tv_search_repos)).check(
            matches(withEffectiveVisibility(Visibility.GONE))
        )
        onView(withId(R.id.ll_loading)).check(
            matches(withEffectiveVisibility(Visibility.GONE))
        )
        onView(withId(R.id.rv_repos)).check(
            matches(withEffectiveVisibility(Visibility.GONE))
        )
        onView(withId(R.id.tv_nothing_found)).check(
            matches(withEffectiveVisibility(Visibility.GONE))
        )
        onView(withId(R.id.ll_main_error)).check(
            matches(withEffectiveVisibility(Visibility.VISIBLE))
        )
    }

    @Test
    fun githubSearchDataObserver_initialErrorGithubSearchDataScenario_clickMainRetryButton_callsGetRepos() {
        // to reset previous viewModel calls that occurred on fragment creation (such as initial user loading)
        Mockito.clearInvocations(mockViewModel)

        initialErrorGithubSearchDataScenario()
        onView(withId(R.id.btn_main_retry)).perform(click())

        Mockito.verify(mockViewModel).getRepos(loadingMore = false)
    }

    @Test
    fun githubSearchDataObserver_initialErrorGithubSearchDataScenario_doesNotInteractWithViewModel_doesNotShowErrorMessage() {
        Mockito.clearInvocations(mockViewModel)

        initialErrorGithubSearchDataScenario("initial toast error")

        Mockito.verifyNoInteractions(mockViewModel)
        ToastMatcher.onToast("initial toast error").check(doesNotExist())
    }

    @Test
    fun githubSearchDataObserver_loadingMoreErrorGithubSearchDataScenario_stillVisibleOnlyReposRecyclerView() {
        val repos = GithubReposGenerator.generate(10)
        val data = GithubRepoSearchData(repos.size, repos)
        loadingMoreErrorGithubSearchDataScenario(data)
        onView(withId(R.id.tv_search_repos)).check(
            matches(withEffectiveVisibility(Visibility.GONE))
        )
        onView(withId(R.id.ll_loading)).check(
            matches(withEffectiveVisibility(Visibility.GONE))
        )
        onView(withId(R.id.rv_repos)).check(
            matches(withEffectiveVisibility(Visibility.VISIBLE))
        )
        onView(withId(R.id.tv_nothing_found)).check(
            matches(withEffectiveVisibility(Visibility.GONE))
        )
        onView(withId(R.id.ll_main_error)).check(
            matches(withEffectiveVisibility(Visibility.GONE))
        )
    }

    @Test
    fun githubSearchDataObserver_loadingMoreErrorGithubSearchDataScenario_offsetsRecyclerVertically_showsErrorToast_releasesLoadingMore() =
        runTest {
            val repoCount = 30
            val repos = GithubReposGenerator.generate(repoCount)
            val data = GithubRepoSearchData(repos.size, repos)

            simulateLoadingMore(repoCount, isAlreadyLoadingMore = false, canPaginate = true)

            loadingMoreErrorGithubSearchDataScenario(data)


            // to wait for recycler view scroll actions, in this situation for offset
            withContext(Dispatchers.Default) {
                delay(500)
            }
            onView(listMatcher().atPosition(repoCount - 1)).check(isNotCompletelyDisplayed())
            ToastMatcher.onToast("error").check(matches(isDisplayed()))
            Mockito.verify(mockViewModel).releaseLoadingMore()
        }

    @Test
    fun onRepoClick_navigatesToWebViewFragmentWithConcreteUrlAndTitle() {
        val repos = GithubReposGenerator.generate(10)
        val data = GithubRepoSearchData(repos.size, repos)
        val targetRepo = repos[0]
        successGithubSearchDataScenario(data)
        onView(withText(targetRepo.name)).perform(click())
        Mockito.verify(mockNavController).navigate(
            SearchFragmentDirections.actionSearchFragmentToWebViewFragment(
                url = targetRepo.html_url,
                title = targetRepo.name
            )
        )
    }

    private fun simulateLoadingMore(
        repoCount: Int,
        isAlreadyLoadingMore: Boolean,
        canPaginate: Boolean
    ) {
        val repos = GithubReposGenerator.generate(repoCount)
        val data = GithubRepoSearchData(repos.size, repos)
        successGithubSearchDataScenario(data)

        Mockito.`when`(mockViewModel.isLoadingMore).thenReturn(isAlreadyLoadingMore)
        Mockito.`when`(mockViewModel.canPaginate()).thenReturn(canPaginate)

        val action = RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(repoCount - 1)
        onView(withId(R.id.rv_repos)).perform(action)

        // to make loadingMore indicator visible
        if (canPaginate && !isAlreadyLoadingMore) {
            Mockito.`when`(mockViewModel.isLoadingMore).thenReturn(true)
            val scrollToIndicator =
                RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(repoCount)
            onView(withId(R.id.rv_repos)).perform(scrollToIndicator)
        }
    }

    @Test
    fun loadMore_addsLoadingIndicatorInTheEnd_callsCaptureLoadingMore_callsGetRepos() = runTest {
        Mockito.clearInvocations(mockViewModel)
        val repoCount = 20
        simulateLoadingMore(repoCount, isAlreadyLoadingMore = false, canPaginate = true)

        withContext(Dispatchers.Default) {
            delay(500)
        }

        onView(withId(R.id.ll_loading_more_indicator)).check(matches(isDisplayed()))
        Mockito.verify(mockViewModel).captureLoadingMore()
        Mockito.verify(mockViewModel).getRepos(loadingMore = true)

    }

    @Test
    fun loadMore_isAlreadyLoadingMore_doesNotAddLoadingIndicator_doesNotLoadMore() {
        Mockito.clearInvocations(mockViewModel)
        val repoCount = 20
        simulateLoadingMore(repoCount, isAlreadyLoadingMore = true, canPaginate = true)

        onView(withId(R.id.ll_loading_more_indicator)).check(doesNotExist())
        Mockito.verify(mockViewModel, Mockito.never()).captureLoadingMore()
        Mockito.verify(mockViewModel, Mockito.never()).getRepos(loadingMore = true)

    }

    @Test
    fun loadMore_canNOTPaginate_doesNotAddLoadingIndicator_doesNotLoadMore() {
        Mockito.clearInvocations(mockViewModel)
        val repoCount = 20
        simulateLoadingMore(repoCount, isAlreadyLoadingMore = false, canPaginate = false)

        onView(withId(R.id.ll_loading_more_indicator)).check(doesNotExist())
        Mockito.verify(mockViewModel, Mockito.never()).captureLoadingMore()
        Mockito.verify(mockViewModel, Mockito.never()).getRepos(loadingMore = true)

    }


    private fun listMatcher(): RecyclerViewMatcher {
        return RecyclerViewMatcher(R.id.rv_repos)
    }
}