package com.amarchaud.ampoi.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.amarchaud.ampoi.R
import com.amarchaud.ampoi.databinding.ItemLocationResultBinding
import com.amarchaud.ampoi.interfaces.ILocationClickListener
import com.amarchaud.ampoi.model.app.VenueApp
import com.amarchaud.ampoi.utils.DiffCallback
import com.bumptech.glide.Glide
import kotlin.math.roundToInt

class SearchResultsAdapter(private val onClickListener: ILocationClickListener) :
    RecyclerView.Adapter<SearchResultsAdapter.ItemLocationResultViewHolder>() {

    private var venueModels: MutableList<VenueApp> = mutableListOf()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ItemLocationResultViewHolder {
        val binding =
            ItemLocationResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemLocationResultViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemLocationResultViewHolder, position: Int) {

        val context = holder.itemView.context
        val venueApp = venueModels[position]

        // binding
        with(holder.binding) {

            //name and category display
            locationName.text = venueApp.locationName ?: ""
            locationCategory.text = venueApp.locationCategory ?: ""

            //distance display
            locationDistance.text = ""
            venueApp.locationDistance?.let { meters ->
                //if longer than a mile display miles
                if (meters >= 1000) {

                    val km = meters / 1000.0

                    locationDistance.text = context.resources.getQuantityString(
                        R.plurals.km, km.roundToInt(),
                        String.format("%.1f", km)
                    )
                } else {
                    locationDistance.text =
                        context.resources.getQuantityString(
                            R.plurals.meters,
                            meters,
                            meters.toString()
                        )
                }
            }





            if (venueApp.locationIcon.isNullOrEmpty()) {
                locationImage.setImageResource(R.drawable.unknown)
            } else {
                try {
                    Glide.with(context)
                        .load(venueApp.locationIcon) // use placeholder
                        .error(R.drawable.unknown)
                        .into(locationImage)
                } catch (e: IllegalArgumentException) {
                    locationImage.setImageResource(R.drawable.unknown)
                }
            }

            //set the initial state of the favorites icon by checking if its a favorite in the database
            setupFavoriteIndicator(holder.binding, venueApp, onClickListener)

            // callback to the entire view
            holder.itemView.setOnClickListener {
                onClickListener.onLocationClicked(venueApp)
            }
        }
    }


    override fun getItemCount(): Int {
        return venueModels.size
    }

    inner class ItemLocationResultViewHolder(var binding: ItemLocationResultBinding) :
        RecyclerView.ViewHolder(binding.root)

    fun removeVenue(venueAppToRemove: VenueApp) {
        venueModels.indexOfFirst {
            it.id == venueAppToRemove.id
        }.let { pos ->
            if (pos >= 0) {
                venueModels.removeAt(pos)
                notifyItemRemoved(pos)
            }
        }
    }

    /**
     * Callable from outside (View)
     * Automatically detect if view must be refreshed
     */
    fun setLocationResults(locations: MutableList<VenueApp>) {
        if (locations.isNullOrEmpty()) {
            venueModels = locations
            notifyDataSetChanged()
            return
        }

        val diffResult = DiffUtil.calculateDiff(DiffCallback(this.venueModels, locations), true)

        venueModels = locations

        // This will notify the adapter of what is new data, and will animate/update it for you ("this" being the adapter)
        diffResult.dispatchUpdatesTo(this)
    }

    private fun setupFavoriteIndicator(
        binding: ItemLocationResultBinding,
        venueApp: VenueApp,
        clickListener: ILocationClickListener
    ) {

        with(binding) {

            locationFavorite.isChecked = venueApp.isFavorite //set default

            //handle the status changes for favorites when the user clicks the star
            locationFavorite.setOnClickListener {
                venueApp.id.let {
                    venueApp.isFavorite = !venueApp.isFavorite
                    locationFavorite.isChecked = venueApp.isFavorite //set new value
                    clickListener.onFavoriteClicked(venueApp)
                }
            }
        }

    }
}