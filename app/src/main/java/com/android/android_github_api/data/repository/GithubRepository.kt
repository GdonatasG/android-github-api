package com.android.android_github_api.data.repository

import com.android.android_github_api.data.api.GithubService
import com.android.android_github_api.data.model.github.GithubRepoSearchData
import javax.inject.Inject

class GithubRepository @Inject constructor(private val githubService: GithubService) :
    IGithubRepository {
    override suspend fun getUserByName(name: String) = githubService.getUserByName(name)
    override suspend fun getRepos(
        user: String?,
        queryText: String?,
        language: String?,
        perPage: Int?,
        page: Int?
    ): GithubRepoSearchData {
        var query = ""
        if (queryText != null) {
            query += "$queryText"
        }
        if (user != null) {
            query += " user:${user}"
        }
        if (language != null) {
            query += " language:${language}"
        }
        return githubService.getRepos(
            query = query,
            perPage = perPage ?: 15,
            page = page ?: 1
        )
    }
}