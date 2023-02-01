package one.mixin.android.util.mlkit.scan.analyze

import android.graphics.Bitmap

class AnalyzeResult<T> {
    var bitmap: Bitmap? = null
    var result: T? = null
        private set

    constructor() {}
    constructor(bitmap: Bitmap?, result: T) {
        this.bitmap = bitmap
        this.result = result
    }

    fun setResult(result: T) {
        this.result = result
    }
}
