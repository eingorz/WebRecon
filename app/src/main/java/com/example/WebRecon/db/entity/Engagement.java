package com.example.WebRecon.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.WebRecon.db.EngagementStatus;

@Entity(tableName = "engagements")
public class Engagement {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String domain;
    public long startedAt;
    public Long completedAt;
    public EngagementStatus status;

    public Engagement() {}

    public Engagement(String domain) {
        this.domain = domain;
        this.startedAt = System.currentTimeMillis();
        this.status = EngagementStatus.RUNNING;
    }
}
