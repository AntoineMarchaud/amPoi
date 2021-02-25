package com.amarchaud.ampoi.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.amarchaud.ampoi.R
import com.amarchaud.ampoi.interfaces.ILocationClickListener
import com.amarchaud.ampoi.databinding.ItemLocationResultBinding
import com.amarchaud.ampoi.model.app.VenueModel
import com.amarchaud.ampoi.model.database.AppDao
import com.amarchaud.ampoi.utils.DiffCallback
import com.bumptech.glide.Glide
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class SearchResultsAdapter(private val onClickListener: ILocationClickListener) :
    RecyclerView.Adapter<SearchResultsAdapter.ItemLocationResultViewHolder>() {

    private var venueModels: List<VenueModel> = ArrayList()
    var myDao: AppDao? = null

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
        val venueModel = venueModels[position]

        // binding
        with(holder.binding) {

            //name and category display
            locationName.text = venueModel.locationName ?: ""
            locationCategory.text = venueModel.locationCategory ?: ""

            //distance display
            locationDistance.text = ""
            venueModel.locationDistance?.let { meters ->
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

                //load the image async with Glide so that the UI doesnt have to wait around on images to load (GlideConfig.kt)
                Glide.with(context).load(venueModel.locationIcon).into(locationImage)

                //set the initial state of the favorites icon by checking if its a favorite in the database
                setupFavoriteIndicator(holder.binding, venueModel, onClickListener)
            }

            // callback to the entire view
            holder.itemView.setOnClickListener {
                venueModel.id?.let {
                    onClickListener.onLocationClicked(it)
                }
            }
        }
    }


    override fun getItemCount(): Int {
        return venueModels.size
    }

    inner class ItemLocationResultViewHolder(var binding: ItemLocationResultBinding) :
        RecyclerView.ViewHolder(binding.root)


    /**
     * Callable from outside (View)
     * Automatically detect if view must be refreshed
     */
    fun setLocationResults(locations: List<VenueModel>) {
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
        locationResult: VenueModel,
        clickListener: ILocationClickListener
    ) {

        with(binding) {

            locationFavorite.isChecked = false //set default

            locationResult.id?.let { locationId ->

                GlobalScope.launch {
                    if (myDao != null) {
                        val favorite = myDao!!.getFavoriteById(locationId)
                        if (favorite != null) {
                            locationFavorite.isChecked = favorite.id == locationId
                        }
                    }
                }
            }

            //handle the status changes for favorites when the user clicks the star
            locationFavorite.setOnClickListener {
                locationResult.id?.let {
                    clickListener.onFavoriteClicked(it)
                }
            }
        }

    }
}