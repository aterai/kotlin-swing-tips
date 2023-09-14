package example

import java.awt.*
import javax.swing.*

private val log = JTextArea()

fun makeUI(): Component {
  val p = JPanel(GridLayout(2, 1)).also {
    it.add(makeDefaultChooserPanel())
    it.add(makeCustomChooserPanel())
  }

  return JPanel(BorderLayout(2, 2)).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(log))
    it.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeCustomChooserPanel(): JPanel {
  UIManager.put("FileChooser.cancelButtonText", "キャンセル")
  val p = JPanel()
  p.border = BorderFactory.createTitledBorder("custom")
  val showOpenDialog = JButton("Open:取消->キャンセル")
  showOpenDialog.addActionListener {
    val fileChooser = JFileChooser()
    appendSelectedFile(fileChooser, p)
  }
  val showSaveDialog = JButton("Save:取消->キャンセル")
  showSaveDialog.addActionListener {
    val fileChooser = JFileChooser()
    appendSelectedFile(fileChooser, p)
  }
  p.add(showOpenDialog)
  p.add(showSaveDialog)
  return p
}

private fun makeDefaultChooserPanel(): JPanel {
  val defaultChooser = JFileChooser()
  val p = JPanel()
  p.border = BorderFactory.createTitledBorder("default")
  val showOpenDialog = JButton("showOpenDialog")
  showOpenDialog.addActionListener {
    appendSelectedFile(defaultChooser, p)
  }
  val showSaveDialog = JButton("showSaveDialog")
  showSaveDialog.addActionListener {
    appendSelectedFile(defaultChooser, p)
  }
  p.add(showOpenDialog)
  p.add(showSaveDialog)
  return p
}

private fun appendSelectedFile(
  fileChooser: JFileChooser,
  p: Component,
) {
  val retValue = fileChooser.showOpenDialog(p)
  if (retValue == JFileChooser.APPROVE_OPTION) {
    log.append(fileChooser.selectedFile.toString() + "\n")
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
