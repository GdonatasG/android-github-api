package com.android.android_github_api.ui.main

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.lifecycle.ViewModelProvider
import com.android.android_github_api.ui.main.adapter.GithubReposAdapter
import com.android.android_github_api.ui.main.view.HomeFragment
import com.android.android_github_api.ui.main.view.SearchFragment
import com.android.android_github_api.ui.main.view.WebViewFragment
import com.android.android_github_api.ui.main.viewmodel.SearchViewModel
import com.bumptech.glide.RequestManager
import javax.inject.Inject

class TestCustomFragmentFactory @Inject constructor(
    private val glide: RequestManager,
    private val githubReposAdapter: GithubReposAdapter,
    var viewModelFactory: ViewModelProvider.Factory? = null,
    var searchViewModelAssistedFactory: SearchViewModel.AssistedSearchViewModelFactory? = null,
) :
    FragmentFactory() {

    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        return when (className) {
            HomeFragment::class.java.name -> HomeFragment(
                glide,
                viewModelFactory!!
            )
            WebViewFragment::class.java.name -> WebViewFragment()
            SearchFragment::class.java.name -> SearchFragment(
                githubReposAdapter,
                searchViewModelAssistedFactory!!
            )
            else -> super.instantiate(classLoader, className)
        }
    }
}