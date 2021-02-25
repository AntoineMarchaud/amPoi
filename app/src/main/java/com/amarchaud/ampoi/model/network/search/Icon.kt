package com.amarchaud.ampoi.model.network.search

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Icon (
    var prefix: String? = null,
    var suffix: String? = null
) : Parcelable