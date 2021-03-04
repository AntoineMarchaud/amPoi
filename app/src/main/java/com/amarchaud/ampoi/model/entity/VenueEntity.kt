package com.amarchaud.ampoi.model.entity

import android.os.Parcelable
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.amarchaud.ampoi.interfaces.ILocationResult
import com.amarchaud.ampoi.model.app.VenueApp
import com.amarchaud.ampoi.model.network.search.Venue
import kotlinx.parcelize.Parcelize

//locationCategory = venue.categories?.firstOrNull()?.pluralName
//locationDistance = venue.location?.distance ?: 0

@Parcelize
@Entity(tableName = "favorites")
data class VenueEntity(
    @PrimaryKey @NonNull @ColumnInfo(name = "id") var id: String,
    @ColumnInfo(name = "locationName") @Nullable val locationName: String? = null,
    @ColumnInfo(name = "locationCategory") val locationCategory: String? = null,
    @ColumnInfo(name = "locationIcon") @Nullable var locationIcon: String? = null,
    @ColumnInfo(name = "locationDistance") @Nullable val locationDistance: Int?,
    @ColumnInfo(name = "lat") @Nullable val lat: Double? = null,
    @ColumnInfo(name = "lng") @Nullable val lng: Double? = null
) : Parcelable {
    constructor(venueApp: VenueApp) : this(
        venueApp.id!!,
        venueApp.locationName,
        venueApp.locationCategory,
        venueApp.locationIcon ,
        venueApp.locationDistance,
        venueApp.lat,
        venueApp.lng
    )
}