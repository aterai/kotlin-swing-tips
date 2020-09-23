package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  // GroupLayout
  val p1 = JPanel()
  p1.border = BorderFactory.createTitledBorder("GroupLayout")
  val layout = GroupLayout(p1)
  p1.layout = layout
  layout.autoCreateGaps = true
  layout.autoCreateContainerGaps = true
  val label1 = JLabel("0123456789_0123456789abc:")
  val label2 = JLabel("GroupLayout:")
  val tf1 = JTextField()
  val tf2 = JTextField()
  val hgp = layout.createSequentialGroup()
  hgp.addGroup(layout.createParallelGroup().addComponent(label1).addComponent(label2))
  hgp.addGroup(layout.createParallelGroup().addComponent(tf1).addComponent(tf2))
  layout.setHorizontalGroup(hgp)
  val vgp = layout.createSequentialGroup()
  vgp.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(label1).addComponent(tf1))
  vgp.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(label2).addComponent(tf2))
  layout.setVerticalGroup(vgp)

  // GridBagLayout
  val p2 = JPanel(GridBagLayout())
  val inside = BorderFactory.createEmptyBorder(10, 5 + 2, 10, 10 + 2)
  val outside = BorderFactory.createTitledBorder("GridBagLayout")
  p2.border = BorderFactory.createCompoundBorder(outside, inside)
  val c = GridBagConstraints()
  c.insets = Insets(5, 5, 5, 0)
  c.anchor = GridBagConstraints.LINE_START
  c.gridx = 0
  val label3 = JLabel("0123456789_0123456789abc:")
  val label4 = JLabel("GridBagLayout:")
  p2.add(label3, c)
  p2.add(label4, c)
  c.fill = GridBagConstraints.HORIZONTAL
  c.weightx = 1.0
  c.gridx = 1
  val tf3 = JTextField()
  val tf4 = JTextField()
  p2.add(tf3, c)
  p2.add(tf4, c)

  return JPanel(GridLayout(2, 1)).also {
    it.add(p1)
    it.add(p2)
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
