package com.amarchaud.ampoi.model.app

import android.os.Parcel
import android.os.Parcelable
import com.amarchaud.ampoi.interfaces.ILocationResult
import com.amarchaud.ampoi.model.network.search.Venue
import kotlinx.parcelize.Parcelize

/**
 * Transform Api Venue to App Venue
 */
class VenueModel(private val venue: Venue) : ILocationResult, Parcelable {

    val id = venue.id
    val locationName = venue.name
    val locationCategory = venue.categories?.firstOrNull()?.pluralName
    var locationIcon : String
    val locationDistance = venue.location?.distance
    val lat = venue.location?.lat
    val lng = venue.location?.lng
    var isFavorite: Boolean = false

    /**
     * Manage Parcelable datas
     */
    constructor(parcel: Parcel) : this(parcel.readParcelable<Venue>(
        Venue::class.java.classLoader)!!)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(venue, 0)
    }
    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<VenueModel> {
        override fun createFromParcel(parcel: Parcel): VenueModel {
            return VenueModel(parcel)
        }

        override fun newArray(size: Int): Array<VenueModel?> {
            return arrayOfNulls(size)
        }
    }


    init {
        locationIcon = buildIconPath(venue)
    }

    private fun buildIconPath(venue: Venue): String {
        venue.categories?.firstOrNull()?.icon?.let{
            if (!it.prefix.isNullOrBlank() && !it.suffix.isNullOrBlank()) {
                return it.prefix + "88" + it.suffix
            }
        }
        return ""
    }

    override fun areItemsSame(other: ILocationResult): Boolean {
        return other is VenueModel
    }

    override fun areContentsSame(other: ILocationResult): Boolean {
        val otherResult = other as VenueModel
        return  id == otherResult.id &&
                locationCategory == otherResult.locationCategory &&
                locationDistance == otherResult.locationDistance &&
                locationIcon == otherResult.locationIcon &&
                locationName == otherResult.locationName &&
                lat == otherResult.lat &&
                lng == otherResult.lng &&
                isFavorite == otherResult.isFavorite
    }
}