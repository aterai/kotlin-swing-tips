package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.io.File
import javax.swing.* // ktlint-disable no-wildcard-imports

private val log = JTextArea()
private val field = JTextField(24)
private val check1 = JCheckBox("Change !dir.exists() case")
private val check2 = JCheckBox("isParent reset?")
private val fc0 = JFileChooser()
private val fc1 = JFileChooser()
private val fc2 = object : JFileChooser() {
  override fun setCurrentDirectory(dir: File?) {
    var current = dir
    if (current != null && !isTraversable(current)) {
      current = current.parentFile
      while (current != null && !isTraversable(current)) {
        current = current.parentFile
      }
    }
    super.setCurrentDirectory(current)
  }
}

fun makeUI(): Component {
  val p = JPanel(GridBagLayout())
  p.border = BorderFactory.createTitledBorder("JFileChooser.DIRECTORIES_ONLY")
  fc0.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
  fc1.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
  fc2.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
  runCatching {
    field.text = File(".").canonicalPath
  }.onFailure {
    it.printStackTrace()
    UIManager.getLookAndFeel().provideErrorFeedback(field)
  }

  val button1 = JButton("setCurrentDirectory")
  button1.addActionListener {
    val f = File(field.text.trim())
    val fc = if (check1.isSelected) fc2 else fc0
    fc.currentDirectory = f
    val retValue = fc.showOpenDialog(p)
    if (retValue == JFileChooser.APPROVE_OPTION) {
      log.text = fc.selectedFile.absolutePath
    }
  }

  val button2 = JButton("setSelectedFile")
  button2.addActionListener {
    val file = File(field.text.trim())
    val fc = fc1
    val b = !fc.fileSystemView.isParent(fc.currentDirectory, file)
    log.append("isAbsolute: ${file.isAbsolute}, isParent: $b\n")
    fc.selectedFile = file
    val retValue = fc.showOpenDialog(p)
    if (retValue == JFileChooser.APPROVE_OPTION) {
      log.text = fc.selectedFile.absolutePath
    }
    if (check2.isSelected) {
      fc.selectedFile = file.parentFile // XXX: reset???
    }
  }

  val c = GridBagConstraints()
  c.fill = GridBagConstraints.HORIZONTAL
  c.insets = Insets(5, 0, 0, 0)
  c.gridwidth = 2
  p.add(field, c)
  c.gridwidth = 1
  c.gridy = 1
  p.add(button1, c)
  p.add(check1, c)
  c.gridy = 2
  p.add(button2, c)
  p.add(check2, c)

  val panel = object : JPanel(BorderLayout()) {
    override fun updateUI() {
      super.updateUI()
      EventQueue.invokeLater {
        SwingUtilities.updateComponentTreeUI(fc0)
        SwingUtilities.updateComponentTreeUI(fc1)
        SwingUtilities.updateComponentTreeUI(fc2)
      }
    }
  }
  panel.add(p, BorderLayout.NORTH)
  panel.add(JScrollPane(log))
  panel.preferredSize = Dimension(320, 240)
  return panel
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
