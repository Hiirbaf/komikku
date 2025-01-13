package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isNotEmpty
import androidx.core.view.isVisible
import eu.kanade.tachiyomi.ui.reader.model.EpisodeTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderEpisode
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderProgressIndicator
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderTransitionView
import eu.kanade.tachiyomi.util.system.dpToPx
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR

/**
 * Holder of the webtoon viewer that contains a episode transition.
 */
class WebtoonTransitionHolder(
    val layout: LinearLayout,
    viewer: WebtoonViewer,
    // KMK -->
    @ColorInt private val seedColor: Int? = null,
    // KMK <--
) : WebtoonBaseHolder(layout, viewer) {

    private val scope = MainScope()
    private var stateJob: Job? = null

    private val transitionView = ReaderTransitionView(
        context,
        // KMK -->
        seedColor = seedColor,
        // KMK <--
    )

    /**
     * View container of the current status of the transition page. Child views will be added
     * dynamically.
     */
    private var pagesContainer = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
    }

    init {
        layout.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER

        val paddingVertical = 128.dpToPx
        val paddingHorizontal = 32.dpToPx
        layout.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical)

        val childMargins = 16.dpToPx
        val childParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            setMargins(0, childMargins, 0, childMargins)
        }

        layout.addView(transitionView)
        layout.addView(pagesContainer, childParams)
    }

    /**
     * Binds the given [transition] with this view holder, subscribing to its state.
     */
    fun bind(transition: EpisodeTransition) {
        transitionView.bind(transition, viewer.downloadManager, viewer.activity.viewModel.anime)

        transition.to?.let { observeStatus(it, transition) }
    }

    /**
     * Called when the view is recycled and being added to the view pool.
     */
    override fun recycle() {
        stateJob?.cancel()
    }

    /**
     * Observes the status of the page list of the next/previous episode. Whenever there's a new
     * state, the pages container is cleaned up before setting the new state.
     */
    private fun observeStatus(episode: ReaderEpisode, transition: EpisodeTransition) {
        stateJob?.cancel()
        stateJob = scope.launch {
            episode.stateFlow
                .collectLatest { state ->
                    pagesContainer.removeAllViews()
                    when (state) {
                        is ReaderEpisode.State.Loading -> setLoading()
                        is ReaderEpisode.State.Error -> setError(state.error, transition)
                        is ReaderEpisode.State.Wait, is ReaderEpisode.State.Loaded -> {
                            // No additional view is added
                        }
                    }
                    pagesContainer.isVisible = pagesContainer.isNotEmpty()
                }
        }
    }

    /**
     * Sets the loading state on the pages container.
     */
    private fun setLoading() {
        // KMK -->
        val progress = ReaderProgressIndicator(
            context = context,
            seedColor = seedColor,
        )
        // KMK <--

        val textView = AppCompatTextView(context).apply {
            wrapContent()
            text = context.stringResource(MR.strings.transition_pages_loading)
        }

        pagesContainer.addView(progress)
        pagesContainer.addView(textView)
    }

    /**
     * Sets the error state on the pages container.
     */
    private fun setError(error: Throwable, transition: EpisodeTransition) {
        val textView = AppCompatTextView(context).apply {
            wrapContent()
            text = context.stringResource(MR.strings.transition_pages_error, error.message ?: "")
        }

        val retryBtn = AppCompatButton(context).apply {
            wrapContent()
            text = context.stringResource(MR.strings.action_retry)
            setOnClickListener {
                val toEpisode = transition.to
                if (toEpisode != null) {
                    viewer.activity.requestPreloadEpisode(toEpisode)
                }
            }
        }

        pagesContainer.addView(textView)
        pagesContainer.addView(retryBtn)
    }
}
