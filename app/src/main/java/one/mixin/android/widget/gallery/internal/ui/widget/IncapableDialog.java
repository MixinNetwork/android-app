package one.mixin.android.widget.gallery.internal.ui.widget;

import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import android.text.TextUtils;

public class IncapableDialog extends DialogFragment {

    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_MESSAGE = "extra_message";

    public static IncapableDialog newInstance(String title, String message) {
        IncapableDialog dialog = new IncapableDialog();
        Bundle args = new Bundle();
        args.putString(EXTRA_TITLE, title);
        args.putString(EXTRA_MESSAGE, message);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String title = getArguments().getString(EXTRA_TITLE);
        String message = getArguments().getString(EXTRA_MESSAGE);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        if (!TextUtils.isEmpty(title)) {
            builder.setTitle(title);
        }
        if (!TextUtils.isEmpty(message)) {
            builder.setMessage(message);
        }
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss());

        return builder.create();
    }
}
