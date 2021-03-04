package com.amarchaud.ampoi.viewmodel

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.amarchaud.ampoi.R
import com.amarchaud.ampoi.model.app.VenueApp
import com.amarchaud.ampoi.model.database.AppDao
import com.amarchaud.ampoi.model.entity.VenueEntity
import com.amarchaud.ampoi.model.network.search.SearchResponse
import com.amarchaud.ampoi.model.network.search.Venue
import com.amarchaud.ampoi.network.FoursquareApi
import com.amarchaud.ampoi.view.MainFragment
import com.google.android.gms.location.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val app: Application,
    private val myApi: FoursquareApi,
    private val myDao: AppDao
) : AndroidViewModel(app) {
    companion object {
        const val TAG = "MainViewModel"
        const val ERROR_CODE_RETRIEVE = 500
        const val ERROR_CODE_NO_CURRENT_LOCATION = 501
        const val ERROR_CODE_NOGPS = 502
        const val ERROR_PERMISSION = 503
    }

    var searchFilter: String = ""

    private var _venueModelsLiveData = MutableLiveData<ArrayList<VenueApp>>()
    val venueModelsLiveData: LiveData<ArrayList<VenueApp>>
        get() = _venueModelsLiveData

    private var _locationResultsError = MutableLiveData<Int>()
    val locationResultsError: LiveData<Int>
        get() = _locationResultsError


    private val locationProviderClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(app)
    }
    private var mLocationRequest: LocationRequest = LocationRequest.create()


    init {
        mLocationRequest.interval = MainFragment.UPDATE_TIME
        mLocationRequest.fastestInterval = MainFragment.UPDATE_TIME
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    var currentLocation: Location? = null

    /**
     * Methods called from View
     */
    fun setQuery(query: String) {
        searchFilter = query
        refresh()
    }

    fun refresh() {
        if (currentLocation == null) {
            _locationResultsError.postValue(ERROR_CODE_NO_CURRENT_LOCATION)
            return
        }

        getLocationResults(searchFilter)
    }

    /**
     * Private methods of the viewmodel
     */
    private fun getLocationResults(query: String) {

        if (query.isBlank()) {
            Log.d(TAG, "Query empty so no request will be made")
            _venueModelsLiveData.postValue(ArrayList())
            return
        }

        val currentLatLng =
            currentLocation!!.latitude.toString() + "," + currentLocation!!.longitude.toString()

        viewModelScope.launch {
            val response: SearchResponse? = try {
                myApi.getSearch(query = query, latlng = currentLatLng)
            } catch (e: Exception) {
                Log.e(TAG, "Error getSearch : ${e.printStackTrace()}")
                null
            }
            if (response?.response == null || response.response?.venues == null) {
                _locationResultsError.postValue(ERROR_CODE_RETRIEVE)
            } else {
                _venueModelsLiveData.postValue(buildResults(response.response?.venues!!))
            }
        }
    }

    /**
     * Build VenueModel from VenueApi
     */
    private suspend fun buildResults(venues: List<Venue>): ArrayList<VenueApp> {

        if (venues.isEmpty()) {
            return ArrayList()
        }

        val resultsList = ArrayList<VenueApp>()
        venues.forEach { venue ->

            venue.id?.let {
                resultsList.add(
                    VenueApp(venue).apply {
                        viewModelScope.launch {
                            id?.let {
                                isFavorite = myDao.getFavoriteById(it) != null
                            }
                        }.join() // very important to wait !
                    }
                )
            }
        }

        return ArrayList(resultsList.sortedBy { it.locationDistance })
    }

    fun onFavoriteClicked(venueApp: VenueApp) {

        viewModelScope.launch {

            val venueEntity = VenueEntity(venueApp)
            val favorite = myDao.getFavoriteById(venueEntity.id)
            if (favorite == null) {
                myDao.addFavorite(venueEntity)
            } else {
                myDao.removeFavorite(venueEntity)
            }

        }
    }

    fun startLocation() {

        if (ActivityCompat.checkSelfPermission(
                app,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                app,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            _locationResultsError.postValue(ERROR_PERMISSION)
            return
        }

        locationProviderClient.requestLocationUpdates(
            mLocationRequest,
            object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    for (location in locationResult.locations) {
                        currentLocation = location

                        currentLocation?.let {
                            val sharedPref = app.getSharedPreferences(
                                app.getString(R.string.shared_pref),
                                Context.MODE_PRIVATE
                            )

                            with(sharedPref.edit()) {
                                putLong(
                                    app.getString(R.string.saved_location_lat),
                                    it.latitude.toBits()
                                )
                                putLong(
                                    app.getString(R.string.saved_location_lon),
                                    it.longitude.toBits()
                                )
                                apply()
                            }
                        }
                    }
                }

                override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                    if (!locationAvailability.isLocationAvailable) {
                        currentLocation = null
                        _locationResultsError.postValue(ERROR_CODE_NOGPS)
                    } else {
                        _locationResultsError.postValue(-1)
                    }
                }
            },
            Looper.getMainLooper()
        )
    }
}