package com.example.WebRecon.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.WebRecon.db.dao.EngagementDao;
import com.example.WebRecon.db.dao.FindingDao;
import com.example.WebRecon.db.dao.ToolOperationDao;
import com.example.WebRecon.db.entity.Engagement;
import com.example.WebRecon.db.entity.Finding;
import com.example.WebRecon.db.entity.ToolOperation;

@Database(
    entities = {Engagement.class, Finding.class, ToolOperation.class},
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters.class)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    public abstract EngagementDao engagementDao();
    public abstract FindingDao findingDao();
    public abstract ToolOperationDao toolOperationDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        "webrecon.db"
                    ).build();
                }
            }
        }
        return instance;
    }
}
