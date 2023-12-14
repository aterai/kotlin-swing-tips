package example

import java.awt.*
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.font.TextLayout
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

fun makeUI(): Component {
  val field1 = JPasswordField()
  val b = Box.createHorizontalBox().also {
    it.add(JLabel("Password: "))
    it.add(field1)
    it.add(Box.createHorizontalGlue())
  }
  val field2 = WatermarkPasswordField()
  val box = Box.createVerticalBox().also {
    it.border = BorderFactory.createEmptyBorder(16, 16, 16, 16)
    it.add(makeTitledPanel("JPasswordField", b))
    it.add(Box.createVerticalStrut(16))
    it.add(makeTitledPanel("InputHintPasswordField", field2))
  }
  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
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

private class WatermarkPasswordField : JPasswordField(), FocusListener, DocumentListener {
  private var showWatermark = true

  init {
    addFocusListener(this)
    document.addDocumentListener(this)
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    if (showWatermark) {
      val g2 = g.create() as? Graphics2D ?: return
      val frc = g2.fontRenderContext
      val tl = TextLayout("Password", font, frc)
      g2.paint = if (hasFocus()) Color.GRAY else Color.BLACK
      val baseline = getBaseline(width, height)
      tl.draw(g2, insets.left + 1f, baseline.toFloat())
      g2.dispose()
    }
  }

  override fun focusGained(e: FocusEvent) {
    repaint()
  }

  override fun focusLost(e: FocusEvent) {
    update()
  }

  override fun insertUpdate(e: DocumentEvent) {
    update()
  }

  override fun removeUpdate(e: DocumentEvent) {
    update()
  }

  override fun changedUpdate(e: DocumentEvent) {
    // not needed
  }

  private fun update() {
    showWatermark = password.isEmpty()
    repaint()
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
