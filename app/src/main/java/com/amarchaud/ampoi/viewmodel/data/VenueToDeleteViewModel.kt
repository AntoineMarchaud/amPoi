package com.amarchaud.amgraphqlartist.viewmodel.data

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.amarchaud.ampoi.model.entity.VenueEntity

class VenueToDeleteViewModel : ViewModel() {
    data class VenueToDelete(val artist: VenueEntity)
    val venueToDeleteLiveData = MutableLiveData<VenueToDelete>()
}