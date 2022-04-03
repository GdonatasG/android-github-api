package com.android.android_github_api.ui.main.viewmodel

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.*
import com.android.android_github_api.OpenForTesting
import com.android.android_github_api.data.model.github.GithubRepo
import com.android.android_github_api.data.model.github.GithubRepoSearchData
import com.android.android_github_api.data.repository.GithubRepository
import com.android.android_github_api.utils.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.*

@OpenForTesting
class SearchViewModel @AssistedInject constructor(
    private val connectivityManager: ConnectivityManager,
    private val githubRepository: GithubRepository,
    @Assisted
    private val initialName: String,
) :
    ViewModel() {

    private var name = ""

    val getName
        get() = name

    fun setName(githubName: String) {
        if (githubName != name) {
            name = githubName
            getRepos(loadingMore = false)
        }
    }

    private var query = ""

    val getQuery
        get() = query

    fun setQuery(newQuery: String) {
        if (newQuery != query) {
            query = newQuery
            getRepos(loadingMore = false)
        }
    }

    private var _isLoadingMore: Boolean = false
    val isLoadingMore: Boolean
        get() = _isLoadingMore

    fun captureLoadingMore() {
        _isLoadingMore = true
    }

    fun releaseLoadingMore() {
        _isLoadingMore = false
    }

    private var _currentPageCounter: Int = Constants.STARTING_GITHUB_PAGE
    val currentPageCounter
        get() = _currentPageCounter
    private val _currentPage = MutableLiveData(_currentPageCounter)
    val currentPage
        get() = _currentPage

    // job for search request
    private var searchDataJob: Job = Job()

    private val _githubSearchData = MutableLiveData<Event<Resource<GithubRepoSearchData>>>()

    val githubSearchData: LiveData<Event<Resource<GithubRepoSearchData>>>
        get() = _githubSearchData

    fun getRepos(loadingMore: Boolean) {
        // canceling previous job
        // there is no point to continue previous search
        // if user performed a new one
        searchDataJob.cancel()
        searchDataJob = Job()
        viewModelScope.launch(
            CoroutineScope(
                searchDataJob
            ).coroutineContext
        ) {
            try {
                if (query.isEmpty() && name.isEmpty()) {
                    _githubSearchData.setValue(Event(Resource.empty()))
                } else {
                    // just increasing page number when paginating
                    if (loadingMore) {
                        _currentPageCounter++
                        _currentPage.setValue(currentPageCounter)
                    }
                    // resetting pagination when not paginating
                    else {
                        _currentPageCounter = Constants.STARTING_GITHUB_PAGE
                        _currentPage.setValue(_currentPageCounter)
                        _githubSearchData.setValue(Event(Resource.loading(data = null)))
                    }

                    if (!connectivityManager.isOnline()) {
                        if (loadingMore) {
                            withContext(Dispatchers.IO + searchDataJob) {
                                delay(Constants.LOADING_MORE_DELAY_MS)
                            }
                            _currentPageCounter--
                            _currentPage.setValue(_currentPageCounter)
                        }
                        _githubSearchData.setValue(
                            Event(
                                Resource.error(
                                    data = if (loadingMore) githubSearchData.value?.peekContent()?.data else null,
                                    message = "Check your internet connection!"
                                )
                            )
                        )
                    } else {
                        var searchData = withContext(Dispatchers.IO + searchDataJob) {
                            githubRepository.getRepos(
                                user = if (name.isNotEmpty()) name else null,
                                queryText = query,
                                language = null,
                                perPage = Constants.DEFAULT_GITHUB_REPOS_PER_PAGE,
                                page = _currentPageCounter,
                            )
                        }

                        // concatenating previous and new results when paginating
                        if (loadingMore) {
                            val newResults: ArrayList<GithubRepo> = arrayListOf()
                            newResults.addAll(
                                githubSearchData.value?.peekContent()?.data?.items
                                    ?: arrayListOf()
                            )
                            newResults.addAll(searchData.items)
                            searchData = GithubRepoSearchData(
                                searchData.total_count,
                                items = newResults
                            )
                        }
                        _githubSearchData.setValue(Event(Resource.success(searchData)))
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    return@launch
                }
                if (loadingMore) {
                    _currentPageCounter--
                    _currentPage.setValue(_currentPageCounter)
                }
                _githubSearchData.setValue(
                    Event(
                        Resource.error(
                            data = if (loadingMore) githubSearchData.value?.peekContent()?.data else null,
                            message = e.message ?: "Error getting repos!"
                        )
                    )
                )
            }
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun fakeInitialGithubSearchData(data: GithubRepoSearchData) {
        _githubSearchData.value = Event(Resource.success(data = data))
    }

    init {
        name = initialName
    }

    interface SearchViewModelFactory {
        fun create(initialName: String): SearchViewModel
    }

    @dagger.assisted.AssistedFactory
    interface AssistedSearchViewModelFactory : SearchViewModelFactory

    companion object {
        fun provideFactory(
            assistedFactory: SearchViewModelFactory,
            initialName: String
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(initialName) as T
            }
        }
    }

    fun canPaginate(): Boolean {
        return try {
            _githubSearchData.value!!.peekContent().data!!.total_count > _githubSearchData.value!!.peekContent().data!!.items.size
        } catch (e: Exception) {
            false
        }
    }

    public override fun onCleared() {
        super.onCleared()
        searchDataJob.cancel()
    }
}