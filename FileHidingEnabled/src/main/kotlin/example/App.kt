package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val log = JTextArea()
  val tk = Toolkit.getDefaultToolkit()
  val key = "awt.file.showHiddenFiles"
  val showHiddenFiles = tk.getDesktopProperty(key)
  log.text = "$key: $showHiddenFiles"

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
