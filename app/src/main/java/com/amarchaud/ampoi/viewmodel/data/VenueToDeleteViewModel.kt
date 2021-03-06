package com.amarchaud.ampoi.viewmodel.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.amarchaud.ampoi.model.app.VenueApp

class VenueToDeleteViewModel : ViewModel() {

    private val _venueToDelete = MutableLiveData<VenueApp?>()
    val venueToDelete: LiveData<VenueApp?>
        get() = _venueToDelete

    fun setVenueToDelete(venueToDelete: VenueApp?) {
        _venueToDelete.value = venueToDelete
    }
}