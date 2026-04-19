package com.example.WebRecon.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.WebRecon.db.ToolType;

@Entity(tableName = "tool_operations")
public class ToolOperation {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public ToolType toolType;
    public String input;
    public String output;
    public long createdAt;

    public ToolOperation() {}

    public ToolOperation(ToolType toolType, String input, String output) {
        this.toolType = toolType;
        this.input = input;
        this.output = output;
        this.createdAt = System.currentTimeMillis();
    }
}
