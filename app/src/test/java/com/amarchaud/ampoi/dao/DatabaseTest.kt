package com.amarchaud.ampoi.dao

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.amarchaud.ampoi.model.database.AppDao
import com.amarchaud.ampoi.model.entity.VenueEntity
import io.mockk.mockk
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@get:Rule
var instantExecutorRule = InstantTaskExecutorRule()

class DatabaseTest {
    private lateinit var favoriteDAO: AppDao

    @Before
    fun setup() {
        favoriteDAO = mockk()
    }

    @Test
    @Throws(Exception::class)
    fun writeFavoriteAndRead() {
        val favorite = VenueEntity("1", "Le grand monarque", "Restaurant", "", 100, 48.9, 1.5)
        GlobalScope.launch {
            favoriteDAO.addFavorite(favorite)
            val retrievedFav = favoriteDAO.getFavoriteById("10qsdqsdsqwdqsd")
            assertEquals(retrievedFav?.id, favorite.id)
        }
    }

    @Test
    @Throws(Exception::class)
    fun readNonFavorite() {
        GlobalScope.launch {
            val retrievedFav = favoriteDAO.getFavoriteById("345")
            assertNull(retrievedFav)
        }
    }

    @Test
    @Throws(Exception::class)
    fun writeFavoriteAndRemove() {
        GlobalScope.launch {
            val favorite =
                VenueEntity("id", "Le grand monarque", "Restaurant", "", 100, 48.9, 1.5)
            favoriteDAO.addFavorite(favorite)
            favoriteDAO.removeFavoriteById("1")
            assertNull(favoriteDAO.getFavoriteById("1"))
        }
    }
}