package com.expeditee.plant.identifier.thesis

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseActivity
import com.expeditee.plant.identifier.common.BaseViewModel
import com.expeditee.plant.identifier.common.Constants
import com.expeditee.plant.identifier.databinding.ActivitySymptomRemedyBinding
import com.expeditee.plant.identifier.extension.collectLatestRepeatOnLifecycle
import com.expeditee.plant.identifier.network.model.FolkRemedyData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.androidx.viewmodel.ext.android.viewModel

class SymptomRemedyViewModel(
    application: android.app.Application,
) : BaseViewModel<SymptomRemedyUiState>(application) {
    private val _state = MutableStateFlow(SymptomRemedyUiState())
    val uiState = _state.asStateFlow()

    override fun onState(state: SymptomRemedyUiState) {
        _state.value = state
    }

    fun load(query: String?, intentId: String?) {
        val app = getApplication<android.app.Application>()
        val disclaimer = SymptomRemedyCatalog.disclaimer(app)
        val intent = when {
            !intentId.isNullOrBlank() -> SymptomRemedyCatalog.intentById(app, intentId)
            !query.isNullOrBlank() -> SymptomRemedyCatalog.matchIntent(app, query)
            else -> null
        }
        if (intent == null) {
            onState(
                SymptomRemedyUiState(
                    disclaimer = disclaimer,
                    lead = app.getString(R.string.thesis_symptom_pick_lead),
                    pickIntents = SymptomRemedyCatalog.allIntents(app),
                    showRemedies = false,
                ),
            )
            return
        }
        onState(
            SymptomRemedyUiState(
                disclaimer = disclaimer,
                lead = intent.title.ifBlank { query.orEmpty() },
                intentId = intent.id,
                questions = intent.questions,
                questionHints = intent.questionHints,
                remedies = emptyList(),
                showRemedies = false,
            ),
        )
    }

    fun onQuestionPicked() {
        val current = _state.value
        val id = current.intentId ?: return
        val app = getApplication<android.app.Application>()
        val intent = SymptomRemedyCatalog.intentById(app, id) ?: return
        onState(
            current.copy(
                remedies = SymptomRemedyCatalog.remediesFor(app, intent),
                showRemedies = true,
            ),
        )
    }

    fun selectIntent(id: String) {
        load(query = null, intentId = id)
    }
}

data class SymptomRemedyUiState(
    val disclaimer: String = "",
    val lead: String = "",
    val intentId: String? = null,
    val questions: List<String> = emptyList(),
    val questionHints: List<String> = emptyList(),
    val remedies: List<FolkRemedyData> = emptyList(),
    val pickIntents: List<SymptomIntent> = emptyList(),
    val showRemedies: Boolean = false,
) : BaseViewModel.State

