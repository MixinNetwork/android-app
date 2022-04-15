package one.mixin.android.ui.imageeditor

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.collection.arraySetOf
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDispose
import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants.Account.PREF_INCOGNITO_KEYBOARD
import one.mixin.android.R
import one.mixin.android.databinding.FragmentImageEditorBinding
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.createImageTemp
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getImageCachePath
import one.mixin.android.extension.heavyClickVibrate
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.save
import one.mixin.android.extension.toast
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.imageeditor.ColorPaletteAdapter.Companion.paletteColors
import one.mixin.android.ui.imageeditor.ImageEditorActivity.Companion.ARGS_EDITOR_RESULT
import one.mixin.android.ui.imageeditor.ImageEditorActivity.Companion.ARGS_IMAGE_URI
import one.mixin.android.ui.imageeditor.ImageEditorActivity.Companion.ARGS_NEXT_TITLE
import one.mixin.android.widget.PrevNextView
import one.mixin.android.widget.imageeditor.ColorableRenderer
import one.mixin.android.widget.imageeditor.ImageEditorView
import one.mixin.android.widget.imageeditor.SelectableRenderer
import one.mixin.android.widget.imageeditor.ThrottledDebouncer
import one.mixin.android.widget.imageeditor.model.EditorElement
import one.mixin.android.widget.imageeditor.model.EditorModel
import one.mixin.android.widget.imageeditor.renderers.BezierDrawingRenderer
import one.mixin.android.widget.imageeditor.renderers.MultiLineTextRenderer

class ImageEditorFragment : BaseFragment(), TextEntryDialogFragment.Controller {
    companion object {
        const val TAG = "ImageEditorFragment"

        private const val MAX_IMAGE_SIZE = 4096

        fun newInstance(imageUri: Uri, nextTitle: String? = null) = ImageEditorFragment().withArgs {
            putParcelable(ARGS_IMAGE_URI, imageUri)
            nextTitle?.let { putString(ARGS_NEXT_TITLE, it) }
        }
    }

    private var currentSelection: EditorElement? = null
    private var currentMode = Mode.None
    private var activeColor = paletteColors[5]

    private var undoAvailable: Boolean = false
    private var redoAvailable: Boolean = false

    private val imageUri: Uri by lazy { requireArguments().getParcelable(ARGS_IMAGE_URI)!! }

    private val deleteFadeDebouncer = ThrottledDebouncer(500)
    private var wasInTrashHitZone = false

    private val currentElementIds = arraySetOf<String>()

    private val colorPaletteAdapter = ColorPaletteAdapter(
        paletteColors[5],
        onColorChanged = { c ->
            onColorChanged(c)
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
            requireArguments().getString(ARGS_NEXT_TITLE)?.let {
                nextTv.text = it
            }
            closeIv.setOnClickListener { activity?.onBackPressed() }
            nextTv.setOnClickListener { goNext() }
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
            setDragListener(dragListener)
            setDrawListener(drawListener)
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
                .setNegativeButton(R.string.Cancel) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(R.string.Exit) { dialog, _ ->
                    dialog.dismiss()
                    activity?.finish()
                }.show()
            return true
        }
        return super.onBackPressed()
    }

    private fun onModeSet(mode: Mode) {
        binding.apply {
            imageEditorView.mode = ImageEditorView.Mode.MoveAndResize
            imageEditorView.doneTextEditing()

            if (mode != Mode.Crop) {
                imageEditorView.model.doneCrop()
            }

            binding.imageEditorView.model.trash.flags.setVisible(mode == Mode.Delete).persist()

            currentElementIds.clear()

            when (mode) {
                Mode.None -> currentSelection = null
                Mode.Crop -> imageEditorView.model.startCrop()
                Mode.Text -> addText()
                Mode.Draw -> imageEditorView.startDrawing(binding.sizeSeekbar.progressFloat / 100, Paint.Cap.ROUND, false)
                else -> {}
            }
        }
    }

    private fun setMode(mode: Mode, notify: Boolean = true) {
        currentMode = mode

        when (mode) {
            Mode.None -> presentModeNone()
            Mode.Crop -> presentModeCrop()
            Mode.Text -> presentModeText(!notify)
            Mode.Draw -> presentModeDraw()
            Mode.Delete -> {}
        }

        if (notify) {
            onModeSet(mode)
        }
    }

    private fun goNext() {
        RxPermissions(requireActivity())
            .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .autoDispose(stopScope)
            .subscribe { granted ->
                if (granted) {
                    renderAndSave()
                } else {
                    context?.openPermissionSetting()
                }
            }
    }

    private fun onColorChanged(c: Int) {
        activeColor = c
        binding.imageEditorView.setDrawingBrushColor(c)
        if (currentSelection != null) {
            val render = currentSelection?.renderer
            if (render is ColorableRenderer) {
                render.color = c
            }
        }
    }

