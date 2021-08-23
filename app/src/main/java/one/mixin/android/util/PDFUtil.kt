package one.mixin.android.util

import android.graphics.pdf.PdfDocument
import android.view.View
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

suspend fun generatePDF(views: Sequence<View>, filePath: String, listener: PDFGenerateListener) {
    withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        views.forEachIndexed{ index,view->
            val pageInfo =
                PdfDocument.PageInfo.Builder((8.3 * 72).toInt(), (11.7 * 72).toInt(), index+1).create()
            val page = pdfDocument.startPage(pageInfo)
            val pageCanvas = page.canvas
            pageCanvas.scale(1f, 1f)
            val pageWidth = pageCanvas.width
            val pageHeight = pageCanvas.height
            val measuredWidth =
                View.MeasureSpec.makeMeasureSpec(pageWidth, View.MeasureSpec.EXACTLY)
            val measuredHeight =
                View.MeasureSpec.makeMeasureSpec(pageHeight, View.MeasureSpec.EXACTLY)
            view.measure(measuredWidth, measuredHeight)
            view.layout(0, 0, pageWidth, pageHeight)
            view.draw(pageCanvas)
            pdfDocument.finishPage(page)
        }
        val file = File(filePath)
        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(file)
            pdfDocument.writeTo(fos)
            pdfDocument.close()
            fos = null
            listener.pdfGenerationSuccess()
        } catch (e: IOException) {
            fos?.close()
            fos = null
            listener.pdfGenerationFailure(e)
        } finally {
            fos?.close()
        }
    }
}

interface PDFGenerateListener {
    fun pdfGenerationSuccess()

    fun pdfGenerationFailure(exception: Exception)
}