package one.mixin.android.di

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import one.mixin.android.db.MixinDatabase
import one.mixin.android.di.type.DatabaseCategory
import one.mixin.android.di.type.DatabaseCategoryEnum
import javax.inject.Singleton

@InstallIn(ApplicationComponent::class)
@Module
internal object ReadDbModule {

    @Singleton
    @Provides
    @DatabaseCategory(DatabaseCategoryEnum.READ)
    fun provideReadDb(app: Application) = MixinDatabase.getReadDatabase(app)

    @Singleton
    @Provides
    @DatabaseCategory(DatabaseCategoryEnum.READ)
    fun provideConversationDao(@DatabaseCategory(DatabaseCategoryEnum.READ) db: MixinDatabase) = db.conversationDao()

    @Singleton
    @Provides
    @DatabaseCategory(DatabaseCategoryEnum.READ)
    fun provideMessageDao(@DatabaseCategory(DatabaseCategoryEnum.READ) db: MixinDatabase) = db.messageDao()
}
