package example

import java.awt.*
import java.util.Locale
import javax.swing.*
import javax.swing.plaf.FontUIResource

private val FONT12 = Font(Font.SANS_SERIF, Font.PLAIN, 12)
private val FONT24 = Font(Font.SANS_SERIF, Font.PLAIN, 24)
private val FONT32 = Font(Font.SANS_SERIF, Font.PLAIN, 32)

fun makeToolBar(parent: JComponent): JToolBar {
  val tgb12 = JToggleButton("12")
  tgb12.addActionListener { updateFont(FONT12, parent) }
  val tgb24 = JToggleButton("24")
  tgb24.addActionListener { updateFont(FONT24, parent) }
  val tgb32 = JToggleButton("32")
  tgb32.addActionListener { updateFont(FONT32, parent) }

  val toolBar = JToolBar()
  val bg = ButtonGroup()
  listOf(tgb12, tgb24, tgb32).forEach {
    it.isFocusPainted = false
    bg.add(it)
    toolBar.add(it)
  }
  return toolBar
}

fun makeUI(): Component {
  val button = JButton("Dialog")
  button.addActionListener {
    val c = button.rootPane
    UIManager.getLookAndFeel().provideErrorFeedback(c)
    JOptionPane.showMessageDialog(c, "Error message", "title", JOptionPane.ERROR_MESSAGE)
  }
  val panel = JPanel(GridBagLayout())
  val c = GridBagConstraints()
  c.weightx = 0.0
  c.insets = Insets(5, 5, 5, 0)
  panel.add(JLabel("Test:"), c)
  c.weightx = 1.0
  c.fill = GridBagConstraints.HORIZONTAL
  panel.add(JComboBox(arrayOf("Test")), c)
  c.weightx = 0.0
  c.insets = Insets(5, 5, 5, 5)
  c.anchor = GridBagConstraints.LINE_END
  panel.add(button, c)

  updateFont(FONT12, panel)

  val p = JPanel(BorderLayout())
  p.add(panel)
  p.add(makeToolBar(panel), BorderLayout.NORTH)
  p.preferredSize = Dimension(320, 240)
  return p
}

private fun updateFont(font: Font, parent: JComponent) {
  val fontResource = FontUIResource(font)
  UIManager.getLookAndFeelDefaults()
    .forEach { key, _ ->
      if (key.toString().lowercase(Locale.ENGLISH).endsWith("font")) {
        UIManager.put(key, fontResource)
      }
    }
  recursiveUpdateUI(parent)
  (parent.topLevelAncestor as? Window)?.pack()
}

private fun recursiveUpdateUI(p: Container) {
  p.components
    .filterIsInstance<JComponent>()
    .filterNot { it is JToolBar }
    .forEach {
      it.updateUI()
      if (it.componentCount > 0) {
        recursiveUpdateUI(it)
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
