package com.example.WebRecon.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.WebRecon.db.entity.Finding;

import java.util.List;

@Dao
public interface FindingDao {

    @Insert
    long insert(Finding finding);

    @Insert
    void insertAll(List<Finding> findings);

    @Query("SELECT * FROM findings WHERE engagementId = :engagementId ORDER BY discoveredAt ASC")
    LiveData<List<Finding>> getForEngagement(long engagementId);

    @Query("SELECT * FROM findings WHERE engagementId = :engagementId ORDER BY discoveredAt ASC")
    List<Finding> getForEngagementSync(long engagementId);

    @Query("DELETE FROM findings WHERE engagementId = :engagementId")
    void deleteForEngagement(long engagementId);
}