class SymptomRemedyActivity :
    BaseActivity<ActivitySymptomRemedyBinding, SymptomRemedyViewModel, SymptomRemedyUiState>() {

    companion object {
        private const val EXTRA_QUERY = "extra_query"
        private const val EXTRA_INTENT_ID = "extra_intent_id"

        fun start(context: Context, query: String? = null, intentId: String? = null) {
            context.startActivity(
                Intent(context, SymptomRemedyActivity::class.java).apply {
                    putExtra(EXTRA_QUERY, query.orEmpty())
                    putExtra(EXTRA_INTENT_ID, intentId.orEmpty())
                },
            )
        }
    }

    private val adapter = SymptomFolkAdapter {
        val url = it.sourceUrl?.takeIf { u -> u.isNotBlank() } ?: Constants.DUOC_DIEN_URL
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    override fun provideLayout() = R.layout.activity_symptom_remedy
    override fun getLazyViewModel() = viewModel<SymptomRemedyViewModel>()
    override fun isFitsSystemWindows() = false

    override fun initViews(savedInstanceState: Bundle?) {
        applyInsetToView(viewBinding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, bars.top, 0, bars.bottom)
        }
        viewBinding.toolbar.setNavigationOnClickListener { finish() }
        viewBinding.rvRemedies.layoutManager = LinearLayoutManager(this)
        viewBinding.rvRemedies.adapter = adapter
        viewModel.load(
            query = intent.getStringExtra(EXTRA_QUERY),
            intentId = intent.getStringExtra(EXTRA_INTENT_ID)?.takeIf { it.isNotBlank() },
        )
    }

    override fun initObservers() {
        super.initObservers()
        collectLatestRepeatOnLifecycle(viewModel.uiState) { state ->
            viewBinding.tvDisclaimer.text = state.disclaimer
            viewBinding.tvLead.text = state.lead
            viewBinding.tvRemediesLabel.isVisible = state.showRemedies
            viewBinding.rvRemedies.isVisible = state.showRemedies || state.pickIntents.isNotEmpty()
            bindChips(state)
            if (state.pickIntents.isNotEmpty() && !state.showRemedies && state.intentId == null) {
                adapter.submitPick(state.pickIntents) { viewModel.selectIntent(it.id) }
            } else {
                adapter.submitRemedies(state.remedies)
            }
        }
    }

    private fun bindChips(state: SymptomRemedyUiState) {
        viewBinding.chipQuestions.removeAllViews()
        val chips = buildList {
            addAll(state.questions)
            addAll(state.questionHints)
        }
        val showQuestions = chips.isNotEmpty() && !state.showRemedies
        viewBinding.tvQuestionsLabel.isVisible = showQuestions
        viewBinding.scrollQuestions.isVisible = showQuestions
        viewBinding.chipQuestions.isVisible = showQuestions
        if (!showQuestions) return
        val green = ContextCompat.getColor(this, R.color.primary_green)
        val padH = (14 * resources.displayMetrics.density).toInt()
        val padV = (10 * resources.displayMetrics.density).toInt()
        val gap = (8 * resources.displayMetrics.density).toInt()
        chips.forEach { label ->
            val tv = TextView(this).apply {
                text = label
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTextColor(green)
                ResourcesCompat.getFont(
                    this@SymptomRemedyActivity,
                    R.font.montserrat_semibold,
                )?.let { typeface = it }
                setPadding(padH, padV, padH, padV)
                background = ContextCompat.getDrawable(this@SymptomRemedyActivity, R.drawable.bg_chip_selected)
                isClickable = true
                isFocusable = true
                setOnClickListener { viewModel.onQuestionPicked() }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { bottomMargin = gap }
            }
            viewBinding.chipQuestions.addView(tv)
        }
    }
}

private class SymptomFolkAdapter(
    private val onRemedyClick: (FolkRemedyData) -> Unit,
) : RecyclerView.Adapter<SymptomFolkAdapter.VH>() {
    private sealed interface Row {
        data class Remedy(val item: FolkRemedyData) : Row
        data class IntentPick(val item: SymptomIntent) : Row
    }

    private val rows = mutableListOf<Row>()
    private var onIntentClick: ((SymptomIntent) -> Unit)? = null

    fun submitRemedies(list: List<FolkRemedyData>) {
        rows.clear()
        rows.addAll(list.map { Row.Remedy(it) })
        onIntentClick = null
        notifyDataSetChanged()
    }

    fun submitPick(list: List<SymptomIntent>, onClick: (SymptomIntent) -> Unit) {
        rows.clear()
        rows.addAll(list.map { Row.IntentPick(it) })
        onIntentClick = onClick
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_thesis_row, parent, false)
        return VH(view)
    }

    override fun getItemCount() = rows.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        when (val row = rows[position]) {
            is Row.Remedy -> holder.bindRemedy(row.item, onRemedyClick)
            is Row.IntentPick -> holder.bindIntent(row.item, onIntentClick)
        }
    }

    class VH(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        fun bindRemedy(item: FolkRemedyData, onClick: (FolkRemedyData) -> Unit) {
            itemView.findViewById<TextView>(R.id.tvTitle).text = item.title
            itemView.findViewById<TextView>(R.id.tvSubtitle).text = buildString {
                append(item.ailment.orEmpty())
                append("\n")
                append(item.method.orEmpty())
                if (!item.caution.isNullOrBlank()) append("\n⚠ ${item.caution}")
            }
            itemView.setOnClickListener { onClick(item) }
        }

        fun bindIntent(item: SymptomIntent, onClick: ((SymptomIntent) -> Unit)?) {
            itemView.findViewById<TextView>(R.id.tvTitle).text = item.title
            itemView.findViewById<TextView>(R.id.tvSubtitle).text =
                item.triggers.take(3).joinToString(" · ")
            itemView.setOnClickListener { onClick?.invoke(item) }
        }
    }
}
