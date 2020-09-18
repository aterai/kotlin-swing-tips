package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val p1 = JPanel()
  p1.border = BorderFactory.createTitledBorder("JFileChooser setResizable")
  p1.add(JButton(DefaultFileChooserAction()))
  p1.add(JButton(FixedSizeFileChooserAction()))

  val p2 = JPanel()
  p2.border = BorderFactory.createTitledBorder("JFileChooser setMinimumSize")
  p2.add(JButton(MinimumSizeFileChooserAction()))

  return JPanel(GridLayout(2, 1)).also {
    it.add(p1)
    it.add(p2)
    it.preferredSize = Dimension(320, 240)
  }
}

private class DefaultFileChooserAction : AbstractAction("Default") {
  override fun actionPerformed(e: ActionEvent) {
    val fileChooser = JFileChooser()
    val retValue = fileChooser.showOpenDialog((e.source as? JComponent)?.rootPane)
    println(retValue)
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
    println(retValue)
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
    println(retValue)
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
