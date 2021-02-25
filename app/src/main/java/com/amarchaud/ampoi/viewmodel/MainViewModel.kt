package com.amarchaud.ampoi.viewmodel

import android.app.Application
import android.location.Location
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.amarchaud.ampoi.model.app.VenueModel
import com.amarchaud.ampoi.model.database.AppDao
import com.amarchaud.ampoi.model.entity.Favorite
import com.amarchaud.ampoi.model.network.search.SearchResponse
import com.amarchaud.ampoi.model.network.search.Venue
import com.amarchaud.ampoi.network.FoursquareApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.lang.Exception
import java.util.concurrent.Executors
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    app: Application,
    private val myApi: FoursquareApi,
    private val myDao: AppDao
) : AndroidViewModel(app) {
    companion object {
        const val TAG = "MainViewModel"
        const val ERROR_CODE_RETRIEVE = 500
        const val ERROR_CODE_NO_CURRENT_LOCATION = 501
    }

    var searchFilter: String = ""

    //** LiveData send to view
    var venueModelsLiveData: MutableLiveData<ArrayList<VenueModel>> = MutableLiveData()
    var locationResultsError: MutableLiveData<Int> = MutableLiveData()

    /**
     * Methods called from View
     */
    fun setQuery(query: String, currentLocation: Location?) {
        searchFilter = query
        refresh(currentLocation)
    }

    fun refresh(currentLocation: Location?) {
        if (currentLocation == null) {
            locationResultsError.postValue(ERROR_CODE_NO_CURRENT_LOCATION)
            return
        }

        getLocationResults(searchFilter, currentLocation)
    }

    /*
    // todo DAO
    fun checkLocationResultsFavorites(context: Context, locations: ArrayList<LocationResult>, statusChangedListener: LocationFavoriteChanged) {
        Executors.newSingleThreadExecutor().submit {
            locations.forEach { location ->
                location.id?.let {id ->
                    FavoritesDatabase.database(context).favoritesDao().getFavoriteById(id).let {
                        if ((it == null && location.isFavorite) || (it != null && !location.isFavorite)) {
                            location.isFavorite = !location.isFavorite
                            statusChangedListener.onFavoriteChangedStatus()
                        }
                    }
                }
            }
        }
    }*/


    /**
     * Private methods of the viewmodel
     */
    private fun getLocationResults(query: String, currentLocation: Location) {

        if (query.isBlank()) {
            Log.d(TAG, "Query empty so no request will be made")
            venueModelsLiveData.postValue(ArrayList())
            return
        }
        val currentLatLng =
            currentLocation.latitude.toString() + "," + currentLocation.longitude.toString()

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
}