package com.amarchaud.ampoi.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.amarchaud.ampoi.model.app.VenueApp
import com.amarchaud.ampoi.model.database.AppDao
import com.amarchaud.ampoi.model.entity.VenueEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    val app: Application,
    val myDao: AppDao
) : AndroidViewModel(app) {

    private var _loadingLiveData = MutableLiveData<Boolean>()
    val loadingLiveData: LiveData<Boolean>
        get() = _loadingLiveData

    private var _poiBookmarkedLiveData= MutableLiveData<List<VenueApp>>()
    val poiBookmarkedLiveData: LiveData<List<VenueApp>>
        get() = _poiBookmarkedLiveData

    init {
        refresh()
    }


    fun refresh() {
        viewModelScope.launch {
            _loadingLiveData.postValue(true)
            _poiBookmarkedLiveData.postValue(myDao.getAllFavorites().map { VenueApp(it) })
            _loadingLiveData.postValue(false)
        }
    }

    fun deleteFavorite(venueApp: VenueApp) {

        if (venueApp.id == null)
            return

        viewModelScope.launch {
            val pos = myDao.getAllFavorites().indexOfFirst {
                it.id == venueApp.id
            }
            if (pos >= 0) {
                myDao.removeFavoriteById(venueApp.id!!)
                refresh()
            }
        }
    }
}