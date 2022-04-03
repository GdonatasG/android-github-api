package com.android.android_github_api.ui.main.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.android.android_github_api.MainCoroutineRule
import com.android.android_github_api.captureValues
import com.android.android_github_api.data.model.github.GithubRepo
import com.android.android_github_api.data.model.github.GithubRepoSearchData
import com.android.android_github_api.data.model.github.GithubUser
import com.android.android_github_api.data.repository.GithubRepository
import com.android.android_github_api.getOrAwaitValueTest
import com.android.android_github_api.utils.ConnectivityManager
import com.android.android_github_api.utils.Constants
import com.android.android_github_api.utils.Event
import com.android.android_github_api.utils.Resource
import com.google.common.truth.Truth
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.*


@ExperimentalCoroutinesApi
class SearchViewModelTest {
    private lateinit var viewModel: SearchViewModel
    private lateinit var mockGithubRepository: GithubRepository
    private lateinit var mockConnectivityManager: ConnectivityManager

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private val initialGithubName = "test"

    @Before
    fun setup() {
        mockGithubRepository = mock(GithubRepository::class.java)
        mockConnectivityManager = mock(ConnectivityManager::class.java)
        viewModel =
            spy(
                SearchViewModel(
                    mockConnectivityManager,
                    mockGithubRepository,
                    initialName = initialGithubName,
                )
            )
    }

    @Test
    fun init_setsNameFromViewModelParams() {
        Truth.assertThat(viewModel.getName).isEqualTo(initialGithubName)
    }

    @Test
    fun setName_DoesNotCallGetReposWithSameName() {
        viewModel.setName(initialGithubName)
        verify(viewModel, never()).getRepos(anyBoolean())
    }

    @Test
    fun setName_setsNewNameAndCallsGetRepos() {
        viewModel.setName("new")
        Truth.assertThat(viewModel.getName).isEqualTo("new")
        verify(viewModel).getRepos(loadingMore = false)
    }

    @Test
    fun query_initiallyEmpty() {
        Truth.assertThat(viewModel.getQuery).isEmpty()
    }

    @Test
    fun setQuery_DoesNotCallGetReposWithSameQuery() {
        viewModel.setQuery("")
        verify(viewModel, never()).getRepos(anyBoolean())
    }

    @Test
    fun setQuery_setsNewQueryAndCallsGetRepos() {
        viewModel.setQuery("new")
        Truth.assertThat(viewModel.getQuery).isEqualTo("new")
        verify(viewModel).getRepos(loadingMore = false)
    }

    @Test
    fun isLoadingMore_initiallyFalse() {
        Truth.assertThat(viewModel.isLoadingMore).isFalse()
    }

    @Test
    fun captureLoadingMore_setsIsLoadingMoreToTrue() {
        viewModel.captureLoadingMore()
        Truth.assertThat(viewModel.isLoadingMore).isTrue()
    }

    @Test
    fun releaseLoadingMore_setsIsLoadingMoreToFalse() {
        viewModel.releaseLoadingMore()
        Truth.assertThat(viewModel.isLoadingMore).isFalse()
    }

    @Test
    fun currentPage_initiallyEqualsToSpecificConst() {
        Truth.assertThat(viewModel.currentPage.getOrAwaitValueTest())
            .isEqualTo(Constants.STARTING_GITHUB_PAGE)
    }

    @Test
    fun getGithubSearchData_emitsEmptyState_DoesNotDoInteractionsWithMocksWhenQueryAndNameAreEmpty() =
        runTest {
            viewModel =
                spy(
                    SearchViewModel(
                        mockConnectivityManager,
                        mockGithubRepository,
                        initialName = "",
                    )
                )
            viewModel.getRepos(loadingMore = false)

            withContext(Dispatchers.Default) {
                viewModel.githubSearchData.captureValues {
                    this.assertSendsValues(100, Event(Resource.empty()))
                }
            }

            verifyNoInteractions(mockGithubRepository)
            verifyNoInteractions(mockConnectivityManager)
        }

    @Test
    fun getGithubSearchData_WhenIsNotLoadingMore_WhenConnectedToTheInternet_resetsCurrentPageToDefault_CallsRepository_EmitsLoadingAndSuccessStates() =
        runTest {
            val githubRepoSearchData = GithubRepoSearchData(
                1,
                arrayListOf()
            )

            `when`(
                mockGithubRepository.getRepos(
                    user = initialGithubName,
                    queryText = "",
                    language = null,
                    perPage = Constants.DEFAULT_GITHUB_REPOS_PER_PAGE,
                    page = Constants.STARTING_GITHUB_PAGE
                )
            ).thenReturn(githubRepoSearchData)
            `when`(mockConnectivityManager.isOnline()).thenReturn(true)



            viewModel.getRepos(loadingMore = false)
            withContext(Dispatchers.Default) {
                viewModel.githubSearchData.captureValues {
                    this.assertSendsValues(
                        100,
                        Event(Resource.loading(data = null)),
                        Event(Resource.success(githubRepoSearchData))
                    )
                }
                Truth.assertThat(viewModel.currentPage.getOrAwaitValueTest())
                    .isEqualTo(Constants.STARTING_GITHUB_PAGE)
            }
            verify(mockGithubRepository).getRepos(
                user = initialGithubName,
                queryText = "",
                language = null,
                perPage = Constants.DEFAULT_GITHUB_REPOS_PER_PAGE,
                page = Constants.STARTING_GITHUB_PAGE
            )
        }

