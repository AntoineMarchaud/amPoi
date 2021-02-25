package com.amarchaud.ampoi.model.network.search

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class Venue (
    var id: String? = null,
    var name: String? = null,
    var location: Location? = null,
    var categories: List<Category>? = null,
    var venuePage: VenuePage? = null
) : Parcelable