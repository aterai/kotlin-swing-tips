package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val log = JTextArea()
  val key = "OptionPane.buttonOrientation"

  val defaultButton = JButton("Default")
  defaultButton.addActionListener {
    UIManager.put(key, UIManager.getLookAndFeelDefaults().getInt(key))
    val msg = "Default"
    log.text = JOptionPane.showInputDialog(log.rootPane, msg)
  }

  val rightButton = JButton("RIGHT")
  rightButton.addActionListener {
    UIManager.put(key, SwingConstants.RIGHT)
    val msg = "OptionPane.buttonOrientation: RIGHT"
    log.text = JOptionPane.showInputDialog(log.rootPane, msg)
  }

  val centerButton = JButton("CENTER")
  centerButton.addActionListener {
    UIManager.put(key, SwingConstants.CENTER)
    val msg = "OptionPane.buttonOrientation: CENTER"
    log.text = JOptionPane.showInputDialog(log.rootPane, msg)
  }

  val leftButton = JButton("LEFT")
  leftButton.addActionListener {
    UIManager.put(key, SwingConstants.LEFT)
    val msg = "OptionPane.buttonOrientation: LEFT"
    log.text = JOptionPane.showInputDialog(log.rootPane, msg)
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
