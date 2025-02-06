package example

import com.sun.java.swing.plaf.windows.WindowsCheckBoxUI
import java.awt.*
import javax.swing.*
import javax.swing.plaf.basic.BasicCheckBoxUI
import javax.swing.plaf.basic.BasicHTML
import javax.swing.text.Position.Bias
import javax.swing.text.View
import kotlin.math.abs
import kotlin.math.roundToInt

private const val TEXT =
  "<html>The vertical alignment of this text gets offset when the font changes."

fun makeUI(): Component {
  val check1 = JCheckBox(TEXT)
  check1.verticalTextPosition = SwingConstants.TOP

  val check2 = object : JCheckBox(TEXT) {
    override fun updateUI() {
      super.updateUI()
      val tmp = if (ui is WindowsCheckBoxUI) {
        WindowsVerticalAlignmentCheckBoxUI()
      } else {
        BasicVerticalAlignmentCheckBoxUI()
      }
      setUI(tmp)
      verticalTextPosition = TOP
    }
  }

  val list = listOf(check1, check2)
  val font0 = check1.font
  val font1 = font0.deriveFont(20f)

  val button = JToggleButton("setFont: 24pt")
  button.addActionListener {
    val flag = button.isSelected
    for (c in list) {
      c.font = if (flag) font1 else font0
    }
  }
  val p = JPanel(GridLayout(1, 2, 2, 2))
  p.add(makeTitledPanel("SwingConstants.TOP", check1))
  p.add(makeTitledPanel("First line center", check2))

  return JPanel(BorderLayout()).also {
    it.add(p)
    it.add(button, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(
  title: String,
  c: Component,
): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
}

private object HtmlViewUtils {
  fun getFirstLineCenterY(
    text: String?,
    c: AbstractButton,
    iconRect: Rectangle,
  ): Int {
    var y = 0
    if (text != null && c.verticalTextPosition == SwingConstants.TOP) {
      (c.getClientProperty(BasicHTML.propertyKey) as? View)?.also {
        runCatching {
          val e = it.element.getElement(0)
          val b = Bias.Forward
          val s = it.modelToView(e.startOffset, Bias.Forward, e.endOffset, b, Rectangle())
          y = (abs(s.bounds.height - iconRect.height) / 2f).roundToInt()
        }
      }
    }
    return y
  }
}

private class WindowsVerticalAlignmentCheckBoxUI : WindowsCheckBoxUI() {
  private val viewRect = Rectangle()
  private val iconRect = Rectangle()
  private val textRect = Rectangle()

  @Synchronized
  override fun paint(
    g: Graphics,
    c: JComponent,
  ) {
    if (c !is AbstractButton) {
      return
    }
    val f = c.font
    g.font = f
    SwingUtilities.calculateInnerArea(c, viewRect)
    iconRect.setBounds(0, 0, 0, 0)
    textRect.setBounds(0, 0, 0, 0)
    val text = SwingUtilities.layoutCompoundLabel(
      c,
      c.getFontMetrics(f),
      c.text,
      defaultIcon,
      c.verticalAlignment,
      c.horizontalAlignment,
      c.verticalTextPosition,
      c.horizontalTextPosition,
      viewRect,
      iconRect,
      textRect,
      if (c.text != null) c.iconTextGap else 0,
    )

    // Paint the radio button
    val y = HtmlViewUtils.getFirstLineCenterY(text, c, iconRect)
    defaultIcon.paintIcon(c, g, iconRect.x, iconRect.y + y)

    // Draw the Text
    text?.also {
      (c.getClientProperty(BasicHTML.propertyKey) as? View)?.paint(g, textRect)
        ?: paintText(g, c, textRect, text)
      if (c.hasFocus() && c.isFocusPainted) {
        paintFocus(g, textRect, c.size)
      }
    }
  }

  override fun paintFocus(
    g: Graphics,
    txtRect: Rectangle,
    sz: Dimension,
  ) {
    if (txtRect.width > 0 && txtRect.height > 0) {
      super.paintFocus(g, txtRect, sz)
    }
  }
}

private class BasicVerticalAlignmentCheckBoxUI : BasicCheckBoxUI() {
  private val viewRect = Rectangle()
  private val iconRect = Rectangle()
  private val textRect = Rectangle()

  @Synchronized
  override fun paint(
    g: Graphics,
    c: JComponent,
  ) {
    if (c !is AbstractButton) {
      return
    }
    val f = c.font
    g.font = f
    SwingUtilities.calculateInnerArea(c, viewRect)
    iconRect.setBounds(0, 0, 0, 0)
    textRect.setBounds(0, 0, 0, 0)
    val text = SwingUtilities.layoutCompoundLabel(
      c,
      c.getFontMetrics(f),
      c.text,
      defaultIcon,
      c.verticalAlignment,
      c.horizontalAlignment,
      c.verticalTextPosition,
      c.horizontalTextPosition,
      viewRect,
      iconRect,
      textRect,
      if (c.text != null) c.iconTextGap else 0,
    )

    // Paint the radio button
    val y = HtmlViewUtils.getFirstLineCenterY(text, c, iconRect)
    defaultIcon.paintIcon(c, g, iconRect.x, iconRect.y + y)

    // Draw the Text
    text?.also {
      (c.getClientProperty(BasicHTML.propertyKey) as? View)?.paint(g, textRect)
        ?: paintText(g, c, textRect, text)
      if (c.hasFocus() && c.isFocusPainted) {
        paintFocus(g, textRect, c.size)
      }
    }
  }

  override fun paintFocus(
    g: Graphics,
    txtRect: Rectangle,
    sz: Dimension,
  ) {
    if (txtRect.width > 0 && txtRect.height > 0) {
      super.paintFocus(g, txtRect, sz)
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
