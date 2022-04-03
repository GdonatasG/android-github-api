package com.android.android_github_api.ui.main.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.android.android_github_api.MainCoroutineRule
import com.android.android_github_api.captureValues
import com.android.android_github_api.data.model.github.GithubUser
import com.android.android_github_api.data.repository.GithubRepository
import com.android.android_github_api.getOrAwaitValueTest
import com.android.android_github_api.utils.Event
import com.android.android_github_api.utils.Resource
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock

@ExperimentalCoroutinesApi
class HomeViewModelTest {
    private lateinit var viewModel: HomeViewModel
    private lateinit var mockGithubRepository: GithubRepository

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setup() {
        mockGithubRepository = mock(GithubRepository::class.java)
        viewModel = HomeViewModel(mockGithubRepository)
    }

    @Test
    fun setName_setsPassedName() {
        viewModel.setName("test")

        assertThat(viewModel.getName).isEqualTo("test")
    }

    @Test
    fun isRefreshing_initiallyFalse() {
        assertThat(viewModel.isRefreshing.getOrAwaitValueTest()).isFalse()
    }

    @Test
    fun loadGithubUser_getsUserFromRepositoryAndEmitsLoadingAndSuccessStates() =
        runTest {
            val githubUser =
                GithubUser(
                    name = "test",
                    avatar_url = "url",
                    html_url = "url",
                    public_repos_count = 5
                )

            Mockito.`when`(mockGithubRepository.getUserByName("test")).thenReturn(githubUser)

            viewModel.setName("test")
            viewModel.loadGithubUser()
            withContext(Dispatchers.Default) {
                viewModel.getGithubUser.captureValues {
                    this.assertSendsValues(
                        timeout = 100,
                        Event(Resource.loading(data = null)),
                        Event(Resource.success(githubUser)),
                    )
                }
            }
        }

    @Test
    fun loadGithubUser_presentsAlreadyExistingDataOnLoadingState() = runTest {
        val existingGithubUser =
            GithubUser(
                name = "test",
                avatar_url = "url",
                html_url = "url",
                public_repos_count = 5
            )

        viewModel.fakeInitialGithubUser(existingGithubUser)

        val newGithubUser =
            GithubUser(
                name = "testNew",
                avatar_url = "url",
                html_url = "url",
                public_repos_count = 5
            )

        Mockito.`when`(mockGithubRepository.getUserByName("new")).thenReturn(newGithubUser)

        viewModel.setName("new")
        viewModel.loadGithubUser()

        withContext(Dispatchers.Default) {
            viewModel.getGithubUser.captureValues {
                this.assertSendsValues(
                    timeout = 100,
                    Event(Resource.loading(data = existingGithubUser)),
                    Event(Resource.success(newGithubUser)),
                )
            }
        }
    }

    @Test
    fun loadGithubUser_getsErrorFromRepositoryWhileGettingUserAndEmitsLoadingAndErrorStates() =
        runTest {
            Mockito.`when`(mockGithubRepository.getUserByName("test")).then {
                throw Exception("error")
            }

            viewModel.setName("test")
            viewModel.loadGithubUser()
            withContext(Dispatchers.Default) {
                viewModel.getGithubUser.captureValues {
                    this.assertSendsValues(
                        timeout = 100,
                        Event(Resource.loading(data = null)),
                        Event(Resource.error(data = null, message = "error")),
                    )
                }
            }
        }

    @Test
    fun loadGithubUser_isNotRefresh_doesNotChangeIsRefreshing() = runTest {
        viewModel.loadGithubUser()
        withContext(Dispatchers.Default) {
            viewModel.isRefreshing.captureValues {
                this.assertSendsValues(500, false)
            }
        }
    }

    @Test
    fun loadGithubUser_isRefresh_changesIsRefreshingOnSuccess() = runTest {
        Mockito.`when`(mockGithubRepository.getUserByName("test"))
            .thenReturn(GithubUser("", "", "", 1))

        viewModel.setName("test")
        viewModel.loadGithubUser(isRefresh = true)
        withContext(Dispatchers.Default) {
            viewModel.isRefreshing.captureValues {
                this.assertSendsValues(300, true, false)
            }
        }
    }

    @Test
    fun loadGithubUser_isRefresh_changesIsRefreshingOnException() = runTest {
        Mockito.`when`(mockGithubRepository.getUserByName("test")).then {
            throw CancellationException("error")
        }
        viewModel.setName("test")
        viewModel.loadGithubUser(isRefresh = true)
        withContext(Dispatchers.Default) {
            viewModel.isRefreshing.captureValues {
                this.assertSendsValues(300, true, false)
            }
        }
    }

    @Test
    fun loadGithubUser_presentsAlreadyExistingDataOnErrorState() = runTest {
        val existingGithubUser =
            GithubUser(
                name = "test",
                avatar_url = "url",
                html_url = "url",
                public_repos_count = 5
            )

        viewModel.fakeInitialGithubUser(existingGithubUser)

        Mockito.`when`(mockGithubRepository.getUserByName("new")).then {
            throw Exception("error")
        }

        viewModel.setName("new")
        viewModel.loadGithubUser()

        withContext(Dispatchers.Default) {
            viewModel.getGithubUser.captureValues {
                this.assertSendsValues(
                    timeout = 100,
                    Event(Resource.loading(data = existingGithubUser)),
                    Event(Resource.error(data = existingGithubUser, message = "error"))
                )
            }
        }
    }

    @Test
    fun loadGithubUser_emitsOnlyLoadingStateOnCancellationException() = runTest {
        Mockito.`when`(mockGithubRepository.getUserByName("test")).then {
            throw CancellationException("error")
        }

        viewModel.setName("test")
        viewModel.loadGithubUser()

        withContext(Dispatchers.Default) {
            viewModel.getGithubUser.captureValues {
                this.assertSendsValues(
                    timeout = 100,
                    Event(Resource.loading(data = null)),
                    // DOESN'T EMIT ERROR STATE HERE
                )
            }
        }
    }

}