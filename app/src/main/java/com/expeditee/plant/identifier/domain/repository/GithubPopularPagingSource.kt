package com.expeditee.plant.identifier.domain.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.expeditee.plant.identifier.domain.github.GithubApi
import com.expeditee.plant.identifier.domain.mapper.toPopularPlantModel
import com.expeditee.plant.identifier.helper.firebase.RemoteConfigManagement
import com.expeditee.plant.identifier.model.PopularPlantModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GithubPopularPagingSource(
    private val remoteConfig: RemoteConfigManagement,
    private val githubApi: GithubApi,
    private val getCache: () -> List<PopularPlantModel>?,
    private val setCache: (List<PopularPlantModel>) -> Unit
) : PagingSource<Int, PopularPlantModel>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PopularPlantModel> {
        return try {
            val data = withContext(Dispatchers.IO) {
                getCache() ?: fetchFromGithub().also { setCache(it) }
            }

            val start = params.key ?: 0
            if (start >= data.size) {
                return LoadResult.Page(data = emptyList(), prevKey = null, nextKey = null)
            }
            val end = minOf(start + params.loadSize, data.size)

            LoadResult.Page(
                data = data.subList(start, end),
                prevKey = if (start == 0) null else start - params.loadSize,
                nextKey = if (end < data.size) end else null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, PopularPlantModel>): Int? {
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(state.config.pageSize)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(state.config.pageSize)
        }
    }

    private suspend fun fetchFromGithub(): List<PopularPlantModel> {
        if (remoteConfig.baseURLInvalid){
            return emptyList()
        }
        val response = githubApi.getDetailedPlants()
        if (!response.isSuccessful) return emptyList()
        return response.body()?.map { it.toPopularPlantModel() } ?: emptyList()
    }
}