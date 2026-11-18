package com.renqing.zhang

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.fragment.app.FragmentActivity
import com.renqing.zhang.ui.AppRoot
import com.renqing.zhang.ui.LocalRepo
import com.renqing.zhang.ui.theme.RenqingTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as RenqingApp).repo
        setContent {
            RenqingTheme {
                CompositionLocalProvider(LocalRepo provides repository) {
                    AppRoot()
                }
            }
        }
    }
}
