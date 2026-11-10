package com.expeditee.plant.identifier.widget.bottomsheet

import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.expeditee.plant.identifier.common.BaseBottomSheetDialogFragment
import com.expeditee.plant.identifier.databinding.FragmentManagementSiteBottomSheetBinding
import com.expeditee.plant.identifier.helper.SiteManager
import com.expeditee.plant.identifier.presenter.home.managementsite.ManagementSiteAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.extension.onClickWithScale
import com.expeditee.plant.identifier.model.GardenOption

class ManagementSiteBottomSheetDialog :
    BaseBottomSheetDialogFragment<FragmentManagementSiteBottomSheetBinding>(
        FragmentManagementSiteBottomSheetBinding::inflate
    ) {
    override val TAG: String = "ManagementSiteBottomSheetDialog"
    private var onManagementClick: (() -> Unit)? = null
    private var onSiteSelected: ((GardenOption) -> Unit)? = null
    private var currentSelectedSiteId: String = SiteManager.ALL_SITE_ID
    private val adapter by lazy {
        ManagementSiteAdapter(isSelectionMode = true, onSiteClick = { selectedSite ->
            currentSelectedSiteId = selectedSite.id
            onSiteSelected?.invoke(selectedSite)
            dismissAllowingStateLoss()
        })
    }

    fun setOnManagementClick(listener: () -> Unit): ManagementSiteBottomSheetDialog {
        onManagementClick = listener
        return this
    }

    fun setSelectedSiteId(siteId: String): ManagementSiteBottomSheetDialog {
        currentSelectedSiteId = siteId
        return this
    }

    fun setOnSiteSelected(listener: (GardenOption) -> Unit): ManagementSiteBottomSheetDialog {
        onSiteSelected = listener
        return this
    }

    override fun onStart() {
        super.onStart()
        isCancelable = false
        val bottomSheetDialog = dialog as? BottomSheetDialog ?: return
        val bottomSheet = bottomSheetDialog.findViewById<ViewGroup>(
            com.google.android.material.R.id.design_bottom_sheet
        ) ?: return
        val behavior = BottomSheetBehavior.from(bottomSheet)
        behavior.skipCollapsed = true
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.isDraggable = false
    }

    override fun dismissWhenTouchOutside(): Boolean = false

    override fun initViews() {
        viewBinding.rvSites.layoutManager = LinearLayoutManager(requireContext())
        viewBinding.rvSites.adapter = adapter
        val sites = SiteManager.getSites(requireContext())
        adapter.submitList(sites)
        adapter.setSelectedSiteId(currentSelectedSiteId)
    }

    override fun initObservers() = Unit

    override fun initListeners() {
        viewBinding.ivClose.onClickWithScale { dismissAllowingStateLoss() }
        viewBinding.btnManagementSite.onClickWithScale {
            dismissAllowingStateLoss()
            onManagementClick?.invoke()
        }
    }
}
