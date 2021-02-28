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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.amarchaud.ampoi.R
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

    //** LiveData send to view
    var venueModelsLiveData: MutableLiveData<ArrayList<VenueEntity>> = MutableLiveData()
    var locationResultsError: MutableLiveData<Int> = MutableLiveData()

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
            locationResultsError.postValue(ERROR_CODE_NO_CURRENT_LOCATION)
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
            venueModelsLiveData.postValue(ArrayList())
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
                locationResultsError.postValue(ERROR_CODE_RETRIEVE)
            } else {
                venueModelsLiveData.postValue(buildResults(response.response?.venues!!))
            }
        }
    }

    /**
     * Build VenueModel from VenueApi
     */
    private fun buildResults(venues: List<Venue>): ArrayList<VenueEntity> {

        if (venues.isEmpty()) {
            return ArrayList()
        }

        val resultsList = ArrayList<VenueEntity>()
        venues.forEach { venue ->

            venue.id?.let {
                resultsList.add(
                    VenueEntity(
                        venue.id!!,
                        venue.name,
                        venue.categories?.firstOrNull()?.pluralName,
                        VenueEntity.buildIconPath(venue),
                        venue.location?.distance ?: 0,
                        venue.location?.lat ?: 0.0,
                        venue.location?.lng ?: 0.0
                    )
                )
            }
        }

        return ArrayList(resultsList.sortedBy { it.locationDistance })
    }

    fun onFavoriteClicked(venueEntity: VenueEntity) {

        viewModelScope.launch {
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
            locationResultsError.postValue(ERROR_PERMISSION)
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
                        locationResultsError.postValue(ERROR_CODE_NOGPS)
                    } else {
                        locationResultsError.postValue(null)
                    }
                }
            },
            Looper.getMainLooper()
        )
    }
}