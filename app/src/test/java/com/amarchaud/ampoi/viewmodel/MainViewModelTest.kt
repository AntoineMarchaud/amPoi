package com.amarchaud.ampoi.viewmodel

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.MutableLiveData
import com.amarchaud.ampoi.model.database.AppDao
import com.amarchaud.ampoi.model.entity.VenueEntity
import com.amarchaud.ampoi.network.FoursquareApi
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.times


class MainViewModelTest {
    private lateinit var viewModel: MainViewModel

    private lateinit var api: FoursquareApi
    private lateinit var dao: AppDao
    private lateinit var application: Application

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {

        api = mockk()
        dao = mockk()
        application = mockk()

        viewModel = MainViewModel(application, api, dao)
        viewModel.locationResultsError = MutableLiveData()
        viewModel.venueModelsLiveData = MutableLiveData()
    }

    @Test
    @Throws(Exception::class)
    fun test_setQuery() {
        viewModel.setQuery("query")
        assertEquals("query", viewModel.searchFilter)
    }

    @Test
    @Throws(Exception::class)
    fun test_refresh_EmptyQuery() {
        viewModel.venueModelsLiveData = mockk()
        viewModel.searchFilter = ""

        val observer = lambdaMock<(ArrayList<VenueEntity>) -> Unit>()
        val lifecycle = LifecycleRegistry(mock(LifecycleOwner::class.java))
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        viewModel.venueModelsLiveData.observe({ lifecycle }) {
            it?.let(observer)
        }

        viewModel.refresh()

        Mockito.verify(viewModel.venueModelsLiveData, times(1)).postValue(Mockito.any())
    }


    private inline fun <reified T> lambdaMock(): T = mockk()
}