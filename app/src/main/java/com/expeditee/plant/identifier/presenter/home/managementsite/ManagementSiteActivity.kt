package com.expeditee.plant.identifier.presenter.home.managementsite

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseActivity
import com.expeditee.plant.identifier.common.BaseViewModel
import com.expeditee.plant.identifier.common.CommonViewModel
import com.expeditee.plant.identifier.databinding.ActivityManagementSiteBinding
import com.expeditee.plant.identifier.helper.SiteManager
import com.expeditee.plant.identifier.model.GardenOption
import com.expeditee.plant.identifier.presenter.home.mygardentab.MyGardenTabState
import com.expeditee.plant.identifier.widget.bottomsheet.CreateSiteBottomSheetDialogFragment
import com.expeditee.plant.identifier.widget.bottomsheet.MoveSuccessDialog
import com.expeditee.plant.identifier.widget.bottomsheet.MoveToBottomSheetDialog
import org.koin.androidx.viewmodel.ext.android.viewModel

class ManagementSiteActivity :
    BaseActivity<ActivityManagementSiteBinding, CommonViewModel, BaseViewModel.State>() {

    private val siteAdapter by lazy {
        ManagementSiteAdapter(isSelectionMode = false, onSiteClick = { }, onArrowClick = { site ->
            showInterAdBeforeDoAction {
                SitePlantsActivity.start(this, site.id, site.name)
            }
        })
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, ManagementSiteActivity::class.java))
        }
    }

    override fun getLazyViewModel() = viewModel<CommonViewModel>()

    override fun provideLayout(): Int = R.layout.activity_management_site

    override fun initViews(savedInstanceState: Bundle?) {
        viewBinding.rvSites.layoutManager = LinearLayoutManager(this)
        viewBinding.rvSites.adapter = siteAdapter
    }

    override fun initListeners() {
        viewBinding.ivBack.setOnClickListener { finish() }
        viewBinding.btnCreateNewSite.setOnClickListener {
            CreateSiteBottomSheetDialogFragment { siteName ->
                SiteManager.createSite(this, siteName)
                loadSites()
            }.show(supportFragmentManager)
        }
    }

    override fun onResume() {
        super.onResume()
        loadSites()
    }

    private fun loadSites() {
        val sites = SiteManager.getSites(this).filter { it.id != SiteManager.ALL_SITE_ID }
        siteAdapter.submitList(sites)
    }
}
