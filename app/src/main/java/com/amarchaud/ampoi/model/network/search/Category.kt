package com.amarchaud.ampoi.model.network.search

import android.os.Parcelable
import android.text.BoringLayout
import kotlinx.parcelize.Parcelize

@Parcelize
data class Category(
    var id: String? = null,
    var name: String? = null,
    var pluralName: String? = null,
    var shortName: String? = null,
    var icon: Icon? = null,
    var primary : Boolean = false
) : Parcelable
