package com.amarchaud.ampoi.model.database

import androidx.room.*
import com.amarchaud.ampoi.model.entity.VenueEntity
import java.util.concurrent.Flow


@Dao
interface AppDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(venueEntity: VenueEntity)

    @Query("select * from favorites where id = :ID limit 1")
    suspend fun getFavoriteById(ID: String): VenueEntity?

    @Query("delete from favorites where id = :ID")
    suspend fun removeFavoriteById(ID: String)

    @Update
    suspend fun updateFavorite(venueEntity: VenueEntity)

    @Delete
    suspend fun removeFavorite(venueEntity: VenueEntity)

    @Query("select * from favorites")
    suspend fun getAllFavorites(): List<VenueEntity>
}