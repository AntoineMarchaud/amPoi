package com.amarchaud.ampoi.view

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.amarchaud.ampoi.R
import com.amarchaud.ampoi.adapter.SearchResultsAdapter
import com.amarchaud.ampoi.databinding.FragmentBookmarksBinding
import com.amarchaud.ampoi.interfaces.ILocationClickListener
import com.amarchaud.ampoi.model.app.VenueApp
import com.amarchaud.ampoi.viewmodel.BookmarksViewModel
import com.amarchaud.ampoi.viewmodel.data.VenueToDeleteViewModel
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BookmarksFragment : Fragment(), ILocationClickListener {

    companion object {
        const val TAG = "BookMarksFragment"
    }

    private var _binding: FragmentBookmarksBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BookmarksViewModel by viewModels()

    // recycler view
    private var venuesRecyclerAdapter = SearchResultsAdapter(this)

    // special viewModel
    private val venueToDelete: VenueToDeleteViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentBookmarksBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.bookmarksViewModel = viewModel
        binding.lifecycleOwner = this

        with(binding) {
            venuesRecyclerView.layoutManager =
                LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
            venuesRecyclerView.adapter = venuesRecyclerAdapter

            viewModel.poiBookmarkedLiveData.observe(viewLifecycleOwner, {
                venuesRecyclerAdapter.setLocationResults(it)
            })

            venueToDelete.venueToDelete.observe(viewLifecycleOwner, {
                if (it != null) {

                    // first method : refresh all
                    //viewModel.refresh()

                    // second method : just remove
                    venuesRecyclerAdapter.removeVenue(it)
                }
            })
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onLocationClicked(venueApp: VenueApp) {

        val sharedPref = requireContext().getSharedPreferences(
            getString(R.string.shared_pref),
            Context.MODE_PRIVATE
        )


        findNavController().navigate(
            BookmarksFragmentDirections.actionBookmarksFragmentToDetailsFragment(
                venueApp = venueApp,
                LatLon = LatLng(
                    Double.Companion.fromBits(
                        sharedPref.getLong(
                            getString(R.string.saved_location_lat),
                            0L
                        )
                    ),
                    Double.Companion.fromBits(
                        sharedPref.getLong(
                            getString(R.string.saved_location_lon),
                            0L
                        )
                    ),
                )
            )
        )
    }

    override fun onFavoriteClicked(venueApp: VenueApp) {

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.DeleteEntryTitle)
            .setMessage(R.string.DeleteEntryMessage)
            .setPositiveButton(android.R.string.ok) { dialog, which ->
                lifecycleScope.launch {
                    viewModel.deleteFavorite(venueApp)
                }
            }
            .setNegativeButton(android.R.string.cancel) { dialog, which ->
                lifecycleScope.launch {
                    viewModel.refresh()
                }
            }
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }
}