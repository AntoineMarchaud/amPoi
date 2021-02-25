package com.amarchaud.ampoi.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import com.amarchaud.ampoi.network.FoursquareApi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class MapViewModel @Inject constructor(
    app: Application,
    private val myApi: FoursquareApi
) : AndroidViewModel(app) {


}