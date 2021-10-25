package one.mixin.android.ui.imageeditor

import android.content.DialogInterface
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.extension.findListener
import one.mixin.android.extension.showKeyboard
import one.mixin.android.ui.common.KeyboardEntryDialogFragment
import one.mixin.android.widget.imageeditor.HiddenEditText
import one.mixin.android.widget.imageeditor.model.EditorElement
import one.mixin.android.widget.imageeditor.renderers.MultiLineTextRenderer

class TextEntryDialogFragment : KeyboardEntryDialogFragment(R.layout.image_editor_text_entry_fragment) {

    private lateinit var hiddenTextEntry: HiddenEditText
    private lateinit var controller: Controller

    private val initColor by lazy { requireArguments().getInt("color") }
    private var activeColor = ColorPaletteAdapter.paletteColors[5]

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        controller = requireNotNull(findListener())

        hiddenTextEntry = HiddenEditText(requireContext())
        (view as ViewGroup).addView(hiddenTextEntry)

        view.setOnClickListener {
            dismissAllowingStateLoss()
        }

        activeColor = initColor
        val element: EditorElement = requireNotNull(requireArguments().getParcelable("element"))
        val incognito = requireArguments().getBoolean("incognito")
        val selectAll = requireArguments().getBoolean("selectAll")

        hiddenTextEntry.setCurrentTextEditorElement(element)
        hiddenTextEntry.setIncognitoKeyboardEnabled(incognito)
        if (selectAll) {
            hiddenTextEntry.selectAll()
        }
        hiddenTextEntry.setOnEditOrSelectionChange { editorElement, textRenderer ->
            controller.zoomToFitText(editorElement, textRenderer)
        }
        hiddenTextEntry.setOnEndEdit {
            dismissAllowingStateLoss()
        }
        hiddenTextEntry.showKeyboard()

        val colorIndicator: ImageView = view.findViewById(R.id.color_indicator)
        colorIndicator.background = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_color_preview)

        val colorPaletteAdapter = ColorPaletteAdapter(
            initColor,
            onColorChanged = { c ->
                activeColor = c
                colorIndicator.drawable.colorFilter = SimpleColorFilter(c)
                controller.onTextColorChange(c)
            }
        ).apply {
            submitList(ColorPaletteAdapter.paletteColors)
        }
        val colorRv: RecyclerView = view.findViewById(R.id.color_rv)
        colorRv.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        colorRv.adapter = colorPaletteAdapter
    }

    override fun onDismiss(dialog: DialogInterface) {
        controller.onTextEntryDialogDismissed(!hiddenTextEntry.text.isNullOrEmpty())
    }

    interface Controller {
        fun onTextEntryDialogDismissed(hasText: Boolean)
        fun zoomToFitText(editorElement: EditorElement, textRenderer: MultiLineTextRenderer)
        fun onTextColorChange(color: Int)
    }

    companion object {
        fun show(
            fragmentManager: FragmentManager,
            editorElement: EditorElement,
            isIncognitoEnabled: Boolean,
            selectAll: Boolean,
            color: Int
        ) {
            val args = Bundle().apply {
                putParcelable("element", editorElement)
                putBoolean("incognito", isIncognitoEnabled)
                putBoolean("selectAll", selectAll)
                putInt("color", color)
            }

            TextEntryDialogFragment().apply {
                arguments = args
                show(fragmentManager, "text_entry")
            }
        }
    }
}

class SimpleColorFilter(@ColorInt color: Int) :
    PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP)
