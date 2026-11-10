package com.expeditee.plant.identifier.presenter.home.managementsite

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseActivity
import com.expeditee.plant.identifier.databinding.ActivitySitePlantsBinding
import com.expeditee.plant.identifier.extension.collectLatestRepeatOnLifecycle
import com.expeditee.plant.identifier.extension.gone
import com.expeditee.plant.identifier.extension.show
import com.expeditee.plant.identifier.model.ReminderScheduleModel
import com.expeditee.plant.identifier.presenter.detail.PlantDetailActivity
import com.expeditee.plant.identifier.presenter.home.mygardentab.MyGardenPlantAdapter
import com.expeditee.plant.identifier.presenter.home.mygardentab.MyGardenTabState
import com.expeditee.plant.identifier.presenter.home.mygardentab.MyGardenTabViewModel
import com.expeditee.plant.identifier.widget.bottomsheet.DeleteConfirmDialog
import com.expeditee.plant.identifier.widget.bottomsheet.EditReminderBottomSheetDialogFragment
import com.expeditee.plant.identifier.widget.bottomsheet.MoveSuccessDialog
import com.expeditee.plant.identifier.widget.bottomsheet.MoveToBottomSheetDialog
import org.koin.androidx.viewmodel.ext.android.viewModel

class SitePlantsActivity :
    BaseActivity<ActivitySitePlantsBinding, MyGardenTabViewModel, MyGardenTabState>() {

    companion object {
        private const val EXTRA_GARDEN_ID = "extra_garden_id"
        private const val EXTRA_GARDEN_NAME = "extra_garden_name"

        fun start(context: Context, gardenId: String, gardenName: String) {
            val intent = Intent(context, SitePlantsActivity::class.java).apply {
                putExtra(EXTRA_GARDEN_ID, gardenId)
                putExtra(EXTRA_GARDEN_NAME, gardenName)
            }
            context.startActivity(intent)
        }
    }

    private val gardenId: String by lazy { intent.getStringExtra(EXTRA_GARDEN_ID).orEmpty() }
    private val gardenName: String by lazy { intent.getStringExtra(EXTRA_GARDEN_NAME).orEmpty() }

    private val adapter by lazy {
        MyGardenPlantAdapter(
            onItemClick = { plant ->
                PlantDetailActivity.start(
                    context = this,
                    plantId = plant.id.toString(),
                    plantName = plant.name,
                    scientificName = plant.scientificName,
                    imageUrl = plant.imageFile,
                    gardenId = plant.gardenId
                )
            },
            onMove = { plant ->
                MoveToBottomSheetDialog.newInstance(plant)
                    .setOnMoveConfirmed { selectedPlant, gardenOption ->
                        viewModel.onState(MyGardenTabState.MovePlant(selectedPlant.copy(gardenId = gardenOption.id)))
                        MoveSuccessDialog(this).show()
                    }
                    .show(supportFragmentManager)
            },
            onDelete = { plant ->
                DeleteConfirmDialog(this) {
                    viewModel.onState(MyGardenTabState.DeletePlant(plant))
                }.show()
            },
            onAddReminder = { plant ->
                val prefilledReminder = ReminderScheduleModel(
                    plantId = plant.id,
                    plantName = plant.name,
                    imagePath = plant.imageFile,
                    remindType = getString(R.string.reminder_watering),
                    remindTime = "09:00",
                    repeatType = getString(R.string.reminder_every_day)
                )
                EditReminderBottomSheetDialogFragment.newInstance(prefilledReminder)
                    .show(supportFragmentManager)
            }
        )
    }

    override fun getLazyViewModel() = viewModel<MyGardenTabViewModel>()
    override fun provideLayout() = R.layout.activity_site_plants

    override fun initViews(savedInstanceState: Bundle?) {
        viewBinding.tvTitle.text = gardenName
        viewBinding.rvPlants.layoutManager = LinearLayoutManager(this)
        viewBinding.rvPlants.adapter = adapter

        val filterId = if (gardenId == "all") null else gardenId
        viewModel.onState(MyGardenTabState.SelectGarden(filterId))
    }

    override fun initObservers() {
        super.initObservers()
        collectLatestRepeatOnLifecycle(viewModel.myPlants) { pagingData ->
            adapter.submitData(pagingData)
        }

        adapter.addLoadStateListener { loadState ->
            val isEmpty = loadState.source.refresh is LoadState.NotLoading && adapter.itemCount == 0
            viewBinding.layoutEmpty.show(isEmpty)
            viewBinding.rvPlants.gone(isEmpty)
        }
    }

    override fun initListeners() {
        viewBinding.ivBack.setOnClickListener { finish() }
    }
}
