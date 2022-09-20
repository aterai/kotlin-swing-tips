package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

private const val KEY = "OptionPane.isYesLast"

fun makeUI(): Component {
  val log = object : JTextArea() {
    override fun updateUI() {
      UIManager.put(KEY, null)
      super.updateUI()
      val b = UIManager.getLookAndFeelDefaults().getBoolean(KEY)
      EventQueue.invokeLater { text = "$KEY: $b" }
    }
  }

  val defaultButton = JButton("$KEY: false(default)")
  defaultButton.addActionListener {
    UIManager.put(KEY, false)
    val str = JOptionPane.showInputDialog(log.rootPane, "$KEY: false")
    log.text = str
  }

  val yesLastButton = JButton("$KEY: true")
  yesLastButton.addActionListener {
    UIManager.put(KEY, true)
    val str = JOptionPane.showInputDialog(log.rootPane, "$KEY: true")
    log.text = str
  }

  val p = JPanel(GridLayout(0, 1, 5, 5))
  p.border = BorderFactory.createTitledBorder("JOptionPane")
  p.add(defaultButton)
  p.add(yesLastButton)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(log))
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
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
