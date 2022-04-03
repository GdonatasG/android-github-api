package com.android.android_github_api

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HiltTestActivity : AppCompatActivity() {
    fun setupActionBar(toolbar: Toolbar){
        setSupportActionBar(toolbar)
    }
}