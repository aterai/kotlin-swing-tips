package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.color.ColorSpace
import java.awt.image.BufferedImage
import java.awt.image.ColorConvertOp
import javax.swing.* // ktlint-disable no-wildcard-imports

private const val HTML_TEXT = "<html>Html <font color='red'>label</font><br/> Test"

fun makeUI(): Component {
  val p = JPanel(GridLayout(2, 3))
  val label0 = JLabel("Default JLabel")
  p.add(initTitledBorder("JLabel", label0))

  val label1 = JLabel(HTML_TEXT)
  p.add(initTitledBorder("JLabel+Html", label1))

  val label2 = object : JLabel(HTML_TEXT) {
    override fun setEnabled(b: Boolean) {
      super.setEnabled(b)
      val key = if (b) "Label.foreground" else "Label.disabledForeground"
      foreground = UIManager.getColor(key)
    }
  }
  p.add(initTitledBorder("JLabel+Html+", label2))

  val label3 = DisabledHtmlLabel(HTML_TEXT)
  p.add(initTitledBorder("JLabel+Html++", label3))

  val editor1 = JEditorPane("text/html", HTML_TEXT)
  editor1.isOpaque = false
  editor1.isEditable = false
  p.add(initTitledBorder("JEditorPane", editor1))

  val editor2 = JEditorPane("text/html", HTML_TEXT)
  editor2.isOpaque = false
  editor2.isEditable = false
  editor2.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
  editor2.font = UIManager.getFont("Label.font")
  p.add(initTitledBorder("JEditorPane+", editor2))

  val check = JCheckBox("setEnabled", true)
  check.addActionListener { e ->
    val f = (e.source as? JCheckBox)?.isSelected == true
    for (c in p.components) {
      c.isEnabled = f
    }
  }

  return JPanel(BorderLayout()).also {
    it.add(check, BorderLayout.NORTH)
    it.add(p)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun initTitledBorder(title: String, c: JComponent): Component {
  c.border = BorderFactory.createTitledBorder(title)
  return c
}

private class DisabledHtmlLabel(text: String?) : JLabel(text) {
  @Transient private var shadow: BufferedImage? = null
  override fun setEnabled(b: Boolean) {
    val key = if (b) "Label.foreground" else "Label.disabledForeground"
    foreground = UIManager.getColor(key)
    if (!b) {
      val source = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
      val g2 = source.createGraphics()
      g2.paint = Color(0x0, true)
      g2.fillRect(0, 0, width, height)
      // print(g2)
      paint(g2)
      g2.dispose()
      shadow = GRAY_CCO.filter(source, null)
    }
    super.setEnabled(b)
  }

  override fun paintComponent(g: Graphics) {
    if (!isEnabled && shadow != null) {
      g.drawImage(shadow, 0, 0, this)
    } else {
      super.paintComponent(g)
    }
  }

  companion object {
    private val GRAY_CCO = ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null)
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
