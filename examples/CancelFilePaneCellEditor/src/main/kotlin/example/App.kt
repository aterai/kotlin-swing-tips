package example

import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.*

private val log = JTextArea()

fun makeUI(): Component {
  val button0 = JButton("Default")
  button0.addActionListener { openDefaultFileChooser() }
  val p0 = JPanel()
  val t0 = "JFileChooser resizing may result in incorrect cell editor positioning"
  p0.setBorder(BorderFactory.createTitledBorder(t0))
  p0.add(button0)
  val button1 = JButton("JFileChooser")
  button1.addActionListener { openListViewFileChooser1() }
  val button2 = JButton("Dialog")
  button2.addActionListener { openListViewFileChooser2() }
  val p1 = JPanel()
  val t1 = "override ComponentListener#componentResized to cancel editing"
  p1.setBorder(BorderFactory.createTitledBorder(t1))
  p1.add(button1)
  p1.add(button2)
  val p = JPanel(GridLayout(2, 1))
  p.add(p0)
  p.add(p1)
  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(log))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun openDefaultFileChooser() {
  val chooser = JFileChooser()
  val retValue = chooser.showOpenDialog(log.rootPane)
  if (retValue == JFileChooser.APPROVE_OPTION) {
    log.text = chooser.selectedFile.absolutePath
  }
}

private fun openListViewFileChooser1() {
  val chooser = JFileChooser()
  chooser.addComponentListener(CancelEditListener(chooser))
  val retValue = chooser.showOpenDialog(log.rootPane)
  if (retValue == JFileChooser.APPROVE_OPTION) {
    log.text = chooser.selectedFile.absolutePath
  }
}

private fun openListViewFileChooser2() {
  val chooser = object : JFileChooser() {
    override fun createDialog(parent: Component?): JDialog {
      val dialog = super.createDialog(parent)
      dialog.addComponentListener(CancelEditListener(this))
      return dialog
    }
  }
  val retValue = chooser.showOpenDialog(log.rootPane)
  if (retValue == JFileChooser.APPROVE_OPTION) {
    log.text = chooser.selectedFile.absolutePath
  }
}

private class CancelEditListener(
  private val chooser: JFileChooser,
) : ComponentAdapter() {
  override fun componentResized(e: ComponentEvent?) {
    // sun.swing.FilePane.cancelEdit()
    chooser.setSelectedFile(null)
  }
}

fun main() {
  EventQueue.invokeLater {
    JFrame().apply {
      runCatching {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
      }.onFailure {
        it.printStackTrace()
        Toolkit.getDefaultToolkit().beep()
      }
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
