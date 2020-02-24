package one.mixin.android.di;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import dagger.android.AndroidInjection;
import dagger.android.HasAndroidInjector;
import dagger.android.support.AndroidSupportInjection;
import one.mixin.android.MixinApplication;
import org.jetbrains.annotations.NotNull;

/**
 * Helper class to automatically inject fragments if they implement {@link Injectable}.
 */
public class AppInjector{
    public static AppComponent inject(MixinApplication mixinApp){
        AppComponent component = DaggerAppComponent.builder().application(mixinApp).build();
        component.inject(mixinApp);
        return component;
    }

    public static AppComponent init(MixinApplication mixinApp) {
        AppComponent component = inject(mixinApp);
        mixinApp.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                    @Override
                    public void onActivityCreated(@NotNull Activity activity, Bundle savedInstanceState) {
                        handleActivity(activity);
                    }

                    @Override
                    public void onActivityStarted(@NotNull Activity activity) {
                    }

                    @Override
                    public void onActivityResumed(@NotNull Activity activity) {
                    }

                    @Override
                    public void onActivityPaused(@NotNull Activity activity) {
                    }

                    @Override
                    public void onActivityStopped(@NotNull Activity activity) {
                    }

                    @Override
                    public void onActivitySaveInstanceState(@NotNull Activity activity, @NotNull Bundle outState) {
                    }

                    @Override
                    public void onActivityDestroyed(@NotNull Activity activity) {
                    }
                });
        return component;
    }

    private static void handleActivity(Activity activity) {
        if (activity instanceof Injectable) {
            AndroidInjection.inject(activity);
            return;
        }
        if (activity instanceof HasAndroidInjector) {
            AndroidInjection.inject(activity);
        }
        if (activity instanceof FragmentActivity) {
            ((FragmentActivity) activity).getSupportFragmentManager()
                    .registerFragmentLifecycleCallbacks(
                            new FragmentManager.FragmentLifecycleCallbacks() {
                                @Override
                                public void onFragmentCreated(
                                    @NotNull FragmentManager fm,
                                    @NotNull Fragment f,
                                    Bundle savedInstanceState
                                ) {
                                    if (f instanceof Injectable) {
                                        AndroidSupportInjection.inject(f);
                                    }
                                }
                            }, true);
        }
    }
}
