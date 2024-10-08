/*
 * Copyright (C) 2015 Jorge Castillo Pérez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package one.mixin.android.widget.fabprogresscircle.progressarc.animations;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.view.animation.DecelerateInterpolator;

/**
 * @author Jorge Castillo Pérez
 */
public class GrowArcAnimation implements ArcAnimation {

  private ValueAnimator growAnim;

  GrowArcAnimation(ValueAnimator.AnimatorUpdateListener updateListener,
      Animator.AnimatorListener listener) {
    growAnim = ValueAnimator.ofFloat(ArcAnimationFactory.MINIMUM_SWEEP_ANGLE,
        ArcAnimationFactory.MAXIMUM_SWEEP_ANGLE);
    growAnim.setInterpolator(new DecelerateInterpolator());
    growAnim.setDuration(ArcAnimationFactory.SWEEP_ANIM_DURATION);
    growAnim.addUpdateListener(updateListener);
    growAnim.addListener(listener);
  }

  @Override public ValueAnimator getAnimator() {
    return growAnim;
  }
}