package com.amarchaud.ampoi.network

import com.amarchaud.ampoi.model.network.details.DetailsResponse
import com.amarchaud.ampoi.model.network.search.SearchResponse
import retrofit2.http.*

interface FoursquareApi {

    companion object {
        private const val CLIENT_ID = "ATSUR2UPOY5ZSCYRG4UI3KZQ2G5JG0BCDXDATOC22QND3GQL"
        private const val CLIENT_SECRET = "ULGITGGDBYQJSEQYKYX1X1KUGZLZT1BRCQ2C0POK2MN3HPG0"
        private const val VERSION = "20180401"
        private const val COMMON_PARAMS = "&client_id=$CLIENT_ID&client_secret=$CLIENT_SECRET&v=$VERSION"
        //47.606200,-122.332100     ll=47.606200,-122.332100&
    }

    // https://api.foursquare.com/v2/venues/search/limit=20&client_id=IK4QB0KWMKJMNIFDXJY5X51VUP4NPXSPV0K12D4Z3D5YZUQZ&client_secret=PGSIDON1C2TSKTSDNKIK4G2GSMCRMK1AXTKUJZWUU0GRWWEL&v=20180401"
    @GET("/v2/venues/search?limit=20$COMMON_PARAMS")
    suspend fun getSearch(@Query("query") query: String, @Query("ll") latlng: String): SearchResponse

    @GET("/v2/venues/{venue_id}/?$COMMON_PARAMS")
    suspend fun getDetails(@Path("venue_id") venueId: String): DetailsResponse
}