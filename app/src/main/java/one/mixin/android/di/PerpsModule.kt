package one.mixin.android.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import one.mixin.android.db.PerpsDatabase
import one.mixin.android.db.perps.PerpsMarketDao
import one.mixin.android.db.perps.PerpsOrderDao
import one.mixin.android.db.perps.PerpsPositionDao
import one.mixin.android.session.CurrentUserScopeManager
import javax.inject.Provider

@Module
@InstallIn(SingletonComponent::class)
object PerpsModule {

    @Provides
    fun providePerpsDatabase(scopeManagerProvider: Provider<CurrentUserScopeManager>): PerpsDatabase =
        scopeManagerProvider.get().getPerpsDatabase()

    @Provides
    fun providePerpsPositionDao(database: PerpsDatabase): PerpsPositionDao {
        return database.perpsPositionDao()
    }

    @Provides
    fun providePerpsOrderDao(database: PerpsDatabase): PerpsOrderDao {
        return database.perpsOrderDao()
    }

    @Provides
    fun providePerpsMarketDao(database: PerpsDatabase): PerpsMarketDao {
        return database.perpsMarketDao()
    }
}
