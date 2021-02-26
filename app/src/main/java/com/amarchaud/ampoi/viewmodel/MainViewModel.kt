package com.amarchaud.ampoi.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.amarchaud.ampoi.R
import com.amarchaud.ampoi.model.app.VenueModel
import com.amarchaud.ampoi.model.database.AppDao
import com.amarchaud.ampoi.model.entity.Favorite
import com.amarchaud.ampoi.model.network.search.SearchResponse
import com.amarchaud.ampoi.model.network.search.Venue
import com.amarchaud.ampoi.network.FoursquareApi
import com.amarchaud.ampoi.view.MainFragment
import com.google.android.gms.location.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.lang.Exception
import java.util.concurrent.Executors
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
    var venueModelsLiveData: MutableLiveData<ArrayList<VenueModel>> = MutableLiveData()
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

    var currentLocation : Location? = null

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
    private fun buildResults(venues: List<Venue>): ArrayList<VenueModel> {

        if (venues.isEmpty()) {
            return ArrayList()
        }

        val resultsList = ArrayList<VenueModel>()
        venues.forEach { resultsList.add(VenueModel(it)) }
        return ArrayList(resultsList.sortedBy { it.locationDistance })
    }

    fun onFavoriteClicked(id: String) {

        viewModelScope.launch {
            val favorite = myDao.getFavoriteById(id)
            if (favorite == null) {
                myDao.addFavorite(Favorite(id))
            } else {
                myDao.removeFavoriteById(id)
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
                    }
                }

                override fun onLocationAvailability( locationAvailability : LocationAvailability) {
                    if(!locationAvailability.isLocationAvailable) {
                        currentLocation = null
                        locationResultsError.postValue(ERROR_CODE_NOGPS)
                    }
                }
            },
            Looper.getMainLooper()
        )
    }
}