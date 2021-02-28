package com.amarchaud.ampoi.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.amarchaud.ampoi.model.database.AppDao
import com.amarchaud.ampoi.model.entity.VenueEntity
import com.amarchaud.ampoi.model.network.details.VenueDetail
import com.amarchaud.ampoi.network.FoursquareApi
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import javax.inject.Inject

@HiltViewModel
class DetailsViewModel @Inject constructor(
    app: Application,
    private val myDao: AppDao,
    private val myApi: FoursquareApi,
) : AndroidViewModel(app) {

    companion object {
        const val TAG = "DetailsViewModel"
    }

    var isVenueInBookMarkedDb: MutableLiveData<Boolean> = MutableLiveData()

    var details: MutableLiveData<VenueDetail> = MutableLiveData()

    // given by screen
    lateinit var venueEntity : VenueEntity
    lateinit var currentLocation: LatLng
    //

    var venueName: MutableLiveData<String> = MutableLiveData()
    var venueRating: MutableLiveData<String> = MutableLiveData()
    var venueBar: MutableLiveData<Float> = MutableLiveData()
    var venueReviews: MutableLiveData<Int> = MutableLiveData()

    var venueHours: MutableLiveData<String> = MutableLiveData()
    var venueAddress: MutableLiveData<String> = MutableLiveData()
    var venueCategory: MutableLiveData<String> = MutableLiveData()
    var venueWebsite: MutableLiveData<String> = MutableLiveData()
    var venuePhone: MutableLiveData<String> = MutableLiveData()

    var error: MutableLiveData<String> = MutableLiveData()


    /**
     * Asynchronously request the location details
     */
    fun refresh() {

        if(venueEntity.id.isBlank())
            return

        viewModelScope.launch {
            val result = try {
                myApi.getDetails(venueEntity.id)
            } catch (e: Exception) {
                Log.d(TAG, "error when calling getDetails : $e")
                error.postValue(e.message)
                null
            }

            if (result?.response != null && result.response?.venue != null) {

                result.response?.venue?.let { venue ->
                    //successful response so parse the results and post them to the awaiting live data
                    details.postValue(venue)

                    //post to live datas bound fields
                    venueName.value = venue.name
                    venueRating.value = formatRatings(venue)
                    venueBar.value = ratingBar(venue)
                    //venueBarColor.postValue(ratingBarColor(venue.ratingColor))
                    venueReviews.value = venue.ratingSignals
                    venueHours.value = venue.hours?.status ?: ""
                    venueAddress.value = address(venue)
                    venueCategory.value = category(venue)
                    venueWebsite.value = website(venue)
                    venuePhone.value = phone(venue)
                }

            } else {
                error.postValue("Invalid data for location details")
            }

        }
    }

    private fun formatRatings(venueDetail: VenueDetail): String {
        return DecimalFormat("#.##").format(venueDetail.rating?.div(2) ?: 0) ?: "0"
    }

    private fun ratingBar(venueDetail: VenueDetail): Float {
        return venueDetail.rating?.div(2)?.toFloat() ?: 0f
    }

    /*private fun ratingBarColor(ratingColor: String?): Int {
        return Color.parseColor("#$ratingColor")
    }*/

    private fun address(venueDetail: VenueDetail): String {
        if (venueDetail.location == null || venueDetail.location?.formattedAddress.isNullOrEmpty()) {
            return ""
        }

        var addressString = ""
        venueDetail.location?.formattedAddress.let {
            it?.forEach { s ->
                addressString += s + System.getProperty("line.separator")
            }
        }

        return addressString
    }

    private fun category(venueDetail: VenueDetail): String {

        if (venueDetail.categories.isNullOrEmpty())
            return ""

        return venueDetail.categories?.get(0)?.shortName ?: ""
    }

    private fun website(venueDetail: VenueDetail): String {
        return if (venueDetail.canonicalUrl.isNullOrBlank()) {
            venueDetail.shortUrl ?: ""
        } else {
            venueDetail.canonicalUrl ?: ""
        }
    }

    private fun phone(venueDetail: VenueDetail): String {
        val contact = venueDetail.contact
        return contact?.formattedPhone ?: ""
    }

    fun websiteVisibility(website: String?): Int {
        return if (website.isNullOrBlank()) View.INVISIBLE else View.VISIBLE
    }

    fun phoneVisibility(phone: String?): Int {
        return if (phone.isNullOrBlank()) View.INVISIBLE else View.VISIBLE
    }

    fun onWebsiteClick(context: Context, url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    fun onBookMarkedClick() {
        viewModelScope.launch {
            val toDelete = myDao.getFavoriteById(venueEntity.id)
            if (toDelete == null) {
                myDao.addFavorite(venueEntity)
                isVenueInBookMarkedDb.postValue(true)
            } else {
                myDao.removeFavorite(venueEntity)
                isVenueInBookMarkedDb.postValue(false)
            }
        }
    }
}