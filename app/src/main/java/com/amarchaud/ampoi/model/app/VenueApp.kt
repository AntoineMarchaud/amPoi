package com.amarchaud.ampoi.model.app

import android.os.Parcel
import android.os.Parcelable
import com.amarchaud.ampoi.interfaces.ILocationResult
import com.amarchaud.ampoi.model.entity.VenueEntity
import com.amarchaud.ampoi.model.network.search.Venue

class VenueApp() : Parcelable, ILocationResult {

    var id: String? = null
    var locationName: String? = null
    var locationCategory: String? = null
    var locationIcon: String? = null
    var locationDistance: Int? = 0
    var lat: Double? = null
    var lng: Double? = null
    var isFavorite: Boolean = false


    constructor(parcel: Parcel) : this() {
        id = parcel.readString()
        locationName = parcel.readString()
        locationCategory = parcel.readString()
        locationIcon = parcel.readString()
        locationDistance = parcel.readValue(ClassLoader.getSystemClassLoader()) as Int?
        lat = parcel.readValue(ClassLoader.getSystemClassLoader()) as Double?
        lng = parcel.readValue(ClassLoader.getSystemClassLoader()) as Double?
        isFavorite = parcel.readByte() == 1.toByte()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(locationName)
        parcel.writeString(locationCategory)
        parcel.writeString(locationIcon)
        parcel.writeValue(locationDistance)
        parcel.writeValue(lat)
        parcel.writeValue(lng)
        parcel.writeByte(if (isFavorite) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<VenueApp> {
        override fun createFromParcel(parcel: Parcel): VenueApp {
            return VenueApp(parcel)
        }

        override fun newArray(size: Int): Array<VenueApp?> {
            return arrayOfNulls(size)
        }
    }


    // from api to app
    constructor(venue: Venue) : this() {
        id = venue.id
        locationName = venue.name
        locationCategory = venue.categories?.firstOrNull()?.pluralName
        locationIcon = buildIconPath(venue)
        locationDistance = venue.location?.distance
        lat = venue.location?.lat
        lng = venue.location?.lng
        isFavorite = false
    }

    // from database to app
    constructor(venueEntity: VenueEntity) : this() {
        id = venueEntity.id
        locationName = venueEntity.locationName
        locationCategory = venueEntity.locationCategory
        locationIcon = venueEntity.locationIcon
        locationDistance = venueEntity.locationDistance
        lat = venueEntity.lat
        lng = venueEntity.lng
        isFavorite = true // mandatory
    }


    private fun buildIconPath(venue: Venue): String {
        venue.categories?.firstOrNull()?.icon?.let {
            if (!it.prefix.isNullOrBlank() && !it.suffix.isNullOrBlank()) {
                return it.prefix + "88" + it.suffix
            }
        }
        return ""
    }


    override fun areItemsSame(other: ILocationResult): Boolean {
        return other is VenueApp
    }

    override fun areContentsSame(other: ILocationResult): Boolean {
        val otherResult = other as VenueApp
        return id == otherResult.id &&
                locationCategory == otherResult.locationCategory &&
                locationDistance == otherResult.locationDistance &&
                locationIcon == otherResult.locationIcon &&
                locationName == otherResult.locationName &&
                lat == otherResult.lat &&
                lng == otherResult.lng &&
                isFavorite == otherResult.isFavorite
    }
}