package com.amarchaud.ampoi.model.network.search

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class Location (
    var address: String? = null,
    var crossStreet: String? = null,
    var lat : Double = 0.0,
    var lng : Double = 0.0,
    var labeledLatLngs: List<LabeledLatLng>? = null,
    var distance : Int = 0,
    var postalCode: String? = null,
    var cc: String? = null,
    var city: String? = null,
    var state: String? = null,
    var country: String? = null,
    var formattedAddress: List<String>? = null,
) : Parcelable