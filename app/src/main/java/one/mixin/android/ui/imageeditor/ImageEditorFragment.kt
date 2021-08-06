package one.mixin.android.ui.imageeditor

import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.FragmentImageEditorBinding
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.widget.PrevNextView
import one.mixin.android.widget.imageeditor.ColorableRenderer
import one.mixin.android.widget.imageeditor.ImageEditorView
import one.mixin.android.widget.imageeditor.model.EditorElement
import one.mixin.android.widget.imageeditor.model.EditorModel
import one.mixin.android.widget.imageeditor.renderers.MultiLineTextRenderer

class ImageEditorFragment : BaseFragment() {
    companion object {
        const val TAG = "ImageEditorFragment"
        const val ARGS_IMAGE_URI = "args_image_uri"

        private const val MAX_IMAGE_SIZE = 4096

        fun newInstance(imageUri: Uri) = ImageEditorFragment().withArgs {
            putParcelable(ARGS_IMAGE_URI, imageUri)
        }
    }

    private val paletteColors = listOf(
        Color.parseColor("#DBDCE0"),
        Color.parseColor("#81868C"),
        Color.parseColor("#202125"),
        Color.parseColor("#F5AEA8"),
        Color.parseColor("#F28B82"),
        Color.parseColor("#D83025"),
        Color.parseColor("#D2E4FC"),
        Color.parseColor("#669DF7"),
        Color.parseColor("#1A73E9"),
        Color.parseColor("#FDE293"),
        Color.parseColor("#FCC834"),
        Color.parseColor("#EB8600"),
        Color.parseColor("#A8DAB5"),
        Color.parseColor("#34A853"),
        Color.parseColor("#198039"),
        Color.parseColor("#D7AEFC"),
        Color.parseColor("#A142F4"),
        Color.parseColor("#B430CE"),
    )

    private var currentSelection: EditorElement? = null
    private var currentMode = Mode.None
    private var activeColor = paletteColors[5]

    private var undoAvailable: Boolean = false
    private var redoAvailable: Boolean = false

    private val imageUri: Uri by lazy { requireArguments().getParcelable(ARGS_IMAGE_URI)!! }

    private val colorPaletteAdapter = ColorPaletteAdapter(
        paletteColors[5],
        onColorChanged = { c ->
            activeColor = c
            binding.imageEditorView.setDrawingBrushColor(c)
            if (currentSelection != null) {
                val render = currentSelection?.renderer
                if (render is ColorableRenderer) {
                    render.color = c
                }
            }
        }
    ).apply {
        submitList(paletteColors)
    }

    private var _binding: FragmentImageEditorBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentImageEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            closeIv.setOnClickListener { activity?.onBackPressed() }
            cropLl.setOnClickListener { setMode(Mode.Crop) }
            textLl.setOnClickListener { setMode(Mode.Text) }
            drawLl.setOnClickListener { setMode(Mode.Draw) }
            cancelIv.setOnClickListener { cancel() }
            checkIv.setOnClickListener { setMode(Mode.None) }
            titlePrevNextView.prev.setOnClickListener { undo() }
            titlePrevNextView.next.setOnClickListener { redo() }
            undoRedoView.prev.setOnClickListener { undo() }
            undoRedoView.next.setOnClickListener { redo() }
            sizeSeekbar.onSeekChangeListener = onSeekChangeListener

