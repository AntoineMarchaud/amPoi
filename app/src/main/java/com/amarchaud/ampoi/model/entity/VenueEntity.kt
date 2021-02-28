package com.amarchaud.ampoi.model.entity

import android.os.Parcelable
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.amarchaud.ampoi.interfaces.ILocationResult
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
    @ColumnInfo(name = "locationIcon") var locationIcon: String = "",
    @ColumnInfo(name = "locationDistance")  @Nullable val locationDistance: Int?,
    @ColumnInfo(name = "lat") @Nullable  val lat: Double? = null,
    @ColumnInfo(name = "lng") @Nullable  val lng: Double? = null
) : Parcelable, ILocationResult
{
    companion object {
        fun buildIconPath(venue: Venue): String {
            venue.categories?.firstOrNull()?.icon?.let {
                if (!it.prefix.isNullOrBlank() && !it.suffix.isNullOrBlank()) {
                    return it.prefix + "88" + it.suffix
                }
            }
            return ""
        }
    }

    override fun areItemsSame(other: ILocationResult): Boolean {
        return other is VenueEntity
    }

    override fun areContentsSame(other: ILocationResult): Boolean {
        val otherResult = other as VenueEntity
        return id == otherResult.id &&
                locationCategory == otherResult.locationCategory &&
                locationDistance == otherResult.locationDistance &&
                locationIcon == otherResult.locationIcon &&
                locationName == otherResult.locationName &&
                lat == otherResult.lat &&
                lng == otherResult.lng
    }
}