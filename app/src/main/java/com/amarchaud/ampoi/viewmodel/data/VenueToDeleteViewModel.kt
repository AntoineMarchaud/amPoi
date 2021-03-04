package com.amarchaud.ampoi.viewmodel.data

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.amarchaud.ampoi.model.app.VenueApp

class VenueToDeleteViewModel: ViewModel() {
    data class VenueModified(val artist: VenueApp)
    val venueToDelete = MutableLiveData<VenueModified>()
}