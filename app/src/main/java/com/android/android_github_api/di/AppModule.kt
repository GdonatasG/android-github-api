package com.android.android_github_api.di

import android.content.Context
import com.android.android_github_api.data.api.GithubService
import com.android.android_github_api.data.api.RetrofitBuilder
import com.android.android_github_api.data.repository.GithubRepository
import com.android.android_github_api.data.repository.IGithubRepository
import com.android.android_github_api.utils.ConnectivityManager
import com.bumptech.glide.Glide
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module(includes = [ViewModelModule::class])
@InstallIn(SingletonComponent::class)
object AppModule {
    @Singleton
    @Provides
    fun provideGithubService(): GithubService =
        RetrofitBuilder.getRetrofit().create(GithubService::class.java)

    @Singleton
    @Provides
    fun provideGithubRepository(githubService: GithubService) =
        GithubRepository(githubService) as IGithubRepository

    @Singleton
    @Provides
    fun provideGlideInstance(@ApplicationContext context: Context) =
        Glide.with(context)

    @Singleton
    @Provides
    fun provideConnectivityManager(@ApplicationContext context: Context) =
        ConnectivityManager(context)

}