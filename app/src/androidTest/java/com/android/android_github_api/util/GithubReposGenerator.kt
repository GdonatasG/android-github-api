package com.android.android_github_api.util

import com.android.android_github_api.data.model.github.GithubRepo
import com.android.android_github_api.data.model.github.GithubUser

object GithubReposGenerator {
    fun generate(size: Int): ArrayList<GithubRepo> {
        val list: ArrayList<GithubRepo> = arrayListOf()
        val owner = GithubUser("", "", "", 1)
        for (i in 1..size) {
            list.add(GithubRepo(name = "repo $i", owner, "", html_url = "url $i", ""))
        }
        return list
    }
}