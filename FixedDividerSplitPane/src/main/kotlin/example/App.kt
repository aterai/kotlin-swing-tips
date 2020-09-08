package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val sp = JSplitPane(JSplitPane.VERTICAL_SPLIT)
  sp.topComponent = JScrollPane(JTree())
  sp.bottomComponent = JScrollPane(JTextArea())
  sp.isOneTouchExpandable = true
  EventQueue.invokeLater { sp.setDividerLocation(.5) }

  val check1 = JCheckBox("setEnabled(...)", true)
  check1.addActionListener { e -> sp.isEnabled = (e.source as? JCheckBox)?.isSelected == true }

  val dividerSize = UIManager.getInt("SplitPane.dividerSize")
  val check2 = JCheckBox("setDividerSize(0)")
  check2.addActionListener { e ->
    sp.dividerSize = if ((e.source as? JCheckBox)?.isSelected == true) 0 else dividerSize
  }

  val p = JPanel(GridLayout(1, 0))
  p.border = BorderFactory.createTitledBorder("JSplitPane")
  p.add(check1)
  p.add(check2)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(sp)
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
