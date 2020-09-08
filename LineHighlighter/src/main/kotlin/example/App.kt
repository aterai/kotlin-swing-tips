package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.DefaultCaret

fun makeUI(): Component {
  val textArea = HighlightCursorTextArea()
  textArea.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
  textArea.text = "Highlight Cursor Test\n\n**************************************"
  val check = JCheckBox("LineWrap")
  check.addActionListener {
    textArea.lineWrap = check.isSelected
    textArea.requestFocusInWindow()
  }
  val scroll = JScrollPane(textArea)
  scroll.viewport.background = Color.WHITE

  return JPanel(BorderLayout()).also {
    it.add(check, BorderLayout.NORTH)
    it.add(scroll)
    it.preferredSize = Dimension(320, 240)
  }
}

private class HighlightCursorTextArea : JTextArea() {
  override fun updateUI() {
    super.updateUI()
    isOpaque = false
    val caret = object : DefaultCaret() {
      // [UnsynchronizedOverridesSynchronized]
      // Unsynchronized method damage overrides synchronized method in DefaultCaret
      @Synchronized
      override fun damage(r: Rectangle?) {
        if (r != null) {
          val c = component
          x = 0
          y = r.y
          width = c.size.width
          height = r.height
          c.repaint()
        }
      }
    }
    caret.blinkRate = UIManager.getInt("TextArea.caretBlinkRate")
    setCaret(caret)
  }

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    (caret as? DefaultCaret)?.also {
      val i = insets
      val h = it.height
      val y = it.y
      g2.paint = LINE_COLOR
      g2.fillRect(i.left, y, size.width - i.left - i.right, h)
    }
    g2.dispose()
    super.paintComponent(g)
  }

  companion object {
    private val LINE_COLOR = Color(0xFA_FA_DC)
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
