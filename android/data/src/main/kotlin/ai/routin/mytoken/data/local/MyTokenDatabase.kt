package ai.routin.mytoken.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CredentialEntity::class, UsageSnapshotEntity::class],
    version = 2,
    exportSchema = true
)
abstract class MyTokenDatabase : RoomDatabase() {
    abstract fun credentialDao(): CredentialDao
    abstract fun usageSnapshotDao(): UsageSnapshotDao

    companion object {
        const val NAME = "mytoken.db"

        /** v2 新增每凭证低余额预警值；旧数据按未设置（null）处理。 */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE credentials ADD COLUMN balanceWarningThreshold TEXT")
            }
        }
    }
}
