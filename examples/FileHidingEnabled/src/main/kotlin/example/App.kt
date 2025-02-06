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
  descendants(chooser)
    .filterIsInstance<JComponent>()
    .mapNotNull { it.componentPopupMenu }
    .first()
    .also { pop ->
      pop.addSeparator()
      val item = JCheckBoxMenuItem("isFileHidingEnabled")
      item.addActionListener {
        val b = (it.source as? JCheckBoxMenuItem)?.isSelected == true
        chooser.isFileHidingEnabled = b
      }
      item.isSelected = chooser.isFileHidingEnabled
      pop.add(item)
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

fun descendants(parent: Container): List<Component> = parent.components
  .filterIsInstance<Container>()
  .flatMap { listOf(it) + descendants(it) }

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
