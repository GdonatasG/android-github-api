package com.android.android_github_api.data.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.android.android_github_api.data.api.GithubService
import com.android.android_github_api.data.model.github.GithubUser
import com.android.android_github_api.utils.Constants
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito


@ExperimentalCoroutinesApi
class GithubRepositoryTest {
    lateinit var githubRepository: GithubRepository
    lateinit var mockGithubService: GithubService

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        mockGithubService = Mockito.mock(GithubService::class.java)
        githubRepository = GithubRepository(mockGithubService)
    }

    @Test
    fun getUserByName_callsGetUserByNameFromGithubService() = runTest {
        val githubUser =
            GithubUser(name = "test", avatar_url = "url", html_url = "url", public_repos_count = 5)
        Mockito.`when`(mockGithubService.getUserByName("test")).thenReturn(githubUser)

        val result = githubRepository.getUserByName("test")

        assertThat(result).isEqualTo(githubUser)
        Mockito.verify(mockGithubService).getUserByName("test")

    }

    @Test
    fun getRepos_callsServiceWithConcreteArgumentsWhenPassingNullArgumentsToRepositoryFunction() =
        runTest {
            githubRepository.getRepos(null, null, null, null, null)

            Mockito.verify(mockGithubService).getRepos(
                "", Constants.DEFAULT_GITHUB_REPOS_PER_PAGE,
                Constants.STARTING_GITHUB_PAGE
            )


        }

    @Test
    fun getRepos_callsServiceWithConcreteQueryWhenPassingNullArgumentsExceptQueryTextToRepositoryFunction() =
        runTest {
            githubRepository.getRepos(null, "test", null, null, null)

            Mockito.verify(mockGithubService).getRepos(
                "test", Constants.DEFAULT_GITHUB_REPOS_PER_PAGE,
                Constants.STARTING_GITHUB_PAGE
            )
        }

    @Test
    fun getRepos_callsServiceWithConcreteQueryWhenPassingNullArgumentsExceptUserToRepositoryFunction() =
        runTest {
            githubRepository.getRepos("test", null, null, null, null)

            Mockito.verify(mockGithubService).getRepos(
                " user:test", Constants.DEFAULT_GITHUB_REPOS_PER_PAGE,
                Constants.STARTING_GITHUB_PAGE
            )
        }

    @Test
    fun getRepos_callsServiceWithConcreteQueryWhenPassingNullArgumentsExceptLanguageToRepositoryFunction() =
        runTest {
            githubRepository.getRepos(null, null, "test", null, null)

            Mockito.verify(mockGithubService).getRepos(
                " language:test", Constants.DEFAULT_GITHUB_REPOS_PER_PAGE,
                Constants.STARTING_GITHUB_PAGE
            )
        }

    @Test
    fun getRepos_callsServiceWithConcreteQueryWhenAllArgumentsPassedToRepositoryFunction() =
        runTest {
            githubRepository.getRepos("testUser", "testQuery", "testLanguage", 10, 5)

            Mockito.verify(mockGithubService).getRepos(
                "testQuery user:testUser language:testLanguage", 10,
                5
            )
        }
}