package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val showHiddenFiles = Toolkit.getDefaultToolkit().getDesktopProperty("awt.file.showHiddenFiles")
  println("awt.file.showHiddenFiles: $showHiddenFiles")

  val chooser = JFileChooser()
  searchPopupMenu(chooser)?.also {
    it.addSeparator()
    val mi = JCheckBoxMenuItem("isFileHidingEnabled")
    mi.addActionListener { e ->
      chooser.isFileHidingEnabled = (e.source as? JCheckBoxMenuItem)?.isSelected == true
    }
    mi.isSelected = chooser.isFileHidingEnabled
    it.add(mi)
  }

  val log = JTextArea()
  val button = JButton("showOpenDialog")
  button.addActionListener {
    val retValue = chooser.showOpenDialog(log.rootPane)
    if (retValue == JFileChooser.APPROVE_OPTION) {
      log.text = chooser.selectedFile.absolutePath
    }
  }

  val p = JPanel()
  p.border = BorderFactory.createTitledBorder("JFileChooser")
  p.add(button)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(log))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun searchPopupMenu(parent: Container): JPopupMenu? {
  for (c in parent.components) {
    val pop = when {
      c is JComponent && c.componentPopupMenu != null -> c.componentPopupMenu
      c is Container -> searchPopupMenu(c)
      else -> null
    }
    if (pop != null) {
      return pop
    }
  }
  return null
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
