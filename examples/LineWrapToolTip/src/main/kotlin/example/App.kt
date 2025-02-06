package example

import java.awt.*
import java.awt.event.MouseEvent
import java.awt.font.LineBreakMeasurer
import java.awt.font.TextAttribute
import java.text.AttributedString
import javax.swing.*
import kotlin.math.min

fun makeUI(): Component {
  val very = "very ".repeat(10)
  val txt = "A ${very}long tooltip that must be line wrap"
  val b1 = JButton("JToolTip(Default)")
  b1.toolTipText = "$txt: 1"
  val b2 = makeButton("LineWrapToolTip: Long")
  b2.toolTipText = "$txt: 2"
  val b3 = makeButton("LineWrapToolTip: Short")
  b3.toolTipText = "ToolTipText: 3"
  val field = makeTextField("$txt: 4")
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

private fun makeButton(title: String) = object : JButton(title) {
  private var tip: JToolTip? = null

  override fun createToolTip() = tip ?: LineWrapToolTip().also {
    it.setComponent(this)
  }

  override fun getToolTipText(e: MouseEvent): String? {
    val tipText = super.getToolTipText(e)
    EventQueue.invokeLater {
      if (tip != null) {
        SwingUtilities.getWindowAncestor(tip)?.also {
          if (it.type == Window.Type.POPUP) {
            it.pack()
          }
        }
      }
    }
    return tipText
  }
}

private fun makeTextField(txt: String): JTextField {
  val field = object : JTextField(20) {
    private var tip: JToolTip? = null

    override fun createToolTip() = tip ?: LineWrapToolTip().also {
      it.setComponent(this)
    }

    override fun getToolTipText(e: MouseEvent): String? {
      val tipText = text
      EventQueue.invokeLater {
        if (tip != null) {
          SwingUtilities.getWindowAncestor(tip)?.also {
            if (it.type == Window.Type.POPUP) {
              it.pack()
            }
          }
        }
      }
      return tipText
    }
  }
  field.text = txt
  return field
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
    val version = System.getProperty("java.specification.version")
    return if (version.toDouble() >= JAVA_21) {
      getTextAreaSize21(d)
    } else {
      getTextAreaSize8(d)
    }
  }

  private fun getTextAreaSize8(d: Dimension): Dimension {
    val font = textArea.font
    MEASURER.font = font
    MEASURER.text = textArea.text
    val pad = getTextAreaPaddingWidth(insets)
    d.width = min(TIP_WIDTH, MEASURER.preferredSize.width + pad)
    val attr = AttributedString(textArea.text)
    attr.addAttribute(TextAttribute.FONT, font)
    val aci = attr.iterator
    val fm = textArea.getFontMetrics(font)
    val frc = fm.fontRenderContext
    val lbm = LineBreakMeasurer(aci, frc)
    var y = 0f
    while (lbm.position < aci.endIndex) {
      val tl = lbm.nextLayout(TIP_WIDTH.toFloat())
      y += tl.descent + tl.leading + tl.ascent
    }
    d.height = y.toInt() + getTextAreaPaddingHeight(insets)
    return d
  }

  private fun getTextAreaSize21(d: Dimension): Dimension {
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
    private const val JAVA_21 = 21.0
    private val MEASURER = JLabel(" ")
    private const val TIP_WIDTH = 200
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
