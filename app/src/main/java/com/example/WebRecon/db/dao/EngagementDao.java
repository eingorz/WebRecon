package com.example.WebRecon.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.WebRecon.db.entity.Engagement;

import java.util.List;

@Dao
public interface EngagementDao {

    @Insert
    long insert(Engagement engagement);

    @Update
    void update(Engagement engagement);

    @Query("SELECT * FROM engagements ORDER BY startedAt DESC")
    LiveData<List<Engagement>> getAll();

    @Query("SELECT * FROM engagements WHERE id = :id LIMIT 1")
    Engagement getByIdSync(long id);

    @Query("SELECT * FROM engagements ORDER BY startedAt DESC LIMIT 5")
    LiveData<List<Engagement>> getRecent();

    @Query("SELECT COUNT(*) FROM findings WHERE engagementId = :engagementId")
    int getFindingCount(long engagementId);
}
