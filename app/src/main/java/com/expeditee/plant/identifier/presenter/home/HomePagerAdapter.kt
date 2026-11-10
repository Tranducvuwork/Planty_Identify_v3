package com.expeditee.plant.identifier.presenter.home

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.expeditee.plant.identifier.presenter.home.favorite.FavoriteTabFragment
import com.expeditee.plant.identifier.presenter.home.history.HistoryTabFragment
import com.expeditee.plant.identifier.presenter.home.uihome.HomeTabFragment

class HomePagerAdapter(
    activity: FragmentActivity,
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HomeTabFragment()
            1 -> HistoryTabFragment()
            2 -> FavoriteTabFragment()
            else -> MoreTabFragment()
        }
    }
}
