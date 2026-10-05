package com.appinspector.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppErrorDao {

    @Query("SELECT * FROM app_errors ORDER BY timestamp DESC")
    fun getAllErrors(): Flow<List<AppErrorEntity>>

    @Query("SELECT * FROM app_errors WHERE packageName = :packageName ORDER BY timestamp DESC")
    fun getErrorsByPackage(packageName: String): Flow<List<AppErrorEntity>>

    @Query("SELECT * FROM app_errors WHERE id = :id")
    fun getErrorById(id: Long): Flow<AppErrorEntity?>

    @Query("SELECT COUNT(*) FROM app_errors")
    fun getErrorCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertError(error: AppErrorEntity): Long

    @Delete
    suspend fun deleteError(error: AppErrorEntity)

    @Query("DELETE FROM app_errors")
    suspend fun deleteAllErrors()
}
