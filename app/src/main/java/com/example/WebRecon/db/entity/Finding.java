package com.example.WebRecon.db.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.example.WebRecon.db.FindingType;
import com.example.WebRecon.db.Severity;

@Entity(
    tableName = "findings",
    foreignKeys = @ForeignKey(
        entity = Engagement.class,
        parentColumns = "id",
        childColumns = "engagementId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = @Index("engagementId")
)
public class Finding {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long engagementId;
    public FindingType type;
    public Severity severity;
    public String title;
    public String detail;
    public long discoveredAt;

    public Finding() {}

    public Finding(long engagementId, FindingType type, Severity severity, String title, String detail) {
        this.engagementId = engagementId;
        this.type = type;
        this.severity = severity;
        this.title = title;
        this.detail = detail;
        this.discoveredAt = System.currentTimeMillis();
    }
}
