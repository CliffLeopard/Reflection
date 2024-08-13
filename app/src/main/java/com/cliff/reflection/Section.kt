package com.cliff.reflection

import android.content.Intent
import android.view.View

data class Section(
    val title: String, val activity: Class<*> = Int::class.java, val action: (View) -> Unit = {
        val intent = Intent(it.context, activity)
        it.context.startActivity(intent)
    }
)
