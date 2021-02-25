package com.amarchaud.ampoi.model.network.search

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class LabeledLatLng (
    var label: String? = null,
    var lat : Double = 0.0,
    var lng : Double = 0.0,
) : Parcelable