package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

private fun makeCustomChooserPanel(): JPanel {
  UIManager.put("FileChooser.cancelButtonText", "キャンセル")
  val p = JPanel()
  p.border = BorderFactory.createTitledBorder("custom")
  val showOpenDialog = JButton("Open:取消->キャンセル")
  showOpenDialog.addActionListener {
    val fileChooser = JFileChooser()
    val retValue = fileChooser.showOpenDialog(p)
    println(retValue)
  }
  val showSaveDialog = JButton("Save:取消->キャンセル")
  showSaveDialog.addActionListener {
    val fileChooser = JFileChooser()
    val retValue = fileChooser.showSaveDialog(p)
    println(retValue)
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
    val retValue = defaultChooser.showOpenDialog(p)
    println(retValue)
  }
  val showSaveDialog = JButton("showSaveDialog")
  showSaveDialog.addActionListener {
    val retValue = defaultChooser.showSaveDialog(p)
    println(retValue)
  }
  p.add(showOpenDialog)
  p.add(showSaveDialog)
  return p
}

fun makeUI() = JPanel(GridLayout(2, 1)).also {
  it.add(makeDefaultChooserPanel())
  it.add(makeCustomChooserPanel())
  it.preferredSize = Dimension(320, 240)
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
