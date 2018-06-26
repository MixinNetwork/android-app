package one.mixin.android.widget.gallery.listener;

import android.net.Uri;
import android.support.annotation.NonNull;
import java.util.List;

public interface OnSelectedListener {
    void onSelected(@NonNull List<Uri> uriList, @NonNull List<String> pathList);
}
