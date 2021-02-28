package com.amarchaud.ampoi.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
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

    var loadingLiveData : MutableLiveData<Boolean> = MutableLiveData()
    var poiBookmarkedLiveData: MutableLiveData<List<VenueEntity>> = MutableLiveData()

    init {
        viewModelScope.launch {
            loadingLiveData.postValue(true)
            poiBookmarkedLiveData.postValue(myDao.getAllFavorites())
            loadingLiveData.postValue(false)
        }
    }


    fun refresh() {
        viewModelScope.launch {
            poiBookmarkedLiveData.postValue(myDao.getAllFavorites())
        }
    }


}