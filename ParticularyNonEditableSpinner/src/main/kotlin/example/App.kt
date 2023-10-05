package example

import java.awt.*
import java.awt.font.FontRenderContext
import javax.swing.*
import javax.swing.border.Border

fun makeUI(): Component {
  val spinner1 = JSpinner(SpinnerNumberModel(0, 0, 1, .01))
  val editor1 = JSpinner.NumberEditor(spinner1, "0%")
  spinner1.editor = editor1

  val spinner2 = JSpinner(SpinnerNumberModel(0, 0, 100, 1))
  val editor2 = object : JSpinner.NumberEditor(spinner2) {
    override fun updateUI() {
      if (componentCount > 0) {
        val f = textField
        f.border = null // Nimbus
        super.updateUI()
        initTextFieldBorder(f)
      } else {
        super.updateUI()
      }
    }
  }
  spinner2.editor = editor2
  initTextFieldBorder(editor2.textField)

  return JPanel(GridLayout(2, 1)).also {
    it.add(makeTitledPanel("JSpinner+Default", spinner1))
    it.add(makeTitledPanel("JSpinner+StringBorder", spinner2))
    it.border = BorderFactory.createEmptyBorder(10, 5, 10, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

fun initTextFieldBorder(textField: JTextField) {
  EventQueue.invokeLater {
    val b = StringBorder(textField, "%")
    textField.border = if (textField.ui.javaClass.name.contains("SynthFormattedTextFieldUI")) {
      val c = textField.border
      if (c != null) BorderFactory.createCompoundBorder(c, b) else b
    } else {
      b
    }
  }
}

private fun makeTitledPanel(
  title: String,
  cmp: Component,
): Component {
  val p = JPanel(GridBagLayout())
  p.border = BorderFactory.createTitledBorder(title)
  val c = GridBagConstraints()
  c.weightx = 1.0
  c.fill = GridBagConstraints.HORIZONTAL
  c.insets = Insets(5, 5, 5, 5)
  p.add(cmp, c)
  return p
}

private class StringBorder(parent: JComponent, private val str: String) : Border {
  private val insets: Insets
  private val rect: Rectangle

  init {
    val frc = FontRenderContext(null, true, true)
    rect = parent.font.getStringBounds(str, frc).bounds
    insets = Insets(0, 0, 0, rect.width)
  }

  override fun getBorderInsets(c: Component) = insets

  override fun isBorderOpaque() = false

  override fun paintBorder(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    val tx = (x + width - rect.width).toFloat()
    val ty = y - rect.y + (height - rect.height) / 2f
    g2.drawString(str, tx, ty)
    g2.dispose()
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
