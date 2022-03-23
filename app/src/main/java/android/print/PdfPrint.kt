package android.print

import android.content.Context
import android.os.ParcelFileDescriptor
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.File

fun printPdf(context: Context, html: String, outputFile: File, callback: PrintPdfCallback) {
    val webView = WebView(context)
    val attr = PrintAttributes.Builder()
        .setResolution(PrintAttributes.Resolution(outputFile.name, outputFile.name, 1080, 1080))
        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
        .build()
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            val documentAdapter = view.createPrintDocumentAdapter(outputFile.name)
            documentAdapter.onLayout(
                null, attr, null,
                object : PrintDocumentAdapter.LayoutResultCallback() {
                    override fun onLayoutFinished(info: PrintDocumentInfo?, changed: Boolean) {
                        try {
                            outputFile.createNewFile()
                        } catch (e: Exception) {
                            callback.onFailure(e.message)
                        }
                        val fileDescriptor = ParcelFileDescriptor.open(outputFile, ParcelFileDescriptor.MODE_TRUNCATE or ParcelFileDescriptor.MODE_READ_WRITE)
                        documentAdapter.onWrite(
                            arrayOf(PageRange.ALL_PAGES), fileDescriptor, null,
                            object : PrintDocumentAdapter.WriteResultCallback() {
                                override fun onWriteFinished(pages: Array<out PageRange>?) {
                                    callback.onSuccess()
                                }

                                override fun onWriteFailed(error: CharSequence?) {
                                    callback.onFailure(error)
                                }
                            }
                        )
                    }

                    override fun onLayoutFailed(error: CharSequence?) {
                        callback.onFailure(error)
                    }
                },
                null
            )
        }
    }
    webView.loadDataWithBaseURL("", html, "text/html", "utf-8", null)
}

interface PrintPdfCallback {
    fun onSuccess()
    fun onFailure(error: CharSequence?)
}
