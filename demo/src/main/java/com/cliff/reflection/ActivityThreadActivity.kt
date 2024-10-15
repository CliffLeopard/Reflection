package com.cliff.reflection

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.cliff.reflection.databinding.ActivityThreadBinding
import com.orhanobut.logger.Logger

class ActivityThreadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityThreadBinding
    private val viewModel: ActivityThreadViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityThreadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = SectionAdapter(this)
        binding.list.layoutManager = LinearLayoutManager(this)
        binding.list.adapter = adapter
        binding.list.addItemDecoration(
            DividerItemDecoration(
                this@ActivityThreadActivity,
                DividerItemDecoration.VERTICAL
            )
        )
        viewModel.sections.observe(this) {
            Logger.d("observe data:" + it.size)
            adapter.submitList(it)
        }
    }
}