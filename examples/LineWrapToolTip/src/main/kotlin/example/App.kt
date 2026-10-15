package example

import java.awt.*
import java.awt.event.MouseEvent
import java.awt.font.LineBreakMeasurer
import java.awt.font.TextAttribute
import java.text.AttributedString
import javax.swing.*
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

fun createUI(): Component {
  val very = "very ".repeat(10)
  val txt = "A ${very}long tooltip that must be line wrap"
  val b1 = JButton("JToolTip(Default)")
  b1.toolTipText = "$txt: 1"
  val b2 = createButton("LineWrapToolTip: Long")
  b2.toolTipText = "$txt: 2"
  val b3 = createButton("LineWrapToolTip: Short")
  b3.toolTipText = "ToolTipText: 3"
  val field = createTextField("$txt: 4")
  field.toolTipText = field.text
  val box = Box.createVerticalBox()
  box.add(b1)
  box.add(Box.createVerticalStrut(10))
  box.add(b2)
  box.add(Box.createVerticalStrut(10))
  box.add(b3)
  box.add(Box.createVerticalGlue())
  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.WEST)
    it.add(field, BorderLayout.SOUTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createButton(title: String) = object : JButton(title) {
  private var tip: JToolTip? = null

  override fun createToolTip() = tip ?: LineWrapToolTip().also {
    it.setComponent(this)
    tip = it
  }

  override fun getToolTipText(e: MouseEvent): String? {
    val tipText = super.getToolTipText(e)
    packToolTipWindow(tip)
    return tipText
  }
}

private fun createTextField(txt: String): JTextField {
  val field = object : JTextField(20) {
    private var tip: JToolTip? = null

    override fun createToolTip() = tip ?: LineWrapToolTip().also {
      it.setComponent(this)
      tip = it
    }

    override fun getToolTipText(e: MouseEvent): String? {
      val tipText = text
      packToolTipWindow(tip)
      return tipText
    }
  }
  field.text = txt
  return field
}

// The tooltip may not have been created yet when getToolTipText(...) is first called
// (ToolTipManager calls it on mouseEntered), so a null check is required.
private fun packToolTipWindow(tip: JToolTip?) {
  EventQueue.invokeLater {
    tip
      ?.let { SwingUtilities.getWindowAncestor(it) }
      ?.takeIf { it.type == Window.Type.POPUP }
      ?.pack()
  }
}

private class LineWrapToolTip : JToolTip() {
  private val textArea = JTextArea(0, 20)

  init {
    textArea.lineWrap = true
    textArea.wrapStyleWord = true
    textArea.isOpaque = true
    // textArea.columns = 20
    LookAndFeel.installColorsAndFont(
      textArea,
      "ToolTip.background",
      "ToolTip.foreground",
      "ToolTip.font",
    )
    layout = BorderLayout()
    add(textArea)
  }

  override fun setLayout(mgr: LayoutManager) {
    super.setLayout(mgr)
  }

  override fun add(comp: Component): Component = super.add(comp)

  override fun getPreferredSize(): Dimension {
    val d = layout.preferredLayoutSize(this)
    return if (JAVA21_OR_LATER) getPreferredSizeJava21(d) else getPreferredSizeJava8(d)
  }

  private fun getPreferredSizeJava8(d: Dimension): Dimension {
    val font = textArea.font
    MEASURER.font = font
    MEASURER.text = textArea.text
    val i = insets
    val pad = getTextAreaPaddingWidth(i)
    d.width = min(TIP_WIDTH, MEASURER.preferredSize.width + pad)

    // JDK-8226513 JEditorPane is shown with incorrect size - Java Bug System
    // https://bugs.openjdk.org/browse/JDK-8226513
    // Wrap the text with the width actually available to the text,
    // not the whole tooltip width.
    val wrappingWidth = max(1f, (d.width - pad).toFloat())
    d.height = ceil(getWrappedTextHeight(font, wrappingWidth)).toInt() +
      getTextAreaPaddingHeight(i)
    return d
  }

  private fun getWrappedTextHeight(font: Font, wrappingWidth: Float): Float {
    val text = textArea.text
    var height = 0f
    // AttributedString#addAttribute(...) throws IllegalArgumentException for an empty text
    if (!text.isNullOrEmpty()) {
      val attr = AttributedString(text)
      attr.addAttribute(TextAttribute.FONT, font)
      val aci = attr.iterator
      val frc = textArea.getFontMetrics(font).fontRenderContext
      val lbm = LineBreakMeasurer(aci, frc)
      while (lbm.position < aci.endIndex) {
        val tl = lbm.nextLayout(wrappingWidth)
        height += tl.ascent + tl.descent + tl.leading
      }
    }
    return height
  }

  private fun getPreferredSizeJava21(d: Dimension): Dimension {
    MEASURER.font = textArea.font
    MEASURER.text = textArea.text
    val pad = getTextAreaPaddingWidth(insets)
    d.width = min(d.width, MEASURER.preferredSize.width + pad)
    return d
  }

  private fun getTextAreaPaddingWidth(i: Insets): Int {
    var caretMargin = -1
    var property = UIManager.get("Caret.width")
    if (property is Number) {
      caretMargin = property.toInt()
    }
    property = textArea.getClientProperty("caretWidth")
    if (property is Number) {
      caretMargin = property.toInt()
    }
    if (caretMargin < 0) {
      caretMargin = 1
    }
    val ti = textArea.insets
    return i.left + i.right + ti.left + ti.right + caretMargin
  }

  private fun getTextAreaPaddingHeight(i: Insets): Int {
    val ti = textArea.insets
    return i.top + i.bottom + ti.top + ti.bottom
  }

  override fun setTipText(tipText: String) {
    val oldValue = textArea.text
    textArea.text = tipText
    firePropertyChange("tiptext", oldValue, tipText)
    if (oldValue != tipText) {
      revalidate()
      repaint()
    }
  }

  override fun getTipText(): String? = textArea?.text

  companion object {
    private val JAVA21_OR_LATER = isJava21OrLater()
    private val MEASURER = JLabel(" ")
    private const val TIP_WIDTH = 200

    private fun isJava21OrLater(): Boolean {
      // "1.8", "11", "17", "21", ...
      val version = System.getProperty("java.specification.version")
      return version.toDouble() >= 21.0
    }
  }
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    }.onFailure {
      it.printStackTrace()
      Toolkit.getDefaultToolkit().beep()
    }
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
