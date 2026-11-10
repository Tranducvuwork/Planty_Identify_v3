package com.expeditee.plant.identifier.presenter.careguide

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.expeditee.plant.identifier.domain.repository.GithubRepository
import com.expeditee.plant.identifier.model.reponse.SuggestedPlant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CareGuidesPagingSource(
    private val repository: GithubRepository,
    private val query: String,
    private val language: String? = null,
) : PagingSource<Int, SuggestedPlant>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SuggestedPlant> {
        return try {
            val allData = withContext(Dispatchers.IO) {
                repository.getCareGuide(language)?.careGuides ?: emptyList()
            }

            val filteredData = if (query.isEmpty()) {
                allData
            } else {
                allData.filter {
                    it.scientificName?.contains(query, ignoreCase = true) == true ||
                            it.commonNames?.any { name -> name.contains(query, ignoreCase = true) } == true
                }
            }

            val start = params.key ?: 0
            if (start >= filteredData.size) {
                return LoadResult.Page(data = emptyList(), prevKey = null, nextKey = null)
            }
            val end = (start + params.loadSize).coerceAtMost(filteredData.size)

            LoadResult.Page(
                data = filteredData.subList(start, end),
                prevKey = if (start == 0) null else start - params.loadSize,
                nextKey = if (end < filteredData.size) end else null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, SuggestedPlant>): Int? {
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(state.config.pageSize)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(state.config.pageSize)
        }
    }
}