            colorRv.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
            colorRv.adapter = colorPaletteAdapter
        }
        binding.imageEditorView.apply {
            setDrawingBrushColor(paletteColors[5])
            setTapListener(tapListener)
            setDrawingChangedListener { }
            setUndoRedoStackListener(this@ImageEditorFragment::onUndoRedoAvailabilityChanged)
        }

        val editorModel = EditorModel.create()
        val image = EditorElement(UriGlideRenderer(imageUri, true, MAX_IMAGE_SIZE, MAX_IMAGE_SIZE))
        image.flags.setSelectable(false).persist()
        editorModel.addElement(image)
        binding.imageEditorView.model = editorModel
    }

    override fun onBackPressed(): Boolean {
        if (currentMode != Mode.None) {
            setMode(Mode.None)
            return true
        }
        if (undoAvailable) {
            alertDialogBuilder()
                .setMessage(R.string.exit_without_save)
                .setNegativeButton(R.string.cancel) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(R.string.exit) { dialog, _ ->
                    dialog.dismiss()
                    activity?.finish()
                }.show()
            return true
        }
        return super.onBackPressed()
    }

    private fun onModeSet(mode: Mode) {
        binding.apply {
            imageEditorView.setMode(ImageEditorView.Mode.MoveAndResize)
            imageEditorView.doneTextEditing()

            when (mode) {
                Mode.None -> {
                    imageEditorView.model.doneCrop()
                    currentSelection = null
                }
                Mode.Crop -> imageEditorView.model.startCrop()
                Mode.Text -> addText()
                Mode.Draw -> imageEditorView.startDrawing(binding.sizeSeekbar.progressFloat / 100, Paint.Cap.ROUND, false)
            }
        }
    }

    private fun setMode(mode: Mode, notify: Boolean = true) {
        currentMode = mode

        when (mode) {
            Mode.None -> presentModeNone()
            Mode.Crop -> presentModeCrop()
            Mode.Text -> presentModeText()
            Mode.Draw -> presentModeDraw()
        }

        if (notify) {
            onModeSet(mode)
        }
    }

    private fun cancel() {
        when (currentMode) {
            Mode.Text -> binding.imageEditorView.model.clearMultiLineTextRenderers()
            Mode.Draw -> binding.imageEditorView.model.clearBezierDrawingRenderers()
            Mode.Crop -> undo()
            else -> {}
        }
        setMode(Mode.None)
    }

    private fun undo() {
        binding.imageEditorView.model.undo()
    }

    private fun redo() {
        binding.imageEditorView.model.redo()
    }

    private fun presentModeDraw() {
        binding.apply {
            editorLl.isVisible = true
            tabLl.isVisible = false
            title.isVisible = false
            typeTv.isVisible = false
            undoRedoView.isVisible = true
            colorRv.isVisible = true
            seekbarLl.isVisible = true
        }
    }

    private fun presentModeText() {
        binding.apply {
            editorLl.isVisible = true
            tabLl.isVisible = false
            title.isVisible = false
            typeTv.isVisible = true
            undoRedoView.isVisible = false
            typeTv.text = getString(R.string.text)
            colorRv.isVisible = true
            seekbarLl.isVisible = false
        }
    }

    private fun presentModeCrop() {
        binding.apply {
            editorLl.isVisible = true
            tabLl.isVisible = false
            title.isVisible = false
            typeTv.isVisible = true
            undoRedoView.isVisible = false
            typeTv.text = getString(R.string.crop)
            colorRv.isVisible = false
            seekbarLl.isVisible = false
        }
    }

    private fun presentModeNone() {
        binding.apply {
            editorLl.isVisible = false
            tabLl.isVisible = true
            title.isVisible = true
        }
    }

    private fun startTextEntityEditing(textElement: EditorElement, selectAll: Boolean) {
        binding.imageEditorView.startTextEditing(
            textElement,
            defaultSharedPreferences.getBoolean(
                Constants.Account.PREF_INCOGNITO_KEYBOARD, false
            ),
            selectAll
        )
    }

    private fun addText() {
        val initialText = ""
        val color = activeColor
        val render = MultiLineTextRenderer(initialText, color)
        val element = EditorElement(render, EditorModel.Z_TEXT)
        binding.imageEditorView.apply {
            model.addElementCentered(element, 1f)
            invalidate()
        }
        currentSelection = element
        startTextEntityEditing(element, true)
    }

    private fun onUndoRedoAvailabilityChanged(undoAvailable: Boolean, redoAvailable: Boolean) {
        this.undoAvailable = undoAvailable
        this.redoAvailable = redoAvailable
        binding.undoRedoView.updatePrevNextView()
        binding.titlePrevNextView.updatePrevNextView()
    }

    private fun PrevNextView.updatePrevNextView() {
        prev.isEnabled = undoAvailable
        next.isEnabled = redoAvailable
    }

    private val onSeekChangeListener = object : OnSeekChangeListener {
        override fun onSeeking(seekParams: SeekParams) {
        }

        override fun onStartTrackingTouch(seekBar: IndicatorSeekBar) {
        }

        override fun onStopTrackingTouch(seekBar: IndicatorSeekBar) {
            val value = seekBar.progressFloat
            binding.imageEditorView.startDrawing(value / 100, Paint.Cap.ROUND, false)
        }
    }

    private val tapListener = object : ImageEditorView.TapListener {
        override fun onEntityDown(editorElement: EditorElement?) {
            currentSelection = null
            setMode(Mode.None)
        }

        override fun onEntitySingleTap(editorElement: EditorElement?) {
            currentSelection = editorElement
            if (currentSelection != null) {
                if (editorElement?.renderer is MultiLineTextRenderer) {
                    setTextElement(editorElement, editorElement.renderer as ColorableRenderer, binding.imageEditorView.isTextEditing)
                }
            }
        }

        override fun onEntityDoubleTap(editorElement: EditorElement) {
            currentSelection = editorElement
            if (currentSelection != null) {
                if (editorElement.renderer is MultiLineTextRenderer) {
                    setTextElement(editorElement, editorElement.renderer as ColorableRenderer, true)
                }
            }
        }

        private fun setTextElement(editorElement: EditorElement, colorableRenderer: ColorableRenderer, startEditing: Boolean) {
            val color = colorableRenderer.color
            setMode(Mode.Text)
            activeColor = color
            if (startEditing) {
                startTextEntityEditing(editorElement, false)
            }
        }
    }

    enum class Mode {
        None, Crop, Text, Draw
    }
}
