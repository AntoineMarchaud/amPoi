package com.amarchaud.ampoi


import android.app.AlertDialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.amarchaud.ampoi.databinding.ActivityMainBinding
import com.amarchaud.ampoi.view.BookmarksFragment
import com.amarchaud.ampoi.view.MainFragment
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.mainFragment, R.id.bookmarksFragment
            )
        )

        // nav host
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.my_first_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.mainFragment -> {

                }
                R.id.mapFragment -> {
                    supportActionBar?.let {
                        it.setDisplayHomeAsUpEnabled(true)
                        it.setDisplayShowTitleEnabled(false)
                    }
                }
                R.id.detailsFragment -> {
                    supportActionBar?.let {
                        it.setDisplayHomeAsUpEnabled(true)
                        it.setDisplayShowTitleEnabled(false)
                    }
                }
                else -> {
                }
            }
        }

        with(binding) {
            setSupportActionBar(toolbar)
            toolbar.setupWithNavController(navController, appBarConfiguration)

            // bottom nav
            bottomNav.setupWithNavController(navController)
        }
    }


    private fun getForegroundFragment(): Fragment? {
        val navHostFragment: Fragment? =
            supportFragmentManager.findFragmentById(R.id.my_first_host_fragment)
        return navHostFragment?.childFragmentManager?.fragments?.get(0)
    }

    override fun onBackPressed() {

        val currentFragment = getForegroundFragment()
        currentFragment?.let {

            if (it is MainFragment || it is BookmarksFragment) {
                AlertDialog.Builder(this)
                    .setTitle(R.string.exitAppTitle)
                    .setMessage(R.string.exitAppBody)
                    .setPositiveButton(android.R.string.ok) { dialog, which ->
                        finish()
                    }
                    .setNegativeButton(android.R.string.cancel) { dialog, which ->
                        dialog.dismiss()
                    }
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show()
            } else {
                super.onBackPressed()
            }
        } ?: super.onBackPressed()
    }
}