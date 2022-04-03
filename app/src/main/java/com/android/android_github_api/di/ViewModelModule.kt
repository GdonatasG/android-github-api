package com.android.android_github_api.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.android.android_github_api.ui.main.viewmodel.HomeViewModel
import com.android.android_github_api.ui.main.viewmodel.SearchViewModel
import com.android.android_github_api.ui.main.viewmodel.VmFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
abstract class ViewModelModule {
    @Binds
    @IntoMap
    @ViewModelKey(HomeViewModel::class)
    abstract fun bindHomeViewModel(homeViewModel: HomeViewModel): ViewModel

    @Binds
    abstract fun bindSearchViewModelAssistedFactory(factory: SearchViewModel.SearchViewModelFactory): SearchViewModel.SearchViewModelFactory

    @Binds
    abstract fun bindViewModelFactory(factory: VmFactory): ViewModelProvider.Factory
}