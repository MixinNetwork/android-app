package one.mixin.android.ui.conversation.markdown.pdf

import android.graphics.Paint
import android.graphics.Point
import android.graphics.pdf.PdfDocument
import android.view.View
import androidx.core.view.drawToBitmap
import androidx.recyclerview.widget.RecyclerView
import io.noties.markwon.recycler.MarkwonAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.extension.colorFromAttribute
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

private fun newPage(
    pdfDocument: PdfDocument,
    pageIndex: Int,
    width: Int,
    height: Int
): PdfDocument.Page {
    return pdfDocument.startPage(
        PdfDocument.PageInfo.Builder(
            width,
            height,
            pageIndex
        ).create()
    )
}

suspend fun generatePDF(
    recyclerView: RecyclerView,
    filePath: String,
    listener: PDFGenerateListener
) {
    val adapter = (recyclerView.adapter as MarkwonAdapter?) ?: return
    val background = recyclerView.context.colorFromAttribute(R.attr.bg_post)
    val size = adapter.itemCount
    val paint = Paint()
    val pageSize = Point(
        recyclerView.measuredWidth,
        recyclerView.measuredWidth * 16 / 9
    )
    withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        val file = File(filePath)
        var pageIndex = 0
        var page = newPage(pdfDocument, pageIndex, pageSize.x, pageSize.y)
        var pageCanvas = page.canvas
        pageCanvas.drawColor(background)
        pageCanvas.scale(1f, 1f)
        val pageWidth = pageCanvas.width
        val pageHeight = pageCanvas.height
        var currentHeight = 0f
        for (i in 0 until size) {
            val itemViewType = adapter.getItemViewType(i)
            val holder = adapter.createViewHolder(recyclerView, itemViewType)
            adapter.onBindViewHolder(holder, i)
            holder.itemView.measure(
                View.MeasureSpec.makeMeasureSpec(pageWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            holder.itemView.layout(
                0,
                0,
                holder.itemView.measuredWidth,
                holder.itemView.measuredHeight
            )
            if (currentHeight + holder.itemView.measuredHeight > pageHeight) {
                pdfDocument.finishPage(page)
                pageIndex++
                page = newPage(pdfDocument, pageIndex, pageSize.x, pageSize.y)
                pageCanvas = page.canvas
                pageCanvas.drawColor(background)
                currentHeight = 0f
            }

            pageCanvas.drawBitmap(
                holder.itemView.drawToBitmap(),
                0f,
                currentHeight,
                paint
            )
            currentHeight += holder.itemView.measuredHeight
        }
        pdfDocument.finishPage(page)
        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(file)
            pdfDocument.writeTo(fos)
            pdfDocument.close()
            fos = null
            withContext(Dispatchers.Main) {
                listener.pdfGenerationSuccess()
            }
        } catch (e: IOException) {
            fos?.close()
            fos = null
            withContext(Dispatchers.Main) {
                listener.pdfGenerationFailure(e)
            }
        } finally {
            fos?.close()
        }
    }
}

interface PDFGenerateListener {
    fun pdfGenerationSuccess()

    fun pdfGenerationFailure(exception: Exception)
}
