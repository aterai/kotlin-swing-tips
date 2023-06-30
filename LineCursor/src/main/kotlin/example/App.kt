package example

import java.awt.*
import javax.swing.*
import javax.swing.text.DefaultCaret

fun makeUI(): Component {
  val textArea = LineCursorTextArea("Line Cursor Test\n\n*******")
  val check = JCheckBox("LineWrap")
  check.addActionListener { e ->
    textArea.lineWrap = (e.source as? JCheckBox)?.isSelected == true
    textArea.requestFocusInWindow()
  }
  return JPanel(BorderLayout()).also {
    it.add(check, BorderLayout.NORTH)
    it.add(JScrollPane(textArea))
    it.preferredSize = Dimension(320, 240)
  }
}

private class LineCursorTextArea(text: String?) : JTextArea(text) {
  // constructor() : super()
  // constructor(doc: Document?) : super(doc)
  // constructor(doc: Document?, text: String?, rows: Int, columns: Int) : super(doc, text, rows, columns)
  // constructor(rows: Int, columns: Int) : super(rows, columns)
  // constructor(text: String?) : super(text)
  // constructor(text: String?, rows: Int, columns: Int) : super(text, rows, columns)

  override fun updateUI() {
    super.updateUI()
    val caret = object : DefaultCaret() {
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
    super.paintComponent(g)
    val g2 = g.create() as? Graphics2D ?: return
    (caret as? DefaultCaret)?.also {
      val i = insets
      val x = size.width - i.left - i.right
      val y = it.y + it.height - 1
      g2.paint = LINE_COLOR
      g2.drawLine(i.left, y, x, y)
    }
    g2.dispose()
  }

  companion object {
    private val LINE_COLOR = Color.BLUE
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
