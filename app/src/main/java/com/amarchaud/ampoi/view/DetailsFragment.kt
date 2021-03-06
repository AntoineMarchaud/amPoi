package com.amarchaud.ampoi.view

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.amarchaud.ampoi.R
import com.amarchaud.ampoi.databinding.FragmentDetailsBinding
import com.amarchaud.ampoi.extensions.addMarker
import com.amarchaud.ampoi.extensions.initMapView
import com.amarchaud.ampoi.model.network.details.VenueDetail
import com.amarchaud.ampoi.utils.Errors
import com.amarchaud.ampoi.viewmodel.DetailsViewModel
import com.amarchaud.ampoi.viewmodel.data.VenueToDeleteViewModel
import com.google.android.material.snackbar.Snackbar
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import dagger.hilt.android.AndroidEntryPoint
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker


@AndroidEntryPoint
class DetailsFragment : Fragment() {

    private var _binding: FragmentDetailsBinding? = null
    private val binding get() = _binding!!

    private val args: DetailsFragmentArgs by navArgs()

    private val viewModel: DetailsViewModel by viewModels()

    private var snackBar: Snackbar? = null


    // special viewModel
    private val venueToDeleteViewModel: VenueToDeleteViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailsBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = this@DetailsFragment
        binding.model = viewModel

        // give param to the ViewModel !
        viewModel.venueApp = args.venueApp

        with(binding) {

            dismissSnackBar()
            viewModel.details.observe(viewLifecycleOwner, { venueDetail ->
                if (venueDetail == null) {
                    //no data returned which is indicative of an error case, so show an error message
                    activity?.let {
                        detailsCoordinator.let {
                            snackBar = Errors.showError(
                                it,
                                R.string.request_failed_details,
                                R.string.retry
                            ) {
                                dismissSnackBar()
                                viewModel.refresh()
                            }
                        }
                    }
                } else {
                    //data returned successfully so lets populate the screen
                    populateScreen(venueDetail)
                }
            })
            viewModel.refresh()

        }
    }


    /**
     * Dismiss any error message that is showing
     */
    private fun dismissSnackBar() {
        snackBar?.let {
            it.dismiss()
            snackBar = null
        }
    }

    /**
     * Populates the screen with the data retrieved from the API
     */
    private fun populateScreen(venueDetail: VenueDetail?) {
        if (venueDetail == null || !isAdded || context == null) {
            activity?.onBackPressed()
            return
        }

        with(binding) {
            //set the ratings bar to the color provided if its available
            context?.let {
                DrawableCompat.setTint(
                    detailsRatingBar.progressDrawable,
                    ContextCompat.getColor(it, R.color.disabled_grey)
                )
            }
            venueDetail.ratingColor?.let { ratingColor ->
                if (ratingColor != "null") {
                    DrawableCompat.setTint(
                        detailsRatingBar.progressDrawable,
                        Color.parseColor("#$ratingColor")
                    )
                }
            }

            //phone button
            val number = venueDetail.contact?.phone
            detailsPhone.setOnClickListener {
                handleCall(number ?: "")
            }

            //colorize the favorites based on if this location has been favorited by the user
            detailsIsFavorite.visibility = View.INVISIBLE

            venueDetail.id?.let { locationId ->
                context?.let { context ->

                    requireActivity().runOnUiThread {
                        detailsIsFavorite.setImageDrawable(
                            ContextCompat.getDrawable(
                                context,
                                if (args.venueApp.isFavorite)
                                    R.drawable.star_circle
                                else
                                    R.drawable.star_circle_disabled
                            )
                        )
                        detailsIsFavorite.visibility = View.VISIBLE
                    }

                }
            }
        }



        setupFavoriteAction(venueDetail)
        setupMap(venueDetail)
    }

    /**
     * add click listener for adding/removing favorite locale
     */
    private fun setupFavoriteAction(venueDetail: VenueDetail) {
        with(binding) {

            detailsIsFavorite.setOnClickListener {
                venueDetail.id?.let { locationId ->
                    context?.let { context ->

                        viewModel.onBookMarkedClick()

                        detailsIsFavorite.setImageDrawable(
                            ContextCompat.getDrawable(
                                requireContext(),
                                if (viewModel.venueApp.isFavorite)
                                    R.drawable.star_circle
                                else
                                    R.drawable.star_circle_disabled
                            )
                        )

                        if (viewModel.venueApp.isFavorite) {
                            view?.let {
                                Snackbar.make(
                                    it,
                                    getString(R.string.added_favorite),
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }

                        } else {

                            view?.let {
                                Snackbar.make(
                                    it,
                                    getString(R.string.removed_favorite),
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
        }

    }

    /**
     * Setup the map with current location and venue location pinned
     */
    private fun setupMap(venueDetail: VenueDetail) {

        with(binding) {

            mapView.initMapView(GeoPoint(args.LatLon.latitude, args.LatLon.longitude))
            val positions: ArrayList<GeoPoint> = ArrayList()

            // my position
            val myPositionMarker = Marker(binding.mapView)
            myPositionMarker.let { marker ->
                val geoPoint = GeoPoint(args.LatLon.latitude, args.LatLon.longitude)
                marker.position = geoPoint
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                mapView.overlayManager.add(marker)
            }

            positions.add(myPositionMarker.position)


            //if all fields exist pin the detail location
            venueDetail.location?.lat?.let { lat ->
                venueDetail.location?.lng?.let { lng ->
                    venueDetail.id?.let { id ->
                        val marker = mapView.addMarker(lat, lng, venueDetail.name, id)
                        positions.add(marker.position)
                    }
                }
            }

            // find bounding box
            if (positions.isNotEmpty()) {
                val boundingBox = BoundingBox.fromGeoPointsSafe(positions)
                mapView.zoomToBoundingBox(boundingBox, true)
            }
        }
    }

    private fun handleCall(number: String) {

        Dexter
            .withContext(requireContext())
            .withPermission(Manifest.permission.CALL_PHONE)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    if (number.isNotBlank()) {
                        startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")))
                    }
                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    binding.detailsPhone.isEnabled = false
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    p1: PermissionToken?
                ) {
                    p1?.continuePermissionRequest()
                }
            }).check()

    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null


        if (viewModel.venueApp.isFavorite) {
            venueToDeleteViewModel.setVenueToDelete(null)
        } else {
            venueToDeleteViewModel.setVenueToDelete(viewModel.venueApp)
        }
    }
}