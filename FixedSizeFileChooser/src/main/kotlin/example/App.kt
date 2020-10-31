package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

private val log = JTextArea()

fun makeUI(): Component {
  val p1 = JPanel()
  p1.border = BorderFactory.createTitledBorder("JFileChooser setResizable")
  p1.add(JButton(DefaultFileChooserAction()))
  p1.add(JButton(FixedSizeFileChooserAction()))

  val p2 = JPanel()
  p2.border = BorderFactory.createTitledBorder("JFileChooser setMinimumSize")
  p2.add(JButton(MinimumSizeFileChooserAction()))

  val panel = JPanel(GridLayout(2, 1))
  panel.add(p1)
  panel.add(p2)

  return JPanel(BorderLayout(2, 2)).also {
    it.add(panel, BorderLayout.NORTH)
    it.add(JScrollPane(log))
    it.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
    it.preferredSize = Dimension(320, 240)
  }
}

private class DefaultFileChooserAction : AbstractAction("Default") {
  override fun actionPerformed(e: ActionEvent) {
    val fileChooser = JFileChooser()
    val retValue = fileChooser.showOpenDialog((e.source as? JComponent)?.rootPane)
    if (retValue == JFileChooser.APPROVE_OPTION) {
      log.append(fileChooser.selectedFile.toString() + "\n")
    }
  }
}

private class FixedSizeFileChooserAction : AbstractAction("Resizable(false)") {
  override fun actionPerformed(e: ActionEvent) {
    val fileChooser = object : JFileChooser() {
      override fun createDialog(parent: Component) = super.createDialog(parent).also {
        it.isResizable = false
      }
    }
    val retValue = fileChooser.showOpenDialog((e.source as? JComponent)?.rootPane)
    if (retValue == JFileChooser.APPROVE_OPTION) {
      log.append(fileChooser.selectedFile.toString() + "\n")
    }
  }
}

private class MinimumSizeFileChooserAction : AbstractAction("MinimumSize(640, 480)") {
  override fun actionPerformed(e: ActionEvent) {
    val fileChooser = object : JFileChooser() {
      override fun createDialog(parent: Component) = super.createDialog(parent).also {
        it.minimumSize = Dimension(640, 480)
      }
    }
    val retValue = fileChooser.showOpenDialog((e.source as? JComponent)?.rootPane)
    if (retValue == JFileChooser.APPROVE_OPTION) {
      log.append(fileChooser.selectedFile.toString() + "\n")
    }
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
