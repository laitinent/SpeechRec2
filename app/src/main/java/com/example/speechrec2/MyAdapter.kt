package com.example.speechrec2


import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


/**
 * @seealso https://www.skcript.com/svr/how-to-add-header-to-recyclerview-in-android/
 * TO ADD HEADER
 */

/**
 * @param myDataset - list of ShoppingListItems
 */
class MyAdapter (private val myDataset: ArrayList<ShoppingListItem>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_HEADER = 0
    private val TYPE_ITEM = 1
   // recyclerView - position=0 -> header
   //              - position=1 -> list[0]


    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder.
    // Each data item is just a string in this case that is shown in a TextView.
    class MyViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)


    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): RecyclerView.ViewHolder {

        return if(viewType==TYPE_ITEM) {
            // create a new view

            val textView = LayoutInflater.from(parent.context)
                .inflate(R.layout.my_text_view, parent, false) as TextView
            // set the view's size, margins, paddings and layout parameters
            MyViewHolder(textView)
        } else {
            val textView = LayoutInflater.from(parent.context)
                .inflate(R.layout.my_header_view, parent, false) as TextView
            MyHeaderViewHolder(textView)
        }

//        return super.createViewHolder(parent,viewType)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        when(holder) {
            is MyHeaderViewHolder -> apply {
                val headerViewHolder:MyHeaderViewHolder = holder
                /*
                if (myDataset[position].collected) {
                    holder.headerView.paintFlags =
                        holder.headerView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                }*/
                // TODO: maybe count could be added here
               // headerViewHolder.headerView.text = "Ostoslista"  // myDataset[position].title
            }
            is MyViewHolder -> apply {

                if (myDataset[position-1].collected) {
                    holder.textView.paintFlags =
                        holder.textView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                }
                else {
                    holder.textView.paintFlags =
                        holder.textView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()   // TODO: check result
                }
                holder.textView.text = myDataset[position-1].title
            }
        }
        /*
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
        }*/
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = myDataset.size+1

    //override fun getItemId(position: Int): Long = position.toLong()

    override fun getItemViewType(position: Int): Int {
        if (position == 0) {
            return TYPE_HEADER
        }
        return TYPE_ITEM
    }

    class MyHeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val headerView: TextView = view as TextView

        init {

        }
    }


}