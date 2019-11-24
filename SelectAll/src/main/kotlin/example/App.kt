package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.JTextComponent

class MainPanel : JPanel(GridLayout(2, 1)) {
  init {
    val field = JTextField("111111111")
    field.addFocusListener(object : FocusAdapter() {
      override fun focusGained(e: FocusEvent) {
        (e.getComponent() as? JTextComponent)?.selectAll()
      }
    })
    add(makeTitledPanel("focusGained: selectAll", field))
    add(makeTitledPanel("default", JTextField("22222222")))
    setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5))
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeTitledPanel(title: String, cmp: Component): Component {
    val p = JPanel(GridBagLayout())
    p.setBorder(BorderFactory.createTitledBorder(title))
    val c = GridBagConstraints()
    c.weightx = 1.0
    c.fill = GridBagConstraints.HORIZONTAL
    c.insets = Insets(5, 5, 5, 5)
    p.add(cmp, c)
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
