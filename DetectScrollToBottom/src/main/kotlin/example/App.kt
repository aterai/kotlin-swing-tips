package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(GridLayout(1, 2)) {
  init {
    UIManager.put("ScrollBar.minimumThumbSize", Dimension(32, 32))

    val textArea = JTextArea()
    val sb = StringBuilder()
    for (i in 0..2000) {
      sb.append(String.format("%04d%n", i))
    }
    textArea.setText(sb.toString())
    textArea.setCaretPosition(0)

    add(makePanel(textArea))
    add(makePanel(JTable(100, 3)))
    setPreferredSize(Dimension(320, 240))
  }

  private fun makePanel(c: Component): Component {
    val check = JCheckBox("JCheckBox")
    check.setEnabled(false)
    val p = JPanel(BorderLayout())
    val scroll = JScrollPane(c)
    scroll.getVerticalScrollBar().getModel().addChangeListener { e ->
      (e.getSource() as? BoundedRangeModel)?.also {
        val extent = it.getExtent()
        val maximum = it.getMaximum()
        val value = it.getValue()
        if (value + extent >= maximum) {
          check.setEnabled(true)
        }
      }
    }
    p.add(scroll)
    p.add(check, BorderLayout.SOUTH)
    return p
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
