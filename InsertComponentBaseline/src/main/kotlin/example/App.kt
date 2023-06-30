package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val textPane = JTextPane()
  textPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
  textPane.replaceSelection(" Default: ")
  textPane.insertComponent(JCheckBox("CheckBox"))

  val check1 = JCheckBox("JComponent#setAlignmentY(...)")
  val d1 = check1.preferredSize
  var baseline = check1.getBaseline(d1.width, d1.height)
  check1.alignmentY = baseline / d1.height.toFloat()
  textPane.replaceSelection("\n\n Baseline: ")
  textPane.insertComponent(check1)

  val check2 = JCheckBox("setAlignmentY+setCursor+...")
  check2.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
  check2.isOpaque = false
  check2.isFocusable = false
  val d2 = check2.preferredSize
  baseline = check2.getBaseline(d2.width, d2.height)
  check2.alignmentY = baseline / d2.height.toFloat()
  textPane.replaceSelection("\n\n Baseline+Cursor: ")
  textPane.insertComponent(check2)

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(textPane))
    it.border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
    it.preferredSize = Dimension(320, 240)
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
