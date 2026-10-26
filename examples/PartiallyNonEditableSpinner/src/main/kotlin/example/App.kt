package example

import java.awt.*
import javax.swing.*
import javax.swing.border.Border
import javax.swing.plaf.synth.SynthUI
import javax.swing.text.JTextComponent

fun createUI(): Component {
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
    it.add(createTitledPanel("JSpinner+Default", spinner1))
    it.add(createTitledPanel("JSpinner+StringBorder", spinner2))
    it.border = BorderFactory.createEmptyBorder(10, 5, 10, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun initTextFieldBorder(textField: JTextField) {
  // The child JFormattedTextField is updated after this editor by
  // SwingUtilities.updateComponentTreeUI(...), so wait until its new border is installed
  EventQueue.invokeLater {
    val b = StringBorder("%")
    // Synth(Nimbus) paints the text field background with its border, so keep it
    textField.border = if (textField.ui is SynthUI) {
      val c = textField.border
      if (c != null) BorderFactory.createCompoundBorder(c, b) else b
    } else {
      b
    }
  }
}

private fun createTitledPanel(
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

private class StringBorder(
  private val text: String,
) : Border {
  override fun getBorderInsets(c: Component): Insets {
    // Measure with the current font of the component so that the insets
    // follow font and LookAndFeel changes
    val w = c.getFontMetrics(c.font).stringWidth(text)
    return Insets(0, 0, 0, w)
  }

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
    // Use the same text antialiasing as the text field
    val hints = Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints")
    if (hints is Map<*, *>) {
      g2.addRenderingHints(hints)
    }
    val font = c.font
    g2.font = font
    g2.paint = if (c is JTextComponent && !c.isEnabled) c.disabledTextColor else c.foreground
    val fm = c.getFontMetrics(font)
    val tx = x + width - fm.stringWidth(text)
    val ty = y + (height - fm.height) / 2 + fm.ascent
    g2.drawString(text, tx, ty)
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
