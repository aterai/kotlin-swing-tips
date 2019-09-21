package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.util.Locale
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.FontUIResource

class MainPanel : JPanel(BorderLayout()) {
  private fun updateFont(font: Font) {
    val fontResource = FontUIResource(font)
    UIManager.getLookAndFeelDefaults()
      .forEach { key, _ ->
        if (key.toString().toLowerCase(Locale.ENGLISH).endsWith("font")) {
          UIManager.put(key, fontResource)
        }
      }
    recursiveUpdateUI(this)
    (getTopLevelAncestor() as? Window)?.pack()
  }

  private fun recursiveUpdateUI(p: Container) {
    p.getComponents()
      .filterIsInstance<JComponent>()
      .filterNot { it is JToolBar }
      .forEach {
        it.updateUI()
        if (it.getComponentCount() > 0) {
          recursiveUpdateUI(it)
        }
      }
  }

  init {
    val tgb12 = JToggleButton("12")
    tgb12.addActionListener { updateFont(FONT12) }
    val tgb24 = JToggleButton("24")
    tgb24.addActionListener { updateFont(FONT24) }
    val tgb32 = JToggleButton("32")
    tgb32.addActionListener { updateFont(FONT32) }
    val toolbar = JToolBar()
    val bg = ButtonGroup()
    listOf(tgb12, tgb24, tgb32).forEach {
      it.setFocusPainted(false)
      bg.add(it)
      toolbar.add(it)
    }
    val button = JButton("Dialog")
    button.addActionListener {
      Toolkit.getDefaultToolkit().beep()
      JOptionPane.showMessageDialog(
        rootPane,
        "MessageDialog",
        "Change All Font Size",
        JOptionPane.ERROR_MESSAGE
      )
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
    add(toolbar, BorderLayout.NORTH)
    add(panel)
    updateFont(FONT12)
    setPreferredSize(Dimension(320, 240))
  }

  companion object {
    private val FONT12 = Font(Font.SANS_SERIF, Font.PLAIN, 12)
    private val FONT24 = Font(Font.SANS_SERIF, Font.PLAIN, 24)
    private val FONT32 = Font(Font.SANS_SERIF, Font.PLAIN, 32)
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
