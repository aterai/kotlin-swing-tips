package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val log = JTextArea()
  info(log, "FileChooser.newFolderActionLabelText")
  info(log, "FileChooser.newFolderToolTipText")
  info(log, "FileChooser.newFolderAccessibleName")
  info(log, "FileChooser.other.newFolder")
  info(log, "FileChooser.other.newFolder.subsequent")

  val newFolderKey = "FileChooser.win32.newFolder"
  val subsequentKey = "FileChooser.win32.newFolder.subsequent"
  info(log, newFolderKey)
  info(log, subsequentKey)
  UIManager.put(newFolderKey, "新しいフォルダー")
  UIManager.put(subsequentKey, "新しいフォルダー ({0})")
  info(log, newFolderKey)
  info(log, subsequentKey)

  val button = JButton("show JFileChooser")
  button.addActionListener {
    val fileChooser = JFileChooser()
    val retValue = fileChooser.showOpenDialog(log.rootPane)
    if (retValue == JFileChooser.APPROVE_OPTION) {
      log.append(fileChooser.selectedFile.absolutePath + "\n")
    }
  }

  val box = Box.createHorizontalBox().also {
    it.border = BorderFactory.createTitledBorder("JFileChooser")
    it.add(Box.createHorizontalGlue())
    it.add(button)
    it.add(Box.createHorizontalGlue())
  }

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.add(JScrollPane(log))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun info(
  log: JTextArea,
  key: String,
) {
  log.append("%s:%n  %s%n".format(key, UIManager.getString(key)))
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
