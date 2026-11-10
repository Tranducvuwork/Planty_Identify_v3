package com.expeditee.plant.identifier.data.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.expeditee.plant.identifier.data.entity.PopularPlantEntity
import com.expeditee.plant.identifier.data.entity.PopularPlantFtsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PopularPlantDao {

    @Query("SELECT * FROM popular_plants ORDER BY id LIMIT :limit OFFSET :offset")
    fun getAllPlants(limit: Int = 100, offset: Int = 0): Flow<List<PopularPlantEntity>>

    @Query("SELECT * FROM popular_plants ORDER BY id LIMIT :limit")
    fun getPopularPlantsLimited(limit: Int): Flow<List<PopularPlantEntity>>

    @Query("SELECT * FROM popular_plants ORDER BY id")
    fun getAllPlantsPaged(): PagingSource<Int, PopularPlantEntity>

    @Query("SELECT * FROM popular_plants ORDER BY id")
    suspend fun getAllPlantsNoLimitOnce(): List<PopularPlantEntity>

    @Query("""
        SELECT p.* FROM popular_plants p
        JOIN popular_plants_fts fts ON p.rowid = fts.rowid
        WHERE popular_plants_fts MATCH :query || '*'
        ORDER BY p.id
    """)
    fun getPlantsPagedByQuery(query: String): PagingSource<Int, PopularPlantEntity>

    @Query("""
        SELECT p.* FROM popular_plants p
        JOIN popular_plants_fts fts ON p.rowid = fts.rowid
        WHERE popular_plants_fts MATCH :query || '*'
        ORDER BY p.id
        LIMIT 30
    """)
    fun searchPlants(query: String): Flow<List<PopularPlantEntity>>

    @Query(
        """
        SELECT * FROM popular_plants
        WHERE LOWER(commonNames) LIKE '%' || LOWER(:query) || '%'
           OR LOWER(IFNULL(scientificName, '')) LIKE '%' || LOWER(:query) || '%'
        ORDER BY id
        LIMIT 30
        """,
    )
    suspend fun searchPlantsLikeOnce(query: String): List<PopularPlantEntity>

    @Query(
        "SELECT * FROM popular_plants WHERE LOWER(TRIM(scientificName)) = LOWER(TRIM(:scientificName)) LIMIT 1"
    )
    suspend fun findByScientificNameExact(scientificName: String): PopularPlantEntity?

    @Query(
        "SELECT * FROM popular_plants WHERE scientificName IS NOT NULL AND LOWER(scientificName) LIKE LOWER(:genus) || ' %' ORDER BY id LIMIT 1"
    )
    suspend fun findByGenusPrefix(genus: String): PopularPlantEntity?

    @Query("SELECT * FROM popular_plants WHERE commonNames LIKE '%' || :nameKeyword || '%' ORDER BY id LIMIT 1")
    suspend fun findByCommonNameKeyword(nameKeyword: String): PopularPlantEntity?

    @Query("SELECT * FROM popular_plants WHERE id = :id LIMIT 1")
    suspend fun findById(id: Int): PopularPlantEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(plants: List<PopularPlantEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFts(plants: List<PopularPlantFtsEntity>)

    @Query("DELETE FROM popular_plants")
    suspend fun clearAll()

    @Query("DELETE FROM popular_plants_fts")
    suspend fun clearAllFts()

    @Query("SELECT COUNT(*) FROM popular_plants")
    suspend fun getCount(): Int
}