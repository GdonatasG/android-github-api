package com.android.android_github_api.data.model.github

data class GithubRepoSearchData(
    val total_count: Int,
    val items: ArrayList<GithubRepo>
)