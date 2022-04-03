package com.android.android_github_api.data.model.github

data class GithubRepo(
    val name: String,
    val owner: GithubUser,
    val description: String,
    val html_url: String,
    val language: String,
)
