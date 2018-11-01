/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.support.design;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v7.app.MixinAppCompatDialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.util.List;
import javax.annotation.Nullable;
import one.mixin.android.ui.url.UrlInterpreterActivity;

public class MixinBottomSheetDialogFragment extends MixinAppCompatDialogFragment {

    @Override
    @Nullable
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new BottomSheetDialog(requireContext(), getTheme());
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (getActivity() instanceof UrlInterpreterActivity) {
            FragmentManager fragmentManager = getFragmentManager();
            if (fragmentManager != null) {
                List<Fragment> fragments = fragmentManager.getFragments();
                if (fragments.size() <= 0) {
                    Activity activity = getActivity();
                    if (activity != null) {
                        activity.finish();
                    }
                }
            }
        }
    }

}
