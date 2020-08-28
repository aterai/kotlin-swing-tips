package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.JTextComponent

fun makeUI(): Component {
  val field = JTextField("111111111")
  val fl = object : FocusAdapter() {
    override fun focusGained(e: FocusEvent) {
      (e.component as? JTextComponent)?.selectAll()
    }
  }
  field.addFocusListener(fl)
  val p = JPanel(GridLayout(2, 1))
  p.add(makeTitledPanel("focusGained: selectAll", field))
  p.add(makeTitledPanel("default", JTextField("22222222")))
  p.border = BorderFactory.createEmptyBorder(10, 5, 10, 5)
  p.preferredSize = Dimension(320, 240)
  return p
}

private fun makeTitledPanel(title: String, cmp: Component): Component {
  val p = JPanel(GridBagLayout())
  p.border = BorderFactory.createTitledBorder(title)
  val c = GridBagConstraints()
  c.weightx = 1.0
  c.fill = GridBagConstraints.HORIZONTAL
  c.insets = Insets(5, 5, 5, 5)
  p.add(cmp, c)
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
