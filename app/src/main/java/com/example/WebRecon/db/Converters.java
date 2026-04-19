package com.example.WebRecon.db;

import androidx.room.TypeConverter;

public class Converters {

    @TypeConverter
    public static String fromFindingType(FindingType value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public static FindingType toFindingType(String value) {
        return value == null ? null : FindingType.valueOf(value);
    }

    @TypeConverter
    public static String fromSeverity(Severity value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public static Severity toSeverity(String value) {
        return value == null ? null : Severity.valueOf(value);
    }

    @TypeConverter
    public static String fromToolType(ToolType value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public static ToolType toToolType(String value) {
        return value == null ? null : ToolType.valueOf(value);
    }

    @TypeConverter
    public static String fromEngagementStatus(EngagementStatus value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public static EngagementStatus toEngagementStatus(String value) {
        return value == null ? null : EngagementStatus.valueOf(value);
    }
}
