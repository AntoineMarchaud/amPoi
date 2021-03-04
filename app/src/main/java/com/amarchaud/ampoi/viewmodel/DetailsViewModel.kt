package com.amarchaud.ampoi.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.amarchaud.ampoi.model.app.VenueApp
import com.amarchaud.ampoi.model.database.AppDao
import com.amarchaud.ampoi.model.entity.VenueEntity
import com.amarchaud.ampoi.model.network.details.VenueDetail
import com.amarchaud.ampoi.network.FoursquareApi
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

    private var _details = MutableLiveData<VenueDetail>()
    val details: LiveData<VenueDetail>
        get() = _details


    // given by screen
    lateinit var venueApp: VenueApp
    //

    private var _venueName = MutableLiveData<String>()
    val venueName: LiveData<String>
        get() = _venueName

    private var _venueRating = MutableLiveData<String>()
    val venueRating: LiveData<String>
        get() = _venueRating

    private var _venueBar = MutableLiveData<Float>()
    val venueBar: LiveData<Float>
        get() = _venueBar

    private var _venueReviews = MutableLiveData<Int>()
    val venueReviews: LiveData<Int>
        get() = _venueReviews

    private var _venueHours = MutableLiveData<String>()
    val venueHours: LiveData<String>
        get() = _venueHours

    private var _venueAddress = MutableLiveData<String>()
    val venueAddress: LiveData<String>
        get() = _venueAddress

    private var _venueCategory = MutableLiveData<String>()
    val venueCategory: LiveData<String>
        get() = _venueCategory

    private var _venueWebsite = MutableLiveData<String>()
    val venueWebsite: LiveData<String>
        get() = _venueWebsite

    private var _venuePhone = MutableLiveData<String>()
    val venuePhone: LiveData<String>
        get() = _venuePhone

    private var _error = MutableLiveData<String>()
    val error: LiveData<String>
        get() = _error


    /**
     * Asynchronously request the location details
     */
    fun refresh() {

        if (venueApp.id.isNullOrEmpty())
            return

        viewModelScope.launch {
            val result = try {
                myApi.getDetails(venueApp.id!!)
            } catch (e: Exception) {
                Log.d(TAG, "error when calling getDetails : $e")
                _error.postValue(e.message)
                null
            }

            result?.response?.venue?.let { venue ->
                //successful response so parse the results and post them to the awaiting live data
                _details.postValue(venue)

                //post to live datas bound fields
                _venueName.postValue(venue.name)
                _venueRating.postValue(formatRatings(venue))
                _venueBar.postValue(ratingBar(venue))
                //venueBarColor.postValue(ratingBarColor(venue.ratingColor))
                _venueReviews.postValue(venue.ratingSignals)
                _venueHours.postValue(venue.hours?.status ?: "")
                _venueAddress.postValue(address(venue))
                _venueCategory.postValue(category(venue))
                _venueWebsite.postValue(website(venue))
                _venuePhone.postValue(phone(venue))
            } ?: _error.postValue("Invalid data for location details")


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

        if (venueApp.id.isNullOrEmpty())
            return

        venueApp.isFavorite = !venueApp.isFavorite

        viewModelScope.launch {
            val toDelete = myDao.getFavoriteById(venueApp.id!!)
            if (toDelete == null) {
                myDao.addFavorite(VenueEntity(venueApp))
            } else {
                myDao.removeFavorite(toDelete)
            }
        }
    }
}