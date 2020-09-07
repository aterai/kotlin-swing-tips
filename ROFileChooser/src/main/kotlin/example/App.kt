package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  // UIManager.put("FileChooser.readOnly", true)
  val log = JTextArea()

  val readOnlyButton = JButton("readOnly")
  readOnlyButton.addActionListener {
    UIManager.put("FileChooser.readOnly", true)
    val fileChooser = JFileChooser()
    val retValue = fileChooser.showOpenDialog(log.rootPane)
    if (retValue == JFileChooser.APPROVE_OPTION) {
      log.text = fileChooser.selectedFile.absolutePath
    }
  }

  val defaultButton = JButton("Default")
  defaultButton.addActionListener {
    UIManager.put("FileChooser.readOnly", false)
    val fileChooser = JFileChooser()
    val retValue = fileChooser.showOpenDialog(log.rootPane)
    if (retValue == JFileChooser.APPROVE_OPTION) {
      log.text = fileChooser.selectedFile.absolutePath
    }
  }

  val p = JPanel()
  p.border = BorderFactory.createTitledBorder("JFileChooser")
  p.add(readOnlyButton)
  p.add(defaultButton)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(log))
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
