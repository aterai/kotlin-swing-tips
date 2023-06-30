package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val log = JTextArea()
  val chooser = JFileChooser()

  val button1 = JButton("FILES_AND_DIRECTORIES")
  button1.addActionListener {
    chooser.fileSelectionMode = JFileChooser.FILES_AND_DIRECTORIES
    updateFileChooser(chooser)
    val retValue = chooser.showOpenDialog(log.rootPane)
    if (retValue == JFileChooser.APPROVE_OPTION) {
      log.append("${chooser.selectedFile}\n")
    }
  }

  val button2 = JButton("DIRECTORIES_ONLY")
  button2.addActionListener {
    chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    updateFileChooser(chooser)
    val retValue = chooser.showOpenDialog(log.rootPane)
    if (retValue == JFileChooser.APPROVE_OPTION) {
      log.append("${chooser.selectedFile}\n")
    }
  }

  val p = JPanel(GridLayout(0, 1, 5, 5))
  p.border = BorderFactory.createTitledBorder("JFileChooser#showOpenDialog(...)")
  p.add(button1)
  p.add(button2)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(log))
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun updateFileChooser(fileChooser: JFileChooser) {
  val f = fileChooser.fileSelectionMode != JFileChooser.DIRECTORIES_ONLY
  fileChooser.isAcceptAllFileFilterUsed = f
  val labelText = UIManager.getString("FileChooser.filesOfTypeLabelText", fileChooser.locale)
  descendants(fileChooser)
    .filterIsInstance<JLabel>()
    .forEach {
      if (labelText == it.text) {
        val combo = it.labelFor
        it.isEnabled = f
        if (combo is JComboBox<*>) {
          combo.isEnabled = f
          (combo.renderer as? JComponent)?.isOpaque = f
        }
      }
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
