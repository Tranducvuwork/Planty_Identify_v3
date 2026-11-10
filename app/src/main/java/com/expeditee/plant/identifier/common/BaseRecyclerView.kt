package com.expeditee.plant.identifier.common

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView

abstract class BaseRecyclerView<T> : RecyclerView.Adapter<BaseRecyclerView<T>.ViewHolder>() {

    abstract fun getItemLayout(): Int
    abstract fun submitData(newData: List<T>)
    abstract fun setData(binding: ViewDataBinding, item: T, position: Int)
    open fun onResizeView(binding: ViewDataBinding) {}
    open fun onClickView(binding: ViewDataBinding, obj: T, position: Int) {}

    val list: MutableList<T> = mutableListOf()
    var context: Context? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        return ViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                getItemLayout(), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindData(list[position], position)
    }

    override fun getItemCount(): Int {
        if (list.isNotEmpty()) {
            return list.size
        }
        return 0
    }

    fun updateData(newData: List<T>) {
        list.clear()
        list.addAll(newData)
        notifyDataSetChanged()
    }

    fun addData(newData: List<T>) {
        val startPosition = list.size
        list.addAll(newData)
        notifyItemRangeInserted(startPosition, newData.size)
    }

    fun addItem(item: T) {
        list.add(item)
        notifyItemInserted(list.size - 1)
    }

    fun removeItem(position: Int) {
        if (position in 0 until list.size) {
            list.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun clearData() {
        list.clear()
        notifyDataSetChanged()
    }

    fun getItem(position: Int): T? {
        return if (position in 0 until list.size) list[position] else null
    }

    inner class ViewHolder(var binding: ViewDataBinding) : BaseViewHolder<T>(binding) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = getItem(position)
                    item?.let { onClick(it) }
                }
            }
        }

        override fun bindData(obj: T, position: Int) {
            onResize()
            setData(binding, obj, position)
        }

        override fun onResize() {
            onResizeView(binding)
        }

        override fun onClick(obj: T) {
            val position = adapterPosition
            onClickView(binding, obj, position)
        }
    }
}