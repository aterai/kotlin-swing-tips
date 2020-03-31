package example

import com.sun.java.swing.plaf.windows.WindowsFileChooserUI
import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.metal.MetalFileChooserUI

val log = JTextArea()

fun makeUI(): Component {
  val button1 = JButton("Metal")
  button1.addActionListener {
    val fileChooser = object : JFileChooser() {
      override fun updateUI() {
        super.updateUI()
        setUI(EncodingFileChooserUI(this))
        resetChoosableFileFilters()
      }
    }
    val retValue = fileChooser.showSaveDialog(button1.rootPane)
    if (retValue == JFileChooser.APPROVE_OPTION) {
      (fileChooser.ui as? EncodingFileChooserUI)?.also {
        val enc = "\nEncoding: ${it.combo.selectedItem}"
        log.text = fileChooser.selectedFile.absolutePath + enc
      }
    }
  }

  val button2 = JButton("Alignment: Right")
  button2.addActionListener {
    val fileChooser = object : JFileChooser() {
      override fun updateUI() {
        super.updateUI()
        setUI(object : WindowsFileChooserUI(this) {
          override fun installComponents(fc: JFileChooser) {
            super.installComponents(fc)
            SwingUtils.descendants(bottomPanel)
              .filterIsInstance<JLabel>()
              .forEach {
                it.alignmentX = 1f
                it.horizontalAlignment = SwingConstants.RIGHT
              }
          }
        })
        resetChoosableFileFilters()
      }
    }
    val retValue = fileChooser.showSaveDialog(button2.rootPane)
    if (retValue == JFileChooser.APPROVE_OPTION) {
      log.text = fileChooser.selectedFile.absolutePath
    }
  }

  val button3 = JButton("Default")
  button3.addActionListener {
    val fileChooser = JFileChooser()
    val retValue = fileChooser.showSaveDialog(button3.rootPane)
    if (retValue == JFileChooser.APPROVE_OPTION) {
      log.text = fileChooser.selectedFile.absolutePath
    }
  }

  val p = JPanel().also {
    it.border = BorderFactory.createTitledBorder("JFileChooser")
    it.add(button1)
    it.add(button2)
    it.add(button3)
  }
  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(log))
    it.preferredSize = Dimension(320, 240)
  }
}

private class EncodingFileChooserUI(chooser: JFileChooser) : MetalFileChooserUI(chooser) {
  val combo = JComboBox(arrayOf("UTF-8", "UTF-16", "Shift_JIS", "EUC-JP"))

  override fun installComponents(fc: JFileChooser) {
    super.installComponents(fc)
    val bottomPanel = bottomPanel

    val label = object : JLabel("Encoding:") {
      override fun getPreferredSize() = SwingUtils.descendants(bottomPanel)
        .filterIsInstance<JLabel>()
        .firstOrNull()
        ?.preferredSize
        ?: super.getPreferredSize()
    }
    label.setDisplayedMnemonic('E')
    label.labelFor = combo

    val panel = JPanel()
    panel.layout = BoxLayout(panel, BoxLayout.LINE_AXIS)
    panel.add(label)
    panel.add(combo)

    // 0: fileNamePanel
    // 1: RigidArea
    // 2: filesOfTypePanel
    bottomPanel.add(Box.createRigidArea(Dimension(1, 5)), 3)
    bottomPanel.add(panel, 4)
    SwingUtils.descendants(bottomPanel)
      .filterIsInstance<JLabel>()
      .forEach {
        it.horizontalAlignment = SwingConstants.RIGHT
        it.border = BorderFactory.createEmptyBorder(0, 0, 0, 5)
      }
  }
}

object SwingUtils {
  fun descendants(parent: Container): List<Component> = parent.components
    .filterIsInstance<Container>()
    .flatMap { listOf(it) + descendants(it) }
  // .map { children(it) }.fold(listOf<Component>(parent)) { a, b -> a + b }
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