    @Test
    fun getGithubSearchData_WhenIsNotLoadingMore_WhenNotConnectedToTheInternet_EmitsLoadingAndErrorStatesDoesNotInteractWithRepository() =
        runTest {
            `when`(mockConnectivityManager.isOnline()).thenReturn(false)

            withContext(Dispatchers.Default) {
                viewModel.getRepos(loadingMore = false)
                viewModel.githubSearchData.captureValues {
                    this.assertSendsValues(
                        100,
                        Event(Resource.loading(data = null)),
                        Event(
                            Resource.error(
                                data = null,
                                message = "Check your internet connection!"
                            )
                        ),
                    )
                }
            }
            verifyNoInteractions(mockGithubRepository)
        }

    @Test
    fun getGithubSearchData_WhenIsLoadingMore_WhenConnectedToTheInternet_incrementsCurrentPage_CallsRepository_EmitsSuccessState() =
        runTest {
            val githubRepoSearchData = GithubRepoSearchData(
                1,
                arrayListOf()
            )

            `when`(
                mockGithubRepository.getRepos(
                    user = initialGithubName,
                    queryText = "",
                    language = null,
                    perPage = Constants.DEFAULT_GITHUB_REPOS_PER_PAGE,
                    page = Constants.STARTING_GITHUB_PAGE + 1
                )
            ).thenReturn(githubRepoSearchData)
            `when`(mockConnectivityManager.isOnline()).thenReturn(true)



            viewModel.getRepos(loadingMore = true)
            withContext(Dispatchers.Default) {
                viewModel.githubSearchData.captureValues {
                    this.assertSendsValues(
                        100,
                        Event(Resource.success(githubRepoSearchData))
                    )
                }
                Truth.assertThat(viewModel.currentPage.getOrAwaitValueTest())
                    .isEqualTo(Constants.STARTING_GITHUB_PAGE + 1)
            }
            verify(mockGithubRepository).getRepos(
                user = initialGithubName,
                queryText = "",
                language = null,
                perPage = Constants.DEFAULT_GITHUB_REPOS_PER_PAGE,
                page = Constants.STARTING_GITHUB_PAGE + 1
            )
        }

    @Test
    fun getGithubSearchData_WhenIsLoadingMore_WhenConnectedToTheInternet_mergesNewReposWithPrevious_EmitsSuccessState() =
        runTest {
            val githubUser = GithubUser(
                name = "test",
                avatar_url = "",
                html_url = "",
                public_repos_count = 5
            )
            val initialGithubRepoSearchData = GithubRepoSearchData(
                3,
                arrayListOf(
                    GithubRepo(
                        name = "test1",
                        owner = githubUser,
                        description = "test1",
                        html_url = "test1",
                        language = "test1",
                    )
                )
            )
            val newGithubRepoSearchData = GithubRepoSearchData(
                3,
                arrayListOf(
                    GithubRepo(
                        name = "test2",
                        owner = githubUser,
                        description = "test2",
                        html_url = "test2",
                        language = "test2",
                    ),
                    GithubRepo(
                        name = "test3",
                        owner = githubUser,
                        description = "test3",
                        html_url = "test3",
                        language = "test3",
                    )
                )
            )
            val newList: ArrayList<GithubRepo> = arrayListOf()
            newList.apply {
                addAll(initialGithubRepoSearchData.items)
                addAll(newGithubRepoSearchData.items)
            }

            val mergedData = GithubRepoSearchData(
                total_count = newGithubRepoSearchData.total_count,
                items = newList
            )

            viewModel.fakeInitialGithubSearchData(initialGithubRepoSearchData)

            `when`(
                mockGithubRepository.getRepos(
                    user = initialGithubName,
                    queryText = "",
                    language = null,
                    perPage = Constants.DEFAULT_GITHUB_REPOS_PER_PAGE,
                    page = Constants.STARTING_GITHUB_PAGE + 1
                )
            ).thenReturn(newGithubRepoSearchData)
            `when`(mockConnectivityManager.isOnline()).thenReturn(true)



            viewModel.getRepos(loadingMore = true)
            withContext(Dispatchers.Default) {
                viewModel.githubSearchData.captureValues {
                    this.assertSendsValues(
                        100,
                        Event(Resource.success(initialGithubRepoSearchData)),
                        Event(Resource.success(mergedData))
                    )
                }
            }
        }

