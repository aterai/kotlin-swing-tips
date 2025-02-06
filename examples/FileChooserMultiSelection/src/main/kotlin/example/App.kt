package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val log = JTextArea()

  val button1 = JButton("Default")
  button1.addActionListener {
    val fileChooser = JFileChooser()
    val retValue = fileChooser.showOpenDialog(log.rootPane)
    if (retValue == JFileChooser.APPROVE_OPTION) {
      log.text = fileChooser.selectedFile.absolutePath
    }
  }

  val button2 = JButton("setMultiSelectionEnabled(true)")
  button2.addActionListener {
    val fileChooser = JFileChooser()
    // fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
    // fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    // fileChooser.fileSelectionMode = JFileChooser.FILES_AND_DIRECTORIES
    fileChooser.isMultiSelectionEnabled = true
    val retValue = fileChooser.showOpenDialog(log.rootPane)
    if (retValue == JFileChooser.APPROVE_OPTION) {
      log.text = ""
      for (file in fileChooser.selectedFiles) {
        log.append("${file.absolutePath}\n")
      }
    }
  }
  val p = JPanel()
  p.border = BorderFactory.createTitledBorder("JFileChooser")
  p.add(button1)
  p.add(button2)

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
