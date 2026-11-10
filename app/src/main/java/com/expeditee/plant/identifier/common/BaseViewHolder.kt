package com.expeditee.plant.identifier.common

import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView

abstract class BaseViewHolder<T>(mBinding: ViewDataBinding) :
    RecyclerView.ViewHolder(mBinding.root) {
    
    abstract fun bindData(obj: T, position: Int)
    open fun onResize() {}
    open fun onClick(obj: T) {}
}