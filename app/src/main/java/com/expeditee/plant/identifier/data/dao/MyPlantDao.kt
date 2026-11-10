package com.expeditee.plant.identifier.data.dao


import androidx.paging.PagingSource
import androidx.room.*
import com.expeditee.plant.identifier.data.entity.MyPlantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MyPlantDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(coin: MyPlantEntity): Long

    @Update
    suspend fun update(coin: MyPlantEntity): Int

    @Transaction
    suspend fun save(task: MyPlantEntity): Long {
        val id = insert(task)
        return if (id == -1L) {
            update(task).toLong()
        } else {
            id
        }
    }

    @Delete
    suspend fun deletePlant(plant: MyPlantEntity)

    @Query("SELECT * FROM my_plants WHERE id = :id")
    suspend fun getPlantById(id: Int): MyPlantEntity?

    @Query("SELECT * FROM my_plants WHERE (:gardenId IS NULL OR gardenId = :gardenId) ORDER BY id DESC")
    fun getAllMyPlantsPaged(gardenId: String?): PagingSource<Int, MyPlantEntity>

    @Query("SELECT * FROM my_plants WHERE LOWER(type) = LOWER(:type) AND (:gardenId IS NULL OR gardenId = :gardenId) ORDER BY id DESC")
    fun getMyPlantsByTypePaged(type: String, gardenId: String?): PagingSource<Int, MyPlantEntity>

    @Query("SELECT * FROM my_plants ORDER BY id DESC")
    fun getAllMyPlantsPaged(): PagingSource<Int, MyPlantEntity>

    @Query("SELECT * FROM my_plants WHERE LOWER(type) = LOWER(:type) ORDER BY id DESC")
    fun getMyPlantsByTypePaged(type: String): PagingSource<Int, MyPlantEntity>

    @Query("SELECT COUNT(*) FROM my_plants")
    suspend fun getCount(): Int

    @Query("DELETE FROM my_plants")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM my_plants")
    fun getTotalCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM my_plants WHERE LOWER(type) = 'plant'")
    fun getPlantCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM my_plants WHERE LOWER(type) = 'flower'")
    fun getFlowerCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM my_plants WHERE LOWER(status) = 'healthy'")
    fun getHealthyPlantCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM my_plants WHERE LOWER(status) = 'unhealthy'")
    fun getUnhealthyPlantCountFlow(): Flow<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM my_plants WHERE name = :name AND scientificName = :scientificName)")
    suspend fun exists(name: String, scientificName: String): Boolean
}