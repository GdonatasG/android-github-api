package com.android.android_github_api.data.model.github

import com.google.gson.annotations.SerializedName

data class GithubUser(
    @SerializedName("login")
    val name: String,
    val avatar_url: String,
    val html_url: String,
    @SerializedName("public_repos")
    val public_repos_count: Int
)
