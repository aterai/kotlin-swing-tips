package example

import java.awt.*
import javax.swing.*

private val log = JTextArea()

fun createUI(): Component {
  val readOnlyButton = JButton("readOnly")
  readOnlyButton.addActionListener { showOpenDialog(true) }

  val defaultButton = JButton("Default")
  defaultButton.addActionListener { showOpenDialog(false) }

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

private fun showOpenDialog(readOnly: Boolean) {
  // The FileChooserUI reads "FileChooser.readOnly" when it is installed,
  // so the value must be set before the JFileChooser is created.
  UIManager.put("FileChooser.readOnly", readOnly)
  val fileChooser = JFileChooser()
  val retValue = fileChooser.showOpenDialog(log.rootPane)
  if (retValue == JFileChooser.APPROVE_OPTION) {
    log.text = fileChooser.selectedFile.absolutePath
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
