package com.amarchaud.ampoi.interfaces

import com.amarchaud.ampoi.model.entity.VenueEntity

interface ILocationClickListener {
    fun onLocationClicked(venueEntity: VenueEntity)
    fun onFavoriteClicked(venueEntity: VenueEntity)
}