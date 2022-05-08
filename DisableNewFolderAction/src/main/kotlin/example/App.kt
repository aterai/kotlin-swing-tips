package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.io.File
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.filechooser.FileSystemView

fun makeUI(): Component {
  val log = JTextArea()
  val fc1 = JFileChooser()
  val button1 = JButton("Default")
  button1.addActionListener {
    val retValue = fc1.showOpenDialog(log.rootPane)
    if (retValue == JFileChooser.APPROVE_OPTION) {
      log.append("%s%n".format(fc1.selectedFile))
    }
  }

  val fc2 = JFileChooser()
  fc2.fileSystemView = object : FileSystemView() {
    override fun createNewFolder(containingDir: File): File? {
      return null
    }
  }
  val cmd = "New Folder"
  fc2.actionMap[cmd]?.isEnabled = false
  fc2.addPropertyChangeListener(JFileChooser.DIRECTORY_CHANGED_PROPERTY) { e ->
    if (e.newValue is File) {
      fc2.actionMap[cmd]?.isEnabled = false
    }
  }
  val button2 = JButton("disable New Folder")
  button2.addActionListener {
    val retValue = fc2.showOpenDialog(log.rootPane)
    if (retValue == JFileChooser.APPROVE_OPTION) {
      log.append("%s%n".format(fc2.selectedFile))
    }
  }

  val p = JPanel(GridLayout(1, 0, 5, 5))
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
