package com.amarchaud.ampoi.model.database

import androidx.room.*
import com.amarchaud.ampoi.model.entity.Favorite


@Dao
interface AppDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: Favorite)

    @Query("select * from favorites where id = :ID limit 1")
    suspend fun getFavoriteById(ID: String): Favorite?

    @Query("delete from favorites where id = :ID")
    suspend fun removeFavoriteById(ID: String)
}