    private fun cancel() {
        when (currentMode) {
            Mode.Text -> binding.imageEditorView.model.clearRendererByIds(currentElementIds, false)
            Mode.Draw -> binding.imageEditorView.model.clearRendererByIds(currentElementIds, true)
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

    private fun presentModeText(showTools: Boolean) {
        binding.apply {
            editorLl.isVisible = showTools
            tabLl.isVisible = false
            title.isVisible = false
            typeTv.isVisible = true
            undoRedoView.isVisible = false
            typeTv.text = getString(R.string.Text)
            colorRv.isVisible = showTools
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
            typeTv.text = getString(R.string.Crop)
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
        binding.imageEditorView.startTextEditing(textElement)

        TextEntryDialogFragment.show(
            childFragmentManager,
            textElement,
            defaultSharedPreferences.getBoolean(PREF_INCOGNITO_KEYBOARD, false),
            selectAll,
            activeColor
        )
    }

    private fun addText() {
        val initialText = ""
        val color = activeColor
        val render = MultiLineTextRenderer(initialText, color, MultiLineTextRenderer.Mode.REGULAR)
        val element = EditorElement(render, EditorModel.Z_TEXT)
        binding.imageEditorView.apply {
            model.addElementCentered(element, 1f)
            invalidate()
        }
        currentSelection = element
        currentElementIds.add(element.id.toString())
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

    private fun renderAndSave() = lifecycleScope.launch {
        if (viewDestroyed()) return@launch

        val dialog = indeterminateProgressDialog(message = R.string.pb_dialog_message).apply {
            setCancelable(false)
        }
        val file = requireContext().getImageCachePath().createImageTemp()
        val saveResult = withContext(Dispatchers.IO) {
            kotlin.runCatching {
                val image = binding.imageEditorView.model.render(requireContext())
                image.save(file)
            }
        }
        dialog.dismiss()

        if (saveResult.isFailure) {
            file.delete()
            toast(R.string.save_failure)
            return@launch
        }

        val result = Intent().apply {
            putExtra(ARGS_EDITOR_RESULT, file.toUri())
        }
        requireActivity().apply {
            setResult(Activity.RESULT_OK, result)
            finish()
        }
    }

    override fun onTextEntryDialogDismissed(hasText: Boolean) {
        binding.imageEditorView.doneTextEditing()
        if (hasText) {
            setMode(Mode.Text, false)
        } else {
            setMode(Mode.None)
        }
    }

    override fun zoomToFitText(editorElement: EditorElement, textRenderer: MultiLineTextRenderer) {
        binding.imageEditorView.zoomToFitText(editorElement, textRenderer)
    }

    override fun onTextColorChange(color: Int) {
        onColorChanged(color)
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

    private val dragListener = object : ImageEditorView.DragListener {
        override fun onDragStarted(editorElement: EditorElement?) {
            if (currentMode == Mode.Crop) {
                return
            }
            currentSelection = if (editorElement == null || editorElement.renderer is BezierDrawingRenderer) {
                null
            } else {
                editorElement
            }
            if (binding.imageEditorView.mode == ImageEditorView.Mode.MoveAndResize) {
                setMode(Mode.Delete)
            }
        }

        override fun onDragMoved(editorElement: EditorElement?, isInTrashHitZone: Boolean) {
            if (currentMode == Mode.Crop || editorElement == null) {
                return
            }
            if (isInTrashHitZone) {
                deleteFadeDebouncer.publish {
                    if (!wasInTrashHitZone) {
                        wasInTrashHitZone = true
                        requireContext().heavyClickVibrate()
                    }
                    editorElement.animatePartialFadeOut(binding.imageEditorView::invalidate)
                }
            } else {
                deleteFadeDebouncer.publish {
                    wasInTrashHitZone = false
                    editorElement.animatePartialFadeIn(binding.imageEditorView::invalidate)
                }
            }
        }

        override fun onDragEnded(editorElement: EditorElement?, isInTrashHitZone: Boolean) {
            wasInTrashHitZone = false
            if (currentMode == Mode.Crop || currentMode == Mode.Draw) {
                return
            }
            if (isInTrashHitZone) {
                deleteFadeDebouncer.clear()
                binding.imageEditorView.deleteElement(currentSelection)
                currentSelection = null
            } else if (editorElement != null && editorElement.renderer is MultiLineTextRenderer) {
                editorElement.animatePartialFadeIn(binding.imageEditorView::invalidate)
            }
            setMode(Mode.None)
        }
    }

    private val drawListener = ImageEditorView.DrawListener { id -> currentElementIds.add(id) }

    private val tapListener = object : ImageEditorView.TapListener {
        override fun onEntityDown(editorElement: EditorElement?) {
        }

        override fun onEntitySingleTap(editorElement: EditorElement?) {
            setCurrentSelection(editorElement)
            if (currentSelection != null) {
                if (editorElement?.renderer is MultiLineTextRenderer) {
                    setTextElement(editorElement, editorElement.renderer as ColorableRenderer, binding.imageEditorView.isTextEditing)
                }
            } else {
                setMode(Mode.None)
            }
        }

        override fun onEntityDoubleTap(editorElement: EditorElement) {
            setCurrentSelection(editorElement)
            if (editorElement.renderer is MultiLineTextRenderer) {
                setTextElement(editorElement, editorElement.renderer as ColorableRenderer, true)
            }
        }

        private fun setTextElement(editorElement: EditorElement, colorableRenderer: ColorableRenderer, startEditing: Boolean) {
            val color = colorableRenderer.color
            setMode(Mode.Text, startEditing)
            activeColor = color
            if (startEditing) {
                startTextEntityEditing(editorElement, false)
            }
        }

        private fun setCurrentSelection(selection: EditorElement?) {
            setSelectionState(currentSelection, false)
            currentSelection = selection
            setSelectionState(currentSelection, true)
            binding.imageEditorView.invalidate()
        }

        private fun setSelectionState(editorElement: EditorElement?, selected: Boolean) {
            if (editorElement != null && editorElement.renderer is SelectableRenderer) {
                (editorElement.renderer as SelectableRenderer).onSelected(selected)
            }
            binding.imageEditorView.model.setSelected(if (selected) editorElement else null)
        }
    }

    enum class Mode {
        None, Crop, Text, Draw, Delete,
    }
}
