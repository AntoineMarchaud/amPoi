package com.amarchaud.ampoi.viewmodel.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.amarchaud.ampoi.model.app.VenueApp
import com.amarchaud.ampoi.utils.SingleLiveEvent

class VenueToDeleteViewModel : ViewModel() {

    private val _venueToDelete = SingleLiveEvent<VenueApp?>()
    val venueToDelete: LiveData<VenueApp?>
        get() = _venueToDelete

    fun setVenueToDelete(venueToDelete: VenueApp?) {
        _venueToDelete.value = venueToDelete
    }
}