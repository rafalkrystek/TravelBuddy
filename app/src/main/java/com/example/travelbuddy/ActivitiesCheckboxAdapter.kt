package com.example.travelbuddy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ActivitiesCheckboxAdapter(
    private val activities: List<String>,
    private val selectedActivities: MutableSet<String>,
    private val maxSelections: Int = 10,
    private val onSelectionChanged: (Set<String>) -> Unit
) : RecyclerView.Adapter<ActivitiesCheckboxAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkBox: CheckBox = view.findViewById(R.id.activityCheckBox)
        val textView: TextView = view.findViewById(R.id.activityTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_activity_checkbox, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val activity = activities[position]
        holder.textView.text = activity
        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = selectedActivities.contains(activity)
        holder.checkBox.isEnabled = selectedActivities.size < maxSelections || selectedActivities.contains(activity)
        
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (selectedActivities.size < maxSelections) {
                    selectedActivities.add(activity)
                    holder.itemView.post {
                        notifyItemRangeChanged(0, itemCount)
                        onSelectionChanged(selectedActivities)
                    }
                } else {
                    holder.checkBox.setOnCheckedChangeListener(null)
                    holder.checkBox.isChecked = false
                    holder.checkBox.setOnCheckedChangeListener { _, _ -> }
                }
            } else {
                selectedActivities.remove(activity)
                holder.itemView.post {
                    notifyItemRangeChanged(0, itemCount)
                    onSelectionChanged(selectedActivities)
                }
            }
        }
    }

    override fun getItemCount() = activities.size
}