    @Test
    fun getGithubSearchData_WhenIsLoadingMore_WhenNotConnectedToTheInternet_keepsCurrentPage_EmitsErrorState_DoesNotInteractWithRepository() =
        runTest {
            `when`(mockConnectivityManager.isOnline()).thenReturn(false)

            viewModel.getRepos(loadingMore = true)
            withContext(Dispatchers.Default) {
                viewModel.githubSearchData.captureValues {
                    this.assertSendsValues(
                        Constants.LOADING_MORE_DELAY_MS + 100,
                        Event(
                            Resource.error(
                                data = null,
                                message = "Check your internet connection!"
                            )
                        )
                    )
                }
                Truth.assertThat(viewModel.currentPage.getOrAwaitValueTest())
                    .isEqualTo(Constants.STARTING_GITHUB_PAGE)
            }
            verifyNoInteractions(mockGithubRepository)
        }

    @Test
    fun getGithubSearchData_WhenIsLoadingMore_WhenNotConnectedToTheInternet_keepsCurrentPage_EmitsErrorStateWithPreviousData_DoesNotInteractWithRepository() =
        runTest {
            val initialGithubSearchData = GithubRepoSearchData(
                total_count = 2,
                items = arrayListOf(),
            )

            viewModel.fakeInitialGithubSearchData(data = initialGithubSearchData)

            `when`(mockConnectivityManager.isOnline()).thenReturn(false)



            viewModel.getRepos(loadingMore = true)
            withContext(Dispatchers.Default) {
                viewModel.githubSearchData.captureValues {
                    this.assertSendsValues(
                        Constants.LOADING_MORE_DELAY_MS + 100,
                        Event(
                            Resource.success(data = initialGithubSearchData)
                        ),
                        Event(
                            Resource.error(
                                data = initialGithubSearchData,
                                message = "Check your internet connection!"
                            )
                        )
                    )
                }
                Truth.assertThat(viewModel.currentPage.getOrAwaitValueTest())
                    .isEqualTo(Constants.STARTING_GITHUB_PAGE)
            }
            verifyNoInteractions(mockGithubRepository)
        }

    @Test
    fun getGithubSearchData_WhenIsLoadingMore_WhenConnectedToTheInternet_getsErrorFromRepositoryRequest_KeepsCurrentPage_EmitsErrorState() =
        runTest {
            `when`(mockConnectivityManager.isOnline()).thenReturn(true)
            `when`(
                mockGithubRepository.getRepos(
                    user = initialGithubName,
                    queryText = "",
                    language = null,
                    perPage = Constants.DEFAULT_GITHUB_REPOS_PER_PAGE,
                    page = Constants.STARTING_GITHUB_PAGE + 1
                )
            ).then {
                throw Exception("error")
            }

            viewModel.getRepos(loadingMore = true)

            withContext(Dispatchers.Default) {
                viewModel.githubSearchData.captureValues {
                    this.assertSendsValues(
                        100,
                        Event(Resource.error(data = null, message = "error"))
                    )
                }
                Truth.assertThat(viewModel.currentPage.getOrAwaitValueTest())
                    .isEqualTo(Constants.STARTING_GITHUB_PAGE)
            }


        }

    @Test
    fun getGithubSearchData_WhenIsLoadingMore_WhenConnectedToTheInternet_getsErrorFromRepositoryRequest_EmitsErrorStateWithPreviousData() =
        runTest {
            val initialGithubSearchData = GithubRepoSearchData(
                total_count = 2,
                items = arrayListOf(),
            )

            viewModel.fakeInitialGithubSearchData(data = initialGithubSearchData)

            `when`(mockConnectivityManager.isOnline()).thenReturn(true)
            `when`(
                mockGithubRepository.getRepos(
                    user = initialGithubName,
                    queryText = "",
                    language = null,
                    perPage = Constants.DEFAULT_GITHUB_REPOS_PER_PAGE,
                    page = Constants.STARTING_GITHUB_PAGE + 1
                )
            ).then {
                throw Exception("error")
            }

            viewModel.getRepos(loadingMore = true)

            withContext(Dispatchers.Default) {
                viewModel.githubSearchData.captureValues {
                    this.assertSendsValues(
                        100,
                        Event(Resource.success(data = initialGithubSearchData)),
                        Event(Resource.error(data = initialGithubSearchData, message = "error"))
                    )
                }
            }


        }

