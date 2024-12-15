package example

import java.awt.*
import javax.swing.*
import javax.swing.plaf.InsetsUIResource

private val padding = SpinnerNumberModel(0, 0, 50, 1)
private val margin = SpinnerNumberModel(15, 0, 50, 1)

fun makeUI() = JPanel(GridLayout(2, 1)).also {
  val p = JPanel()
  p.add(makeButton1())
  p.add(makeButton2())
  it.add(p)
  it.add(makeBox(), BorderLayout.NORTH)
  it.preferredSize = Dimension(320, 240)
}

private fun makeButton1(): JButton {
  val op = makeOptionPane()
  val title = "Default"
  val button = JButton(title)
  button.addActionListener {
    val dialog = op.createDialog(button.rootPane, title)
    dialog.isVisible = true
  }
  return button
}

private fun makeButton2(): JButton {
  val op = makeOptionPane()
  val button = JButton("separatorPadding")
  button.addActionListener {
    val d = UIDefaults()
    val p = padding.number.toInt()
    d["OptionPane.separatorPadding"] = p
    val m = margin.number.toInt()
    d["OptionPane.contentMargins"] = InsetsUIResource(m, m, m, m)
    op.putClientProperty("Nimbus.Overrides", d)
    op.putClientProperty("Nimbus.Overrides.InheritDefaults", true)
    SwingUtilities.updateComponentTreeUI(op)
    val t1 = "separatorPadding: $p"
    val t2 = "contentMargins: $m"
    val dialog = op.createDialog(button.rootPane, "$t1 / $t2")
    dialog.isVisible = true
  }
  return button
}

private fun makeBox(): Component {
  val c = GridBagConstraints()
  c.gridx = 0
  c.insets = Insets(2, 2, 2, 0)
  c.anchor = GridBagConstraints.LINE_END
  val p = JPanel(GridBagLayout())
  p.border = BorderFactory.createTitledBorder("OptionPane")
  p.add(JLabel("contentMargins:"), c)
  p.add(JLabel("separatorPadding:"), c)
  c.gridx = 1
  c.weightx = 1.0
  c.fill = GridBagConstraints.HORIZONTAL
  p.add(JSpinner(margin), c)
  p.add(JSpinner(padding), c)
  return p
}

private fun makeOptionPane(): JOptionPane {
  val label = JLabel("message1")
  label.border = BorderFactory.createLineBorder(Color.RED)
  val msg = arrayOf<Component>(
    label,
    JTextField("22"),
    JButton("333"),
  )
  return JOptionPane(
    msg,
    JOptionPane.QUESTION_MESSAGE,
    JOptionPane.YES_NO_CANCEL_OPTION,
  )
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel")
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
