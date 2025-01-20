package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.tachiyomi.animesource.model.AnimeFilter
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.source.model.EXHSavedSearch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.CollapsibleBox
import tachiyomi.presentation.core.components.HeadingItem
import tachiyomi.presentation.core.components.SelectItem
import tachiyomi.presentation.core.components.SortItem
import tachiyomi.presentation.core.components.TextItem
import tachiyomi.presentation.core.components.TriStateItem
import tachiyomi.presentation.core.components.material.Button
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun SourceFilterDialog(
    onDismissRequest: () -> Unit,
    filters: AnimeFilterList,
    onReset: () -> Unit,
    onFilter: () -> Unit,
    onUpdate: (AnimeFilterList) -> Unit,
    // SY -->
    startExpanded: Boolean,
    savedSearches: ImmutableList<EXHSavedSearch>,
    onSave: () -> Unit,
    onSavedSearch: (EXHSavedSearch) -> Unit,
    onSavedSearchPress: (EXHSavedSearch) -> Unit,
    // KMK -->
    onSavedSearchPressDesc: String,
    shouldShowSavingButton: Boolean = true,
    // KMK <--
    openMangaDexRandom: (() -> Unit)?,
    openMangaDexFollows: (() -> Unit)?,
    // SY <--
) {
    val updateFilters = { onUpdate(filters) }

    AdaptiveSheet(onDismissRequest = onDismissRequest) {
        LazyColumn {
            stickyHeader {
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .padding(8.dp),
                ) {
                    TextButton(onClick = onReset) {
                        Text(
                            text = stringResource(MR.strings.action_reset),
                            style = LocalTextStyle.current.copy(
                                color = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // KMK -->
                    if (shouldShowSavingButton) {
                        // KMK <--
                        // SY -->
                        IconButton(onClick = onSave) {
                            Icon(
                                Icons.Default.Save,
                                contentDescription = stringResource(MR.strings.action_save),
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                        // SY <--
                    }
                    Button(onClick = {
                        onFilter()
                        onDismissRequest()
                    }) {
                        Text(stringResource(MR.strings.action_filter))
                    }
                }
                HorizontalDivider()
            }

            if (openMangaDexRandom != null && openMangaDexFollows != null) {
                item {
                    MangaDexFilterHeader(
                        openMangaDexRandom = openMangaDexRandom,
                        openMangaDexFollows = openMangaDexFollows,
                    )
                }
            }

            item {
                SavedSearchItem(
                    savedSearches = savedSearches,
                    onSavedSearch = onSavedSearch,
                    onSavedSearchPress = onSavedSearchPress,
                    // KMK -->
                    onSavedSearchPressDesc = onSavedSearchPressDesc,
                    // KMK <--
                )
            }

            items(filters) {
                FilterItem(it, updateFilters /* SY --> */, startExpanded /* SY <-- */)
            }
        }
    }
}

@Composable
private fun FilterItem(filter: AnimeFilter<*>, onUpdate: () -> Unit/* SY --> */, startExpanded: Boolean /* SY <-- */) {
    when (filter) {
        // SY -->
        is AnimeFilter.AutoComplete -> {
            AutoCompleteItem(
                name = filter.name,
                state = filter.state.toImmutableList(),
                hint = filter.hint,
                values = filter.values.toImmutableList(),
                skipAutoFillTags = filter.skipAutoFillTags.toImmutableList(),
                validPrefixes = filter.validPrefixes.toImmutableList(),
            ) {
                filter.state = it
                onUpdate()
            }
        }
        // SY <--
        is AnimeFilter.Header -> {
            HeadingItem(filter.name)
        }
        is AnimeFilter.Separator -> {
            HorizontalDivider()
        }
        is AnimeFilter.CheckBox -> {
            CheckboxItem(
                label = filter.name,
                checked = filter.state,
            ) {
                filter.state = !filter.state
                onUpdate()
            }
        }
        is AnimeFilter.TriState -> {
            TriStateItem(
                label = filter.name,
                state = filter.state.toTriStateFilter(),
            ) {
                filter.state = filter.state.toTriStateFilter().next().toTriStateInt()
                onUpdate()
            }
        }
        is AnimeFilter.Text -> {
            TextItem(
                label = filter.name,
                value = filter.state,
            ) {
                filter.state = it
                onUpdate()
            }
        }
        is AnimeFilter.Select<*> -> {
            SelectItem(
                label = filter.name,
                options = filter.values,
                selectedIndex = filter.state,
            ) {
                filter.state = it
                onUpdate()
            }
        }
        is AnimeFilter.Sort -> {
            CollapsibleBox(
                heading = filter.name,
                // SY -->
                startExpanded = startExpanded,
                // SY <--
            ) {
                Column {
                    filter.values.mapIndexed { index, item ->
                        SortItem(
                            label = item,
                            sortDescending = filter.state?.ascending?.not()
                                ?.takeIf { index == filter.state?.index },
                        ) {
                            val ascending = if (index == filter.state?.index) {
                                !filter.state!!.ascending
                            } else {
                                filter.state!!.ascending
                            }
                            filter.state = AnimeFilter.Sort.Selection(
                                index = index,
                                ascending = ascending,
                            )
                            onUpdate()
                        }
                    }
                }
            }
        }
        is AnimeFilter.Group<*> -> {
            CollapsibleBox(
                heading = filter.name,
                // SY -->
                startExpanded = startExpanded,
                // SY <--
            ) {
                Column {
                    filter.state
                        .filterIsInstance<AnimeFilter<*>>()
                        .map { FilterItem(filter = it, onUpdate = onUpdate /* SY --> */, startExpanded /* SY <-- */) }
                }
            }
        }
    }
}

private fun Int.toTriStateFilter(): TriState {
    return when (this) {
        AnimeFilter.TriState.STATE_IGNORE -> TriState.DISABLED
        AnimeFilter.TriState.STATE_INCLUDE -> TriState.ENABLED_IS
        AnimeFilter.TriState.STATE_EXCLUDE -> TriState.ENABLED_NOT
        else -> throw IllegalStateException("Unknown TriState state: $this")
    }
}

private fun TriState.toTriStateInt(): Int {
    return when (this) {
        TriState.DISABLED -> AnimeFilter.TriState.STATE_IGNORE
        TriState.ENABLED_IS -> AnimeFilter.TriState.STATE_INCLUDE
        TriState.ENABLED_NOT -> AnimeFilter.TriState.STATE_EXCLUDE
    }
}
