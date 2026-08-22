package com.bharatconnect.app.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bharatconnect.app.data.local.room.dao.ConversationDao
import com.bharatconnect.app.data.local.room.dao.MessageDao
import com.bharatconnect.app.data.local.room.dao.PostDao
import com.bharatconnect.app.data.local.room.dao.UserDao
import com.bharatconnect.app.data.local.room.entity.ConversationEntity
import com.bharatconnect.app.data.local.room.entity.MessageEntity
import com.bharatconnect.app.data.local.room.entity.PostEntity
import com.bharatconnect.app.data.local.room.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        PostEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun postDao(): PostDao
}
