package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val tree = JTree()
  val c = JCheckBox("setEnabled", true)
  c.addActionListener { e -> tree.isEnabled = (e.source as? JCheckBox)?.isSelected == true }
  val textArea = JTextArea("1234567890")
  val b = JButton("Clear")
  b.isFocusable = false
  b.addActionListener { textArea.text = "" }

  return JPanel(GridLayout(2, 1, 5, 5)).also {
    it.add(makePanel(JScrollPane(tree), c))
    it.add(makePanel(JScrollPane(textArea), b))
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makePanel(m: JComponent, c: Component): Component {
  val ir = 20 // inset.right
  val ch = c.preferredSize.height / 2
  val ib = BorderFactory.createEmptyBorder(0, 0, ch, 0)
  val eb = BorderFactory.createEtchedBorder()
  val bo = BorderFactory.createCompoundBorder(eb, ib)
  m.border = BorderFactory.createCompoundBorder(ib, bo)
  val layout = SpringLayout()
  val p = JLayeredPane()
  p.layout = layout
  val x = layout.getConstraint(SpringLayout.WIDTH, p)
  val y = layout.getConstraint(SpringLayout.HEIGHT, p)
  val g = Spring.minus(Spring.constant(ir))
  var constraints = layout.getConstraints(c)
  constraints.setConstraint(SpringLayout.EAST, Spring.sum(x, g))
  constraints.setConstraint(SpringLayout.SOUTH, y)
  p.setLayer(c, JLayeredPane.DEFAULT_LAYER + 1)
  p.add(c)
  constraints = layout.getConstraints(m)
  constraints.setConstraint(SpringLayout.WEST, Spring.constant(0))
  constraints.setConstraint(SpringLayout.NORTH, Spring.constant(0))
  constraints.setConstraint(SpringLayout.EAST, x)
  constraints.setConstraint(SpringLayout.SOUTH, y)
  p.setLayer(m, JLayeredPane.DEFAULT_LAYER)
  p.add(m)
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