    @Test
    fun getGithubSearchData_WhenIsNotLoadingMore_WhenConnectedToTheInternet_getsErrorFromRepositoryRequest_KeepsCurrentPage_EmitsLoadingAndErrorStates() =
        runTest {
            `when`(mockConnectivityManager.isOnline()).thenReturn(true)
            `when`(
                mockGithubRepository.getRepos(
                    user = initialGithubName,
                    queryText = "",
                    language = null,
                    perPage = Constants.DEFAULT_GITHUB_REPOS_PER_PAGE,
                    page = Constants.STARTING_GITHUB_PAGE
                )
            ).then {
                throw Exception("error")
            }

            viewModel.getRepos(loadingMore = false)

            withContext(Dispatchers.Default) {
                viewModel.githubSearchData.captureValues {
                    this.assertSendsValues(
                        100,
                        Event(Resource.loading(data = null)),
                        Event(Resource.error(data = null, message = "error"))
                    )
                }
                Truth.assertThat(viewModel.currentPage.getOrAwaitValueTest())
                    .isEqualTo(Constants.STARTING_GITHUB_PAGE)
            }


        }

    @Test
    fun getGithubSearchData_callsRepositoryWithNullUserWhenCurrentNameIsEmpty_IncludesConcreteQuery() =
        runTest {
            viewModel =
                spy(
                    SearchViewModel(
                        mockConnectivityManager,
                        mockGithubRepository,
                        initialName = "",
                    )
                )

            `when`(mockConnectivityManager.isOnline()).thenReturn(true)

            withContext(Dispatchers.Default) {
                viewModel.setQuery("test")
                withTimeout(500) {
                    verify(mockGithubRepository).getRepos(
                        user = null,
                        queryText = "test",
                        language = null,
                        perPage = Constants.DEFAULT_GITHUB_REPOS_PER_PAGE,
                        page = Constants.STARTING_GITHUB_PAGE
                    )
                }
            }
        }

    @Test
    fun getGithubSearchData_WhenIsNotLoadingMore_WhenConnectedToTheInternet_emitsOnlyLoadingStateOnCancellationException() =
        runTest {
            `when`(mockConnectivityManager.isOnline()).thenReturn(true)
            `when`(
                mockGithubRepository.getRepos(
                    user = initialGithubName,
                    queryText = "",
                    language = null,
                    perPage = Constants.DEFAULT_GITHUB_REPOS_PER_PAGE,
                    page = Constants.STARTING_GITHUB_PAGE
                )
            ).then {
                throw CancellationException("error")
            }

            viewModel.getRepos(loadingMore = false)

            withContext(Dispatchers.Default) {
                viewModel.githubSearchData.captureValues {
                    this.assertSendsValues(
                        100,
                        Event(Resource.loading(data = null)),
                        // DOESN'T EMIT ERROR STATE HERE
                    )
                }
            }


        }

    @Test
    fun canPaginate_returnsFalseWhenGithubSearchDataWasNeverSet() {
        Truth.assertThat(viewModel.canPaginate()).isFalse()
    }

    @Test
    fun canPaginate_returnsFalseWhenTotalReposCountIsEqualToCurrentReposListSize() {
        val data = GithubRepoSearchData(
            total_count = 1,
            items = arrayListOf(
                GithubRepo(
                    name = "",
                    owner = GithubUser("", "", "", 0),
                    description = "",
                    html_url = "",
                    language = ""
                )
            )
        )
        viewModel.fakeInitialGithubSearchData(data)
        Truth.assertThat(viewModel.canPaginate()).isFalse()
    }

    @Test
    fun canPaginate_returnsFalseWhenTotalReposCountIsLessThanCurrentReposListSize() {
        val data = GithubRepoSearchData(
            total_count = 1,
            items = arrayListOf(
                GithubRepo(
                    name = "",
                    owner = GithubUser("", "", "", 0),
                    description = "",
                    html_url = "",
                    language = ""
                ),
                GithubRepo(
                    name = "",
                    owner = GithubUser("", "", "", 0),
                    description = "",
                    html_url = "",
                    language = ""
                )
            )
        )
        viewModel.fakeInitialGithubSearchData(data)
        Truth.assertThat(viewModel.canPaginate()).isFalse()
    }

    @Test
    fun canPaginate_returnsTrueWhenTotalReposCountIsGreaterThanCurrentReposListSize() {
        val data = GithubRepoSearchData(
            total_count = 2,
            items = arrayListOf(
                GithubRepo(
                    name = "",
                    owner = GithubUser("", "", "", 0),
                    description = "",
                    html_url = "",
                    language = ""
                ),
            )
        )
        viewModel.fakeInitialGithubSearchData(data)
        Truth.assertThat(viewModel.canPaginate()).isTrue()
    }

}