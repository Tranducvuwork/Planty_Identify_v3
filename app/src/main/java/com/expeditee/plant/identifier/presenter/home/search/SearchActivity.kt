package com.expeditee.plant.identifier.presenter.home.search

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseActivity
import com.expeditee.plant.identifier.common.OnPerformBackPressed
import com.expeditee.plant.identifier.databinding.ActivitySearchBinding
import com.expeditee.plant.identifier.extension.GridSpacingItemDecoration
import com.expeditee.plant.identifier.extension.beGone
import com.expeditee.plant.identifier.extension.beVisible
import com.expeditee.plant.identifier.extension.collectLatestRepeatOnLifecycle
import com.expeditee.plant.identifier.extension.hideKeyboard
import com.expeditee.plant.identifier.extension.onDebounceClick
import com.expeditee.plant.identifier.extension.show
import com.expeditee.plant.identifier.extension.showToast
import com.expeditee.plant.identifier.model.PlantCategory
import com.expeditee.plant.identifier.presenter.detail.PlantDetailActivity
import com.expeditee.plant.identifier.presenter.home.explore.PopularPlantAdapter
import com.expeditee.plant.identifier.thesis.SymptomRemedyCatalog
import com.expeditee.plant.identifier.thesis.SymptomRemedyActivity
import com.expeditee.plant.identifier.thesis.ThesisPlantCatalog
import com.expeditee.plant.identifier.thesis.ThesisPlantDetailActivity
import com.expeditee.plant.identifier.thesis.VoicePlantSearchHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class SearchActivity : BaseActivity<ActivitySearchBinding, SearchViewModel, SearchState>() {

    override fun getLazyViewModel() = viewModel<SearchViewModel>()

    override fun provideLayout() = R.layout.activity_search

    private val popularPlantAdapter by lazy { PopularPlantAdapter() }
    private val searchHistoryAdapter by lazy { SearchHistoryAdapter() }
    private val symptomSuggestionAdapter by lazy { SymptomSuggestionAdapter() }
    private var searchJob: Job? = null
    private var selectedCategory: String = ""

    companion object {
        private const val EXTRA_CATEGORY = "extra_category"
        private const val EXTRA_GARDEN_ID = "extra_garden_id"
        private const val EXTRA_LAUNCH_VOICE = "extra_launch_voice"

        fun start(
            context: Context,
            category: String? = null,
            gardenId: String? = null,
            launchVoice: Boolean = false,
        ) {
            context.startActivity(
                Intent(context, SearchActivity::class.java).apply {
                    putExtra(EXTRA_CATEGORY, category.orEmpty())
                    putExtra(EXTRA_GARDEN_ID, gardenId.orEmpty())
                    putExtra(EXTRA_LAUNCH_VOICE, launchVoice)
                }
            )
        }
    }

    private val voiceLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val spokenTexts = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            .orEmpty()
            .map { it.trim() }
            .filter { it.isNotBlank() }
        if (spokenTexts.isEmpty()) return@registerForActivityResult
        handleVoiceSearch(spokenTexts)
    }

    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            launchVoiceSearch()
        } else {
            showToast("Bạn cần cấp quyền micro để tìm kiếm bằng giọng nói")
        }
    }

    override fun isFitsSystemWindows(): Boolean = false

    override fun initAds(block: (suspend CoroutineScope.() -> Unit)?) {
        super.initAds {
             adsManager.onLoadCollapseBanner(this@SearchActivity)
        }
    }

    override fun initViews(savedInstanceState: Bundle?) {
        setupRecyclerView()
        setupHistoryRecyclerView()
        enableConfigEdgeSystemBar(false, isLight = false)
        viewBinding.ivPremiumBadge.show(false)

        popularPlantAdapter.onPlantClick = { plant ->
            showInterAdBeforeDoAction {
                PlantDetailActivity.start(
                    context = this@SearchActivity,
                    plantId = plant.id,
                    plantName = plant.name,
                    scientificName = plant.scientificName,
                    imageUrl = plant.imageUrl,
                    allowResolveFallback = true,
                )
            }
        }
//
//        viewBinding.etSearchPlants.requestFocus()
//        viewBinding.etSearchPlants.showKeyboard()

        selectedCategory = intent.getStringExtra(EXTRA_CATEGORY).orEmpty().trim()
        val category = PlantCategory.fromQuery(selectedCategory)
        if (category != null) {
            val title = category.getDisplayText(this)
            viewBinding.tvTitle.beVisible()
            viewBinding.tvTitle.text = title
            viewModel.onState(SearchState.Search(category.name))
        } else if (selectedCategory.isNotBlank()) {
            viewBinding.etSearchPlants.setText(selectedCategory)
            viewBinding.etSearchPlants.setSelection(selectedCategory.length)
            viewBinding.tvTitle.beVisible()
            viewBinding.tvTitle.text = selectedCategory
            viewModel.onState(SearchState.Search(selectedCategory))
        } else {
            viewBinding.tvTitle.text = getString(R.string.search)
        }

        if (intent.getBooleanExtra(EXTRA_LAUNCH_VOICE, false)) {
            viewBinding.root.post { requestVoiceSearch() }
        }
    }

    override fun initListeners() {
        super.initListeners()
        viewBinding.ivBack.onDebounceClick {
            showInterAdBeforeDoAction(Runnable { finish() })
        }
        viewBinding.ivPremiumBadge.onDebounceClick {
            
        }
        viewBinding.btnVoiceSearch.onDebounceClick {
            requestVoiceSearch()
        }

        with(viewBinding.etSearchPlants) {
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    val query = text.toString().trim()
                    if (!openLocalPlantIfMatched(query) && !openSymptomIfMatched(query)) {
                        viewModel.onState(SearchState.Search(query))
                    }
                    clearFocus()
                    hideKeyboard()
                }
                true
            }

            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?, start: Int, count: Int, after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?, start: Int, before: Int, count: Int
                ) {
                    searchJob?.cancel()
                    searchJob = lifecycleScope.launch {
                        delay(300)
                        val query = s.toString().trim()
                        if (query.isEmpty()) {
                            viewModel.onState(SearchState.ClearSearch)
                        } else {
                            viewModel.onState(SearchState.Search(query))
                        }
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })
        }

        searchHistoryAdapter.onHistoryClick = { query ->
            viewBinding.etSearchPlants.setText(query)
            viewBinding.etSearchPlants.setSelection(query.length)
            if (!openLocalPlantIfMatched(query) && !openSymptomIfMatched(query)) {
                viewModel.onState(SearchState.Search(query))
            }
        }
        symptomSuggestionAdapter.onSymptomClick = { symptom ->
            SymptomRemedyActivity.start(this, query = symptom.title, intentId = symptom.id)
        }
        viewBinding.root.onDebounceClick {
            viewBinding.etSearchPlants.hideKeyboard()
            viewBinding.etSearchPlants.clearFocus()
        }
        searchHistoryAdapter.onHistoryDelete = { historyModel ->
            viewModel.onState(SearchState.DeleteHistory(historyModel))
        }
    }

    private fun openSymptomIfMatched(query: String): Boolean {
        if (query.isBlank()) return false
        val intent = SymptomRemedyCatalog.matchIntent(this, query) ?: return false
        SymptomRemedyActivity.start(this, query = query, intentId = intent.id)
        return true
    }

    private fun openLocalPlantIfMatched(query: String): Boolean {
        if (query.isBlank()) return false
        val localPlant = VoicePlantSearchHelper.findLocalPlant(this, query)
            ?: ThesisPlantCatalog.findByName(this, query, listOf(query))
            ?: ThesisPlantCatalog.findByQrOrId(this, query)
            ?: return false
        ThesisPlantDetailActivity.startWithResult(
            context = this,
            result = ThesisPlantCatalog.toResult(localPlant),
        )
        return true
    }

    private fun requestVoiceSearch() {
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            launchVoiceSearch()
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun launchVoiceSearch() {
        showToast("Tôi đang lắng nghe... Hãy nói điều bạn muốn tìm hiểu về cây.")
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "vi-VN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Hãy nói điều bạn muốn tìm hiểu về cây")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        runCatching { voiceLauncher.launch(intent) }
            .onFailure { showToast("Thiết bị chưa hỗ trợ nhận giọng nói") }
    }

    private fun handleVoiceSearch(spokenTexts: List<String>) {
        val spokenText = spokenTexts.first()
        val query = VoicePlantSearchHelper.extractPlantQuery(spokenText)
        val localPlant = VoicePlantSearchHelper.findLocalPlant(this, spokenTexts)
            ?: ThesisPlantCatalog.findByName(this, query, listOf(query))
            ?: ThesisPlantCatalog.findByQrOrId(this, query)
        if (localPlant != null) {
            showToast("Đã nghe: $spokenText")
            ThesisPlantDetailActivity.startWithResult(
                context = this,
                result = ThesisPlantCatalog.toResult(localPlant),
            )
            return
        }

        if (openSymptomIfMatched(spokenText)) {
            showToast("Đã nghe: $spokenText")
            return
        }

        val searchQuery = query.ifBlank { spokenText }
        viewBinding.etSearchPlants.setText(searchQuery)
        viewBinding.etSearchPlants.setSelection(searchQuery.length)
        viewModel.onState(SearchState.Search(searchQuery))
        showToast("Đã nghe: $spokenText")
    }

    override fun onHandleBackPressed(onBackPressed: OnPerformBackPressed?) =
        super.onHandleBackPressed {
            showInterAdBeforeDoAction(Runnable { finish() })
        }

    override fun initObservers() {
        super.initObservers()
        collectLatestRepeatOnLifecycle(viewModel.searchResults) { plants ->
            val isSearching = viewModel.currentQuery.value.isNotBlank()
            val isLoading = viewModel.isLoading.value
            when {
                plants.isNotEmpty() && !isSearching -> {
                    popularPlantAdapter.submitList(plants)
                    viewBinding.rvPopularPlants.beVisible()
                    viewBinding.llEmpty.beGone()
                    viewBinding.tvPopular.beVisible()
                    loadAndShowHistory()
                }

                plants.isNotEmpty() && isSearching -> {
                    popularPlantAdapter.submitList(plants)
                    viewBinding.rvPopularPlants.beVisible()
                    viewBinding.llEmpty.beGone()
                    viewBinding.tvPopular.beVisible()
                    viewBinding.rvSearchHistory.beGone()
                    viewBinding.tvHistoryLabel.beGone()
                }

                !isSearching -> {
                    viewBinding.rvPopularPlants.beGone()
                    viewBinding.tvPopular.beGone()
                    viewBinding.llEmpty.beGone()
                    loadAndShowHistory()
                }

                isSearching -> {
                    viewBinding.rvPopularPlants.beGone()
                    if (isLoading) {
                        viewBinding.llEmpty.beGone()
                        viewBinding.rvSearchHistory.beGone()
                        viewBinding.tvHistoryLabel.beGone()
                    } else if (showSymptomSuggestions(viewModel.currentQuery.value)) {
                        viewBinding.llEmpty.beGone()
                    } else {
                        viewBinding.llEmpty.beVisible()
                        viewBinding.rvSearchHistory.beGone()
                        viewBinding.tvHistoryLabel.beGone()
                    }
                    viewBinding.tvPopular.beGone()
                }
            }
        }

        collectLatestRepeatOnLifecycle(viewModel.isLoading) { isLoading ->
            viewBinding.loadingContainer.show(isLoading)
            if (isLoading) {
                viewBinding.llEmpty.beGone()
            } else if (viewModel.currentQuery.value.isNotBlank() &&
                popularPlantAdapter.itemCount == 0 &&
                !showSymptomSuggestions(viewModel.currentQuery.value)
            ) {
                viewBinding.llEmpty.beVisible()
            }
        }

        collectLatestRepeatOnLifecycle(viewModel.errorEvent) { message ->
            showToast(message)
        }
    }

    private fun loadAndShowHistory() {
        collectLatestRepeatOnLifecycle(viewModel.searchHistory) { historyList ->
            if (historyList.isNotEmpty()) {
                if (viewBinding.rvSearchHistory.adapter !== searchHistoryAdapter) {
                    viewBinding.rvSearchHistory.adapter = searchHistoryAdapter
                }
                viewBinding.rvSearchHistory.beVisible()
                viewBinding.tvHistoryLabel.beVisible()
                viewBinding.tvHistoryLabel.text = getString(R.string.recent_searches)
                searchHistoryAdapter.submitList(historyList)
            } else {
                viewBinding.rvSearchHistory.beGone()
                viewBinding.tvHistoryLabel.beGone()
            }
        }
    }

    private fun showSymptomSuggestions(query: String): Boolean {
        if (VoicePlantSearchHelper.findLocalPlant(this, query) != null ||
            ThesisPlantCatalog.findByName(this, query, listOf(query)) != null ||
            ThesisPlantCatalog.findByQrOrId(this, query) != null
        ) {
            return false
        }
        val suggestions = SymptomRemedyCatalog.suggestions(this, query, limit = 6)
        if (suggestions.isEmpty()) return false
        if (viewBinding.rvSearchHistory.adapter !== symptomSuggestionAdapter) {
            viewBinding.rvSearchHistory.adapter = symptomSuggestionAdapter
        }
        symptomSuggestionAdapter.submitList(suggestions)
        viewBinding.tvHistoryLabel.text = getString(R.string.thesis_symptom_title)
        viewBinding.tvHistoryLabel.beVisible()
        viewBinding.rvSearchHistory.beVisible()
        return true
    }

    private fun setupRecyclerView() {
        with(viewBinding.rvPopularPlants) {
            layoutManager = GridLayoutManager(this@SearchActivity, 2)
            adapter = popularPlantAdapter
            setHasFixedSize(true)
            if (itemDecorationCount == 0) {
                addItemDecoration(
                    GridSpacingItemDecoration(
                        2, (8 * resources.displayMetrics.density).toInt()
                    )
                )
            }
        }
    }

    private fun setupHistoryRecyclerView() {
        with(viewBinding.rvSearchHistory) {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = searchHistoryAdapter
            setHasFixedSize(false)
        }
    }

    override fun onResume() {
        super.onResume()
        viewBinding.ivPremiumBadge.show(false)
    }
}
