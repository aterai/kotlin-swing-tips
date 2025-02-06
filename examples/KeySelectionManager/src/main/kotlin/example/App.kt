package example

import java.awt.*
import javax.swing.*
import javax.swing.JComboBox.KeySelectionManager

fun makeUI(): Component {
  val box = Box.createVerticalBox()
  box.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

  val title0 = "BasicComboBoxUI#DefaultKeySelectionManager"
  val combo0 = JComboBox(makeModel())
  box.add(makeTitledPanel(title0, combo0))
  box.add(Box.createVerticalStrut(5))

  val title1 = "disable JComboBox#selectWithKeyChar(...)"
  val combo1 = object : JComboBox<String>(makeModel()) {
    override fun selectWithKeyChar(keyChar: Char) = false
  }
  box.add(makeTitledPanel(title1, combo1))
  box.add(Box.createVerticalStrut(5))

  val title2 = "disable KeySelectionManager#selectionForKey(...)"
  val combo2 = JComboBox(makeModel())
  combo2.keySelectionManager = KeySelectionManager { _, _ -> -1 }
  box.add(makeTitledPanel(title2, combo2))

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

private fun makeModel() = DefaultComboBoxModel(
  arrayOf("a", "ab", "abc", "b1", "b2", "b3"),
)

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
