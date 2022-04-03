package com.android.android_github_api.data.repository

import com.android.android_github_api.data.model.github.GithubRepoSearchData
import com.android.android_github_api.data.model.github.GithubUser

interface IGithubRepository {
    suspend fun getUserByName(name: String): GithubUser
    suspend fun getRepos(
        user: String?,
        queryText: String?,
        language: String?,
        perPage: Int?,
        page: Int?
    ): GithubRepoSearchData
}