package io.github.edsuns.adblockclient.sample.settings

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.URLUtil
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.edsuns.adblockclient.sample.R
import io.github.edsuns.adblockclient.sample.databinding.ActivitySettingsBinding
import io.github.edsuns.adfilter.AdFilter
import io.github.edsuns.adfilter.FilterViewModel

/**
 * Created by Edsuns@qq.com on 2021/1/1.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    private lateinit var viewModel: FilterViewModel

    private lateinit var addFilterDialog: Dialog
    private lateinit var dialogView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel = AdFilter.get().viewModel

        val recyclerView = binding.filterRecyclerView
        val adapter = FilterListAdapter(this, layoutInflater)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        viewModel.filters.observe(this, {
            adapter.data = it.values.toList()
        })

        val enable = viewModel.isEnabled.value ?: false
        binding.enableSwitch.isChecked = enable
        setVisible(recyclerView, enable)
        binding.enableSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.isEnabled.value = isChecked
            setVisible(recyclerView, isChecked)
        }

        dialogView = layoutInflater.inflate(R.layout.dialog_add_filter, LinearLayout(this))
        addFilterDialog = AlertDialog.Builder(this)
            .setTitle(R.string.add_filter)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(
                android.R.string.ok
            ) { _, _ ->
                val urlEdit: EditText = dialogView.findViewById(R.id.filterUrlEdit)
                val url = urlEdit.text.toString()
                if (urlEdit.text.isNotBlank() && URLUtil.isNetworkUrl(url)) {
                    val filter = viewModel.addFilter("", url)
                    viewModel.download(filter.id)
                } else {
                    Toast.makeText(
                        this,
                        R.string.invalid_url, Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setView(dialogView)
            .create()

        viewModel.workToFilterMap.observe(this, { invalidateOptionsMenu() })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
            }
            R.id.menu_update -> {
                viewModel.filters.value?.keys?.forEach {
                    viewModel.download(it)
                }
            }
            R.id.menu_cancel -> {
                viewModel.filters.value?.keys?.forEach {
                    viewModel.cancelDownload(it)
                }
            }
            R.id.menu_add_filter -> {
                val urlEdit: EditText = dialogView.findViewById(R.id.filterUrlEdit)
                urlEdit.setText("")
                addFilterDialog.show()
            }
        }
        return true
    }

    private fun setVisible(view: View, visible: Boolean) {
        if (visible)
            view.visibility = View.VISIBLE
        else
            view.visibility = View.GONE
    }

    private fun setMenuDownloading(menu: Menu?, downloading: Boolean) {
        if (downloading) {
            menu?.findItem(R.id.menu_update)?.isVisible = false
            menu?.findItem(R.id.menu_cancel)?.isVisible = true
        } else {
            menu?.findItem(R.id.menu_update)?.isVisible = true
            menu?.findItem(R.id.menu_cancel)?.isVisible = false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_settings, menu)
        viewModel.workToFilterMap.value?.let {
            setMenuDownloading(menu, it.isNotEmpty())
        }
        return super.onCreateOptionsMenu(menu)
    }

}