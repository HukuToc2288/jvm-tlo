package gui.tabs

import javax.swing.text.*

class WrapEditorKit : StyledEditorKit() {
    private val defaultFactory = WrapColumnFactory()
    override fun getViewFactory(): ViewFactory {
        return defaultFactory
    }
}

internal class WrapColumnFactory : ViewFactory {
    override fun create(elem: Element): View {
        val kind = elem.name
        if (kind != null) {
            when (kind) {
                AbstractDocument.ContentElementName -> {
                    return WrapLabelView(elem)
                }
                AbstractDocument.ParagraphElementName -> {
                    return ParagraphView(elem)
                }
                AbstractDocument.SectionElementName -> {
                    return BoxView(elem, View.Y_AXIS)
                }
                StyleConstants.ComponentElementName -> {
                    return ComponentView(elem)
                }
                StyleConstants.IconElementName -> {
                    return IconView(elem)
                }
            }
        }
        return LabelView(elem)
    }
}

internal class WrapLabelView(elem: Element?) : LabelView(elem) {
    override fun getMinimumSpan(axis: Int): Float {
        return when (axis) {
            X_AXIS -> 0f
            Y_AXIS -> super.getMinimumSpan(axis)
            else -> throw IllegalArgumentException("Invalid axis: $axis")
        }
    }
}