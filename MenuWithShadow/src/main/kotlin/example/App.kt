package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI() = JPanel(BorderLayout()).also {
  EventQueue.invokeLater { it.rootPane.jMenuBar = createMenuBar() }
  val cl = Thread.currentThread().contextClassLoader
  it.add(JLabel(ImageIcon(cl.getResource("example/test.png"))))
  it.preferredSize = Dimension(320, 240)
  it.preferredSize = Dimension(320, 240)
}

private fun createMenuBar(): JMenuBar {
  val mb = JMenuBar()
  var menu = JMenu("File")
  mb.add(menu)
  menu.add("Open")
  menu.add("Save")
  menu.add("Close")
  menu.add("Exit")
  menu = JMenu("Edit")
  mb.add(menu)
  menu.add("Cut")
  menu.add("Copy")
  menu.add("Paste")
  val sub = JMenu("Edit").also {
    it.add("Cut")
    it.add("Copy")
    it.add("Paste")
  }
  menu.add(sub)
  return mb
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
      // https://youtrack.jetbrains.com/issue/KT-12993
      UIManager.put("PopupMenuUI", "example.CustomPopupMenuUI")
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
