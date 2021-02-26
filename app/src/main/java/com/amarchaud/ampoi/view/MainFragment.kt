package com.amarchaud.ampoi.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.view.marginBottom
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import com.amarchaud.ampoi.R
import com.amarchaud.ampoi.adapter.SearchResultsAdapter
import com.amarchaud.ampoi.databinding.FragmentMainBinding
import com.amarchaud.ampoi.interfaces.ILocationClickListener
import com.amarchaud.ampoi.model.app.VenueModel
import com.amarchaud.ampoi.model.database.AppDao
import com.amarchaud.ampoi.utils.Errors
import com.amarchaud.ampoi.viewmodel.MainViewModel
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class MainFragment : Fragment(), ILocationClickListener {

    companion object {
        const val TAG = "MainFragment"
        const val STATE_QUERY_STRING = "queryString"
        private const val PERMISSION_LOCATION_REQUEST_CODE = 579
        const val DEBOUNCE_DELAY = 500L // in milli
        const val UPDATE_TIME = 3000L // in milli
    }

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by viewModels()

    private var mLocationRequest: LocationRequest = LocationRequest.create()

    @VisibleForTesting
    private val locationProviderClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    @Inject
    lateinit var myDao: AppDao

    private var snackBar: Snackbar? = null
    private var searchView: SearchView? = null // will be setted in onPrepareOptionsMenu
    private var currentLocation: Location? = null // saved location from locationProviderClient

    private val searchResultAdapter: SearchResultsAdapter by lazy { SearchResultsAdapter(this) }

    init {
        mLocationRequest.interval = UPDATE_TIME
        mLocationRequest.fastestInterval = UPDATE_TIME
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        setHasOptionsMenu(true)

        // This callback will only be called when MyFragment is at least Started.
        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    searchView?.let {
                        if (!it.isIconified) {
                            it.onActionViewCollapsed()
                            return
                        }
                    }
                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        _binding = FragmentMainBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        handleGetLocation()

        // get last query
        savedInstanceState?.let {
            val query = it.getString(STATE_QUERY_STRING)
            query?.let { queryString ->
                viewModel.setQuery(queryString, currentLocation)
            }
        }

        searchResultAdapter.myDao = myDao

        with(binding) {

            mainToggleFullMap.setOnClickListener {
                viewModel.venueModelsLiveData.value?.let { locations: ArrayList<VenueModel> ->
                    currentLocation?.let { latLng ->
                        findNavController().navigate(
                            MainFragmentDirections.actionMainFragmentToMapFragment(
                                LatLng(latLng.latitude, latLng.longitude),
                                locations.toTypedArray(),
                            )
                        )
                    }

                }
            }

            mainLocationsRecycler.addItemDecoration(
                DividerItemDecoration(
                    requireContext(),
                    DividerItemDecoration.VERTICAL
                )
            )
            mainLocationsRecycler.adapter = searchResultAdapter

            mainSwipeRefresh.setOnRefreshListener {
                if (viewModel.searchFilter.isBlank()) {
                    mainSwipeRefresh.isRefreshing = false
                    return@setOnRefreshListener
                }

                dismissSnackBar()

                if (currentLocation == null) {
                    handleGetLocation()
                } else {
                    viewModel.refresh(currentLocation)
                }
            }

            /**
             * Called when API return something
             */
            viewModel.venueModelsLiveData.observe(viewLifecycleOwner, {
                //cancel the progress indicator

                mainSwipeRefresh.isRefreshing = false
                dismissSnackBar()

                if (it.isEmpty()) {
                    //set empty state
                    toggleEmptyState(true)
                } else {
                    //update adapter with new results
                    toggleEmptyState(false)

                    searchResultAdapter.setLocationResults(it)
                }

                setFullMapVisibleState(it)
            })


            /**
             * Called when auto geo loc not possible
             */
            viewModel.locationResultsError.observe(viewLifecycleOwner, {
                dismissSnackBar()
                if (it != null) {
                    when (it) {

                        MainViewModel.ERROR_CODE_RETRIEVE -> snackBar = Errors.showError(
                            mainCoordinator,
                            R.string.request_failed_main,
                            R.string.retry
                        ) {
                            // when click on snackbar
                            dismissSnackBar()
                            viewModel.refresh(currentLocation)
                        }
                        MainViewModel.ERROR_CODE_NO_CURRENT_LOCATION -> {

                            mainSwipeRefresh.isRefreshing = false

                            val lm =
                                requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
                            val gps_enabled = try {
                                lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                            } catch (ex: Exception) {
                                false
                            }

                            if (!gps_enabled) {
                                snackBar = Errors.showError(
                                    mainCoordinator,
                                    R.string.error_no_gps,
                                    R.string.close
                                ) {
                                    dismissSnackBar()
                                }
                            } else {
                                snackBar = Errors.showError(
                                    mainCoordinator,
                                    R.string.error_location_permission_denied,
                                    R.string.enable
                                ) {

                                    //launch app detail settings page to let the user enable the permission that they denied
                                    val intent = Intent()
                                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                    intent.data =
                                        Uri.fromParts(
                                            "package",
                                            requireActivity().packageName,
                                            null
                                        )
                                    startActivity(intent)
                                    requireActivity().finish()
                                }
                            }

                        }


                    }
                }
            })
        }
    }


    private fun handleGetLocation() {

        Log.d(TAG, "handleGetLocation")

        Dexter
            .withContext(requireContext())
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object : PermissionListener {

                @SuppressLint("MissingPermission")
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {

                    locationProviderClient.requestLocationUpdates(
                        mLocationRequest,
                        object : LocationCallback() {
                            override fun onLocationResult(locationResult: LocationResult) {
                                for (location in locationResult.locations) {
                                    currentLocation = location
                                    dismissSnackBar()
                                }
                            }
                        },
                        Looper.getMainLooper()
                    )

                    locationProviderClient.lastLocation.addOnSuccessListener {
                        currentLocation = it
                        dismissSnackBar()
                        viewModel.refresh(currentLocation)
                    }
                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {

                    dismissSnackBar()
                    AlertDialog.Builder(requireContext())
                        .setMessage(R.string.error_closing_no_location_permission)
                        .setPositiveButton(R.string.close) { _, _ -> /* **** */ }
                        .setNegativeButton(R.string.enable) { _, _ ->
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            intent.data =
                                Uri.fromParts("package", requireActivity().packageName, null)
                            startActivity(intent)
                        }
                        .setCancelable(false)
                        .create()
                        .show()
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    p1: PermissionToken?
                ) {
                    p1?.continuePermissionRequest()
                }

            }).check()


    }

    // *** Menu management
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu) // ajout de la loupe
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_location_search -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        menu.findItem(R.id.menu_item_location_search)?.let {
            (it.actionView as? SearchView).let { searchView ->
                searchView?.let { view ->

                    this.searchView = searchView
                    // to make sure it occupies the entire screen width as possible.
                    view.maxWidth = Integer.MAX_VALUE
                    view.queryHint = getString(R.string.search_locations)

                    // prepare a debounce to not call api all the time
                    setupQueryInputWatcher(view)
                }
            }
        }
    }


    private var mJobDebounce: Job? = null
    private var mPreviousQuery: String? = null
    private fun launchDebounce(query: String) = GlobalScope.launch {
        delay(DEBOUNCE_DELAY)
        setSearchQuery(query)
    }

    @SuppressLint("CheckResult")
    private fun setupQueryInputWatcher(searchView: SearchView) {


        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                if (mPreviousQuery != query) {
                    mJobDebounce?.cancel()
                    mJobDebounce = launchDebounce(query)
                }
                mPreviousQuery = query
                return true
            }

            override fun onQueryTextChange(query: String): Boolean {
                if (mPreviousQuery != query) {
                    mJobDebounce?.cancel()
                    mJobDebounce = launchDebounce(query)
                }
                mPreviousQuery = query
                return true
            }
        })
    }
    // *** Menu management end

    fun setSearchQuery(query: String) {

        searchView?.setQuery(query, true)

        requireActivity().runOnUiThread {
            if (query.isBlank()) {
                viewModel.setQuery("", currentLocation)
            } else {
                binding.mainSwipeRefresh.isRefreshing = true
                dismissSnackBar()
                viewModel.setQuery(query, currentLocation)
            }

            if (currentLocation == null) {
                dismissSnackBar()
                handleGetLocation()
            }
        }
    }


    private fun dismissSnackBar() {
        snackBar?.let {
            it.dismiss()
            snackBar = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }


    /**
     * Check if the query is empty or not and animate the full map action button to the proper location
     */
    private fun setFullMapVisibleState(venueModels: ArrayList<VenueModel>) {

        with(binding) {

            if (venueModels.isEmpty()) {
                mainToggleFullMap.animate()
                    .translationY((mainToggleFullMap.height + mainToggleFullMap.marginBottom).toFloat())
                    .setInterpolator(
                        AccelerateDecelerateInterpolator()
                    ).start()
                mainToggleFullMap.hide()
            } else {
                //results exist or the search query is not blank so
                mainToggleFullMap.show()
                mainToggleFullMap.animate().translationY(0f).setInterpolator(
                    AccelerateDecelerateInterpolator()
                ).start()
            }

        }
    }


    private fun toggleEmptyState(state: Boolean) {
        with(binding) {
            if (state) {
                mainSwipeRefresh.visibility = View.GONE
                groupEmptyData.visibility = View.VISIBLE

            } else {
                mainSwipeRefresh.visibility = View.VISIBLE
                groupEmptyData.visibility = View.GONE
            }
        }

    }

    /**
     * Called when click on on item
     */
    override fun onLocationClicked(id: String) {

        currentLocation?.let { currentLocation ->
            findNavController().navigate(
                MainFragmentDirections.actionMainFragmentToDetailsFragment(
                    id,
                    LatLng(currentLocation.latitude, currentLocation.longitude)
                )
            )
        }
    }

    override fun onFavoriteClicked(id: String) {
        viewModel.onFavoriteClicked(id)
    }
}