package one.mixin.android.di.worker;

import androidx.work.Worker
import dagger.MapKey
import dagger.Module
import dagger.android.AndroidInjector
import dagger.multibindings.Multibinds
import kotlin.reflect.KClass

/**
 * Adapted from https://gist.github.com/ferrerojosh/82bd92748f315155fa6a842f4ed64c82
 */
internal object AndroidWorkerInjector {
    fun inject(worker: Worker) {
        val application = worker.applicationContext
        if (application !is HasWorkerInjector) {
            throw RuntimeException("${application.javaClass.canonicalName} does not implement ${HasWorkerInjector::class.java.canonicalName}")
        }

        val workerInjector = (application as HasWorkerInjector).workerInjector()
        workerInjector?.inject(worker)
    }
}

interface HasWorkerInjector {
    fun workerInjector(): AndroidInjector<Worker>?
}

@Module
abstract class AndroidWorkerInjectionModule {
    @Multibinds
    abstract fun workerInjectorFactories(): Map<String, AndroidInjector.Factory<out Worker>>
}

@MapKey
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class WorkerKey(val value: KClass<out Worker>)