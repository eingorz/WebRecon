package com.example.WebRecon.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.WebRecon.db.ToolType;
import com.example.WebRecon.db.entity.ToolOperation;

import java.util.List;

@Dao
public interface ToolOperationDao {

    @Insert
    long insert(ToolOperation operation);

    @Query("SELECT * FROM tool_operations ORDER BY createdAt DESC")
    LiveData<List<ToolOperation>> getAll();

    @Query("SELECT * FROM tool_operations WHERE toolType = :toolType ORDER BY createdAt DESC")
    LiveData<List<ToolOperation>> getByType(ToolType toolType);

    @Query("SELECT * FROM tool_operations WHERE id = :id LIMIT 1")
    ToolOperation getByIdSync(long id);
}
