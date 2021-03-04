package com.amarchaud.ampoi.interfaces

import com.amarchaud.ampoi.model.app.VenueApp

interface ILocationClickListener {
    fun onLocationClicked(venueApp: VenueApp)
    fun onFavoriteClicked(venueApp: VenueApp)
}