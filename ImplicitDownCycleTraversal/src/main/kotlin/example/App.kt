package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val ftp = LayoutFocusTraversalPolicy()
  ftp.implicitDownCycleTraversal = false

  val p = JPanel(BorderLayout())
  p.isFocusCycleRoot = true
  p.focusTraversalPolicy = ftp

  val check = JCheckBox("ImplicitDownCycleTraversal")
  check.addActionListener { e ->
    ftp.implicitDownCycleTraversal = (e.source as? JCheckBox)?.isSelected == true
  }

  val sub = JPanel(BorderLayout())
  sub.border = BorderFactory.createTitledBorder("sub panel")

  val checkFocusCycleRoot = JCheckBox("sub.FocusCycleRoot", true)
  checkFocusCycleRoot.addActionListener { e ->
    sub.isFocusCycleRoot = (e.source as? JCheckBox)?.isSelected == true
  }
  sub.isFocusCycleRoot = true
  sub.add(JScrollPane(JTextArea("dummy")))
  sub.add(checkFocusCycleRoot, BorderLayout.SOUTH)

  val box = Box.createHorizontalBox()
  box.add(Box.createHorizontalGlue())
  box.add(JButton("JButton1"))
  box.add(JButton("JButton2"))

  p.add(check, BorderLayout.NORTH)
  p.add(sub)
  p.add(box, BorderLayout.SOUTH)
  p.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  p.preferredSize = Dimension(320, 240)
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
