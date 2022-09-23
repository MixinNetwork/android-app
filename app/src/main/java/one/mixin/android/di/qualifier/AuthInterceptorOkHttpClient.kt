package one.mixin.android.di.qualifier

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LocalDatabaseQualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CacheDatabaseQualifier
