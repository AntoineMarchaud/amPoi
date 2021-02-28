package com.amarchaud.ampoi.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.amarchaud.ampoi.R
import com.amarchaud.ampoi.databinding.FragmentMapBinding
import com.amarchaud.ampoi.extensions.initMapView
import com.amarchaud.ampoi.model.entity.VenueEntity
import com.amarchaud.ampoi.viewmodel.MapViewModel
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker


@AndroidEntryPoint
class MapFragment : Fragment() {

    companion object {
        const val TAG = "MapFragment"
    }

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MapViewModel by viewModels()

    private val mapMarkerToData: HashMap<Marker, VenueEntity> = HashMap()

    private val args: MapFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {

            mapView.initMapView(GeoPoint(args.LatLon.latitude, args.LatLon.longitude))
            val positions: ArrayList<GeoPoint> = ArrayList()

            val myPositionMarker = Marker(binding.mapView)


            val geoPoint = GeoPoint(args.LatLon.latitude, args.LatLon.longitude)
            myPositionMarker.position = geoPoint
            myPositionMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            mapView.overlayManager.add(myPositionMarker)

            positions.add(myPositionMarker.position)


            // at each marker with info !
            args.venues.forEach {

                it.lat?.let { lat ->
                    it.lng?.let { lng ->
                        val mark =
                            MyMarker(binding.mapView)

                        mark.icon =
                            ContextCompat.getDrawable(requireContext(), R.drawable.map_marker)
                        mark.position = GeoPoint(lat, lng)
                        mark.title = it.locationName

                        if (!mapView.overlayManager.contains(mark))
                            mapView.overlayManager.add(mark)

                        positions.add(mark.position)

                        // save
                        mapMarkerToData[mark] = it
                    }

                }
            }

            // TODO Does not work !
            // find bounding box
            /*
            if (positions.isNotEmpty()) {
                val boundingBox = BoundingBox.fromGeoPointsSafe(positions)
                mapView.zoomToBoundingBox(boundingBox, true)
            }*/
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    inner class MyMarker(mapView: MapView) : Marker(mapView) {

        override fun onLongPress(event: MotionEvent?, mapView: MapView?): Boolean {
            val touched = hitTest(event, mapView)
            if (touched) {
                val venueModel = mapMarkerToData[this]
                venueModel?.id?.let { id ->
                    findNavController().navigate(
                        MapFragmentDirections.actionMapFragmentToDetailsFragment(
                            venueEntity = venueModel,
                            LatLon = LatLng(args.LatLon.latitude, args.LatLon.longitude) // current pos
                        )
                    )
                }
            }
            return touched
        }
    }

}