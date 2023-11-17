package one.mixin.android.widget.gallery;

import android.app.Activity;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.lang.ref.WeakReference;
import java.util.Set;

public final class Gallery {

    private final WeakReference<Activity> mContext;
    private final WeakReference<Fragment> mFragment;

    private Gallery(Activity activity) {
        this(activity, null);
    }

    private Gallery(Fragment fragment) {
        this(fragment.getActivity(), fragment);
    }

    private Gallery(Activity activity, Fragment fragment) {
        mContext = new WeakReference<>(activity);
        mFragment = new WeakReference<>(fragment);
    }

    public static Gallery from(Activity activity) {
        return new Gallery(activity);
    }


    public static Gallery from(Fragment fragment) {
        return new Gallery(fragment);
    }


    public SelectionCreator choose(Set<MimeType> mimeTypes) {
        return this.choose(mimeTypes, true);
    }


    public SelectionCreator choose(Set<MimeType> mimeTypes, boolean mediaTypeExclusive) {
        return new SelectionCreator(this, mimeTypes, mediaTypeExclusive);
    }

    @Nullable
    Activity getActivity() {
        return mContext.get();
    }

    @Nullable
    Fragment getFragment() {
        return mFragment != null ? mFragment.get() : null;
    }

}
