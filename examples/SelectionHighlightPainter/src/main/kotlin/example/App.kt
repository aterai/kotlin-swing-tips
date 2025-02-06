package example

import java.awt.*
import javax.swing.*
import javax.swing.text.DefaultCaret
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter
import javax.swing.text.JTextComponent
import javax.swing.text.View

fun makeUI(): Component {
  val field1 = JTextField("0987654321").also {
    it.selectedTextColor = Color.RED
    it.selectionColor = Color.GREEN
  }

  val selectionPainter = object : DefaultHighlightPainter(Color.WHITE) {
    override fun paintLayer(
      g: Graphics,
      offs0: Int,
      offs1: Int,
      bounds: Shape,
      c: JTextComponent,
      view: View,
    ) = super.paintLayer(g, offs0, offs1, bounds, c, view).also {
      if (it is Rectangle) {
        g.color = Color.ORANGE
        g.fillRect(it.x, it.y + it.height - 2, it.width, 2)
      }
    }
  }
  val caret = object : DefaultCaret() {
    override fun getSelectionPainter() = selectionPainter
  }
  val field2 = JTextField("123465789735")
  caret.blinkRate = field2.caret.blinkRate
  field2.selectedTextColor = Color.RED
  field2.caret = caret

  val box = Box.createVerticalBox().also {
    it.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
    it.add(makeTitledPanel("Default", JTextField("12345")))
    it.add(Box.createVerticalStrut(10))
    it.add(makeTitledPanel("JTextComponent#setSelectionColor(...)", field1))
    it.add(Box.createVerticalStrut(10))
    it.add(makeTitledPanel("JTextComponent#setCaret(...)", field2))
  }

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
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
