package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val buttonAreaBorder = "OptionPane.buttonAreaBorder"
  val borderCheck = JCheckBox(buttonAreaBorder)
  val op = JOptionPane("message", JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_CANCEL_OPTION)

  val sameSizeButtons = "OptionPane.sameSizeButtons"
  val p1 = JPanel()
  val button1 = JButton("default")
  button1.addActionListener {
    UIManager.getLookAndFeelDefaults()[sameSizeButtons] = false
    val d = UIDefaults()
    d[sameSizeButtons] = false
    op.putClientProperty("Nimbus.Overrides", d)
    op.putClientProperty("Nimbus.Overrides.InheritDefaults", true)
    SwingUtilities.updateComponentTreeUI(op)
    op.createDialog(p1.rootPane, "title").isVisible = true
  }
  val button2 = JButton("sameSizeButtons")
  button2.addActionListener {
    val d = UIDefaults()
    if (borderCheck.isSelected) {
      d[buttonAreaBorder] = BorderFactory.createLineBorder(Color.RED, 10)
    } else {
      d[buttonAreaBorder] = BorderFactory.createEmptyBorder()
    }
    d[sameSizeButtons] = true
    op.putClientProperty("Nimbus.Overrides", d)
    op.putClientProperty("Nimbus.Overrides.InheritDefaults", true)
    SwingUtilities.updateComponentTreeUI(op)
    op.createDialog(p1.rootPane, "title").isVisible = true
  }
  p1.add(button1)
  p1.add(button2)

  val p2 = JPanel()
  p2.add(borderCheck)
  return JPanel(BorderLayout()).also {
    it.add(p1, BorderLayout.NORTH)
    it.add(p2, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel")
      // UIManager.getLookAndFeelDefaults().put("OptionPane.sameSizeButtons", true)
    }.onFailure {
      it.printStackTrace()
      Toolkit.getDefaultToolkit().beep()
    }
    JFrame().apply {
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(makeUI())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
