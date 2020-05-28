package com.example.speechrec2

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/*
class MyItemDetailsLookup(private val recyclerView: RecyclerView) :
    ItemDetailsLookup<Long>() {
    override fun getItemDetails(event: MotionEvent): ItemDetails<Long>? {
        val view = recyclerView.findChildViewUnder(event.x, event.y)
        if (view != null) {
            return (recyclerView.getChildViewHolder(view) as MyAdapter.MyViewHolder)
                .getItemDetails()
        }
        return null
    }
}*/
/**
 * @param myDataset - list of ShoppingListItems
 */
class MyAdapter (private val myDataset: ArrayList<ShoppingListItem>) :
    RecyclerView.Adapter<MyAdapter.MyViewHolder>() {
/*
    init {
        setHasStableIds(true)
    }*/

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder.
    // Each data item is just a string in this case that is shown in a TextView.
    class MyViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)
/*
    fun getItemDetails(): ItemDetailsLookup.ItemDetails<Long> =
        object : ItemDetailsLookup.ItemDetails<Long>() {
            override fun getPosition(): Int = position
            override fun getSelectionKey(): Long? = getItemId(position)
        }*/

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): MyAdapter.MyViewHolder {
        // create a new view
        val textView = LayoutInflater.from(parent.context)
            .inflate(R.layout.my_text_view, parent, false) as TextView
        // set the view's size, margins, paddings and layout parameters

        return MyViewHolder(textView)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        // if selected second time, strike through to mark collected. requires dataset that contains objects with string and bool members
        try {
            //if (myDataset != null) {//&& position >=0 && position < myDataset.size) {
                if (position > 0) {   // do not modify header
                    if (myDataset[position].collected) {
                        holder.textView.paintFlags =
                            holder.textView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    }
                    else {
                        holder.textView.paintFlags =
                            holder.textView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()   // TODO: check result
                    }
                }
                holder.textView.text = myDataset[position].title
            //}
        }
        catch(ex:Exception)
        {
            print(ex.message)
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = myDataset.size

    //override fun getItemId(position: Int): Long = position.toLong()


}