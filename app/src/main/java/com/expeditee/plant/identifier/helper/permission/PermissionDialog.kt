package com.expeditee.plant.identifier.helper.permission

import android.content.DialogInterface
import android.view.View
import com.expeditee.plant.identifier.common.BaseDialogFragment
import com.expeditee.plant.identifier.databinding.LayoutPermissionDialogBinding

class PermissionDialog(
    private val title: String,
    private val subTitle: String,
    private val positiveText: String? = null,
    private val negativeText: String? = null,
    private val onAllow: () -> Unit,
    private val onDeny: () -> Unit = {},
    private val dismissOnAction: Boolean = true
) : BaseDialogFragment<LayoutPermissionDialogBinding>(
    inflate = LayoutPermissionDialogBinding::inflate
) {
    constructor(
        title: String,
        subTitle: String,
        onAllow: () -> Unit
    ) : this(
        title = title,
        subTitle = subTitle,
        positiveText = null,
        negativeText = null,
        onAllow = onAllow,
        onDeny = {}
    )

    override val TAG: String
        get() = "RequestPermissionDialog"

    override fun initViews() {
        viewBinding.tvTitle.text = title
        viewBinding.tvSubTitle.text = subTitle

        viewBinding.tvAllow.text = positiveText ?: viewBinding.tvAllow.text

        val denyView = runCatching {
            viewBinding.root.findViewById<View>(
                resources.getIdentifier(
                    "tvDeny",
                    "id",
                    requireContext().packageName
                )
            )
        }.getOrNull()
        if (denyView != null) {
            if (negativeText.isNullOrEmpty()) {
                denyView.visibility = View.GONE
            } else {
                (denyView as? android.widget.TextView)?.text = negativeText
                denyView.visibility = View.VISIBLE
            }
        }
    }

    override fun initObservers() {}

    override fun initListeners() {

        viewBinding.tvAllow.setOnClickListener {
            it.isEnabled = false
            try {
                if (dismissOnAction) dismissAllowingStateLoss()
                onAllow()
            } finally {
                it.isEnabled = true
            }
        }
        val denyView = runCatching {
            viewBinding.root.findViewById<View>(
                resources.getIdentifier(
                    "tvDeny",
                    "id",
                    requireContext().packageName
                )
            )
        }.getOrNull()
        denyView?.setOnClickListener {
            it.isEnabled = false
            try {
                if (dismissOnAction) dismissAllowingStateLoss()
                onDeny()
            } finally {
                it.isEnabled = true
            }
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        onDeny()
    }
}