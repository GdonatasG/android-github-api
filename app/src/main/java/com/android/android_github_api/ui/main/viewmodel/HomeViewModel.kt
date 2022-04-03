package com.android.android_github_api.ui.main.viewmodel

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.*
import com.android.android_github_api.OpenForTesting
import com.android.android_github_api.data.model.github.GithubUser
import com.android.android_github_api.data.repository.GithubRepository
import com.android.android_github_api.utils.Event
import com.android.android_github_api.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@OpenForTesting
class HomeViewModel @Inject constructor(private val githubRepository: GithubRepository) :
    ViewModel() {

    private var name = ""

    val getName
        get() = name

    fun setName(githubName: String) {
        name = githubName
    }

    private val _isRefreshing = MutableLiveData(false)
    val isRefreshing: LiveData<Boolean>
        get() = _isRefreshing

    private val _githubUserData = MutableLiveData<Event<Resource<GithubUser>>>()

    val getGithubUser: LiveData<Event<Resource<GithubUser>>>
        get() = _githubUserData

    fun loadGithubUser(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _isRefreshing.value = true
            }
            _githubUserData.value =
                Event(Resource.loading(data = getGithubUser.value?.peekContent()?.data))
            try {
                val user = withContext(Dispatchers.IO) {
                    githubRepository.getUserByName(
                        name
                    )
                }
                _githubUserData.value = Event(
                    Resource.success(
                        user
                    )
                )

                if (isRefresh) {
                    _isRefreshing.value = false
                }

            } catch (e: Exception) {
                if (isRefresh) {
                    _isRefreshing.value = false
                }
                if (e is CancellationException) {
                    return@launch
                }
                _githubUserData.value = Event(
                    Resource.error(
                        data = getGithubUser.value?.peekContent()?.data,
                        message = e.message ?: "Error getting user!"
                    )
                )
            }
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun fakeInitialGithubUser(data: GithubUser) {
        _githubUserData.value = Event(Resource.success(data = data))
    }

}
