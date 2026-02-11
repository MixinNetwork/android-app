package one.mixin.android.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import one.mixin.android.db.PerpsDatabase
import one.mixin.android.db.perps.PerpsMarketDao
import one.mixin.android.db.perps.PerpsPositionDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PerpsModule {

    @Provides
    @Singleton
    fun providePerpsDatabase(@ApplicationContext context: Context): PerpsDatabase {
        return PerpsDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun providePerpsPositionDao(database: PerpsDatabase): PerpsPositionDao {
        return database.perpsPositionDao()
    }

    @Provides
    @Singleton
    fun providePerpsMarketDao(database: PerpsDatabase): PerpsMarketDao {
        return database.perpsMarketDao()
    }
}
