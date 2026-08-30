package com.google.refereeschedule.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.google.refereeschedule.data.local.dao.AssignmentDao
import com.google.refereeschedule.data.local.dao.GameDao
import com.google.refereeschedule.data.local.entity.AssignmentEntity
import com.google.refereeschedule.data.local.entity.GameEntity

@Database(entities = [GameEntity::class, AssignmentEntity::class], version = 6, exportSchema = false)
abstract class RefereeDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun assignmentDao(): AssignmentDao
}
