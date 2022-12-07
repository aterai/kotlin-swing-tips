package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val log = JTextArea()
  val key = "OptionPane.buttonOrientation"

  val defaultButton = JButton("Default")
  defaultButton.addActionListener {
    val iv = UIManager.getLookAndFeelDefaults().getInt(key)
    UIManager.put(key, iv)
    val str = JOptionPane.showInputDialog(log.rootPane, "Default")
    log.text = str
  }

  val rightButton = JButton("RIGHT")
  rightButton.addActionListener {
    UIManager.put(key, SwingConstants.RIGHT)
    val str = JOptionPane.showInputDialog(log.rootPane, "OptionPane.buttonOrientation: RIGHT")
    log.text = str
  }

  val centerButton = JButton("CENTER")
  centerButton.addActionListener {
    UIManager.put(key, SwingConstants.CENTER)
    val str = JOptionPane.showInputDialog(log.rootPane, "OptionPane.buttonOrientation: CENTER")
    log.text = str
  }

  val leftButton = JButton("LEFT")
  leftButton.addActionListener {
    UIManager.put(key, SwingConstants.LEFT)
    val str = JOptionPane.showInputDialog(log.rootPane, "OptionPane.buttonOrientation: LEFT")
    log.text = str
  }

  val p = JPanel().also {
    it.border = BorderFactory.createTitledBorder("JOptionPane")
    it.add(defaultButton)
    it.add(rightButton)
    it.add(centerButton)
    it.add(leftButton)
  }

  val panel = object : JPanel(BorderLayout()) {
    override fun updateUI() {
      UIManager.put("OptionPane.buttonOrientation", null)
      super.updateUI()
    }
  }
  panel.add(p, BorderLayout.NORTH)
  panel.add(JScrollPane(log))
  panel.preferredSize = Dimension(320, 240)
  return panel
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
