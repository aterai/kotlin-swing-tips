package example

import com.sun.java.swing.plaf.windows.WindowsFileChooserUI
import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.metal.MetalFileChooserUI

fun makeUI(): Component {
  val log = JTextArea()

  val button1 = JButton("Default")
  button1.addActionListener {
    val fileChooser = JFileChooser()
    val retValue = fileChooser.showOpenDialog(log.rootPane)
    if (retValue == JFileChooser.APPROVE_OPTION) {
      log.text = fileChooser.selectedFile.absolutePath
    }
  }

  val button2 = JButton("Alignment: Right")
  button2.addActionListener {
    val fileChooser = object : JFileChooser() {
      override fun updateUI() {
        super.updateUI()
        val tmp = if (ui is WindowsFileChooserUI) {
          RightAlignmentWindowsFileChooserUI(this)
        } else {
          RightAlignmentMetalFileChooserUI(this)
        }
        setUI(tmp)
      }
    }
    val retValue = fileChooser.showOpenDialog(log.rootPane)
    if (retValue == JFileChooser.APPROVE_OPTION) {
      log.text = fileChooser.selectedFile.absolutePath
    }
  }

  val p = JPanel()
  p.border = BorderFactory.createTitledBorder("JFileChooser")
  p.add(button1)
  p.add(button2)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(log))
    it.preferredSize = Dimension(320, 240)
  }
}

private class RightAlignmentMetalFileChooserUI(fc: JFileChooser) : MetalFileChooserUI(fc) {
  override fun installComponents(fc: JFileChooser) {
    super.installComponents(fc)
    descendants(bottomPanel)
      .filterIsInstance<JLabel>()
      .forEach {
        it.horizontalAlignment = SwingConstants.RIGHT
        it.border = BorderFactory.createEmptyBorder(0, 0, 0, 5)
      }
  }
}

private class RightAlignmentWindowsFileChooserUI(fc: JFileChooser?) : WindowsFileChooserUI(fc) {
  override fun installComponents(fc: JFileChooser) {
    super.installComponents(fc)
    descendants(bottomPanel)
      .filterIsInstance<JLabel>()
      .forEach { it.alignmentX = Component.RIGHT_ALIGNMENT }
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
