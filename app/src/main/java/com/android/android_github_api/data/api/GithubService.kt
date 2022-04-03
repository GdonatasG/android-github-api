package com.android.android_github_api.data.api

import com.android.android_github_api.BuildConfig
import com.android.android_github_api.data.model.github.GithubRepoSearchData
import com.android.android_github_api.data.model.github.GithubUser
import retrofit2.http.*

interface GithubService {

    @GET("/users/{name}")
    suspend fun getUserByName(
        @Path("name") name: String,
        @Header("Authorization") apiKey: String = "Bearer ${BuildConfig.GITHUB_API_KEY}"
    ): GithubUser

    @GET("/search/repositories")
    suspend fun getRepos(
        @Query("q") query: String,
        @Query("per_page") perPage: Int,
        @Query("page") page: Int,
        @Header("Authorization") apiKey: String = "Bearer ${BuildConfig.GITHUB_API_KEY}"
    ): GithubRepoSearchData
}