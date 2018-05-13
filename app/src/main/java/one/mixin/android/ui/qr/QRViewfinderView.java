package one.mixin.android.ui.qr;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

import com.journeyapps.barcodescanner.ViewfinderView;

public class QRViewfinderView extends ViewfinderView {

    public QRViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onDraw(Canvas canvas) {
    }
}
