package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val fileChooser = object : JFileChooser() {
    override fun approveSelection() {
      val f = selectedFile
      if (f.exists() && dialogType == SAVE_DIALOG) {
        val path = f.absolutePath
        val m = "<html>$path already exists.<br>Do you want to replace it?"
        val rv = JOptionPane.showConfirmDialog(this, m, "Save As", JOptionPane.YES_NO_OPTION)
        if (rv != JOptionPane.YES_OPTION) {
          return
        }
      }
      super.approveSelection()
    }
  }

  val log = JTextArea()
  val button = JButton("Override JFileChooser#approveSelection()")
  button.addActionListener {
    val ret = fileChooser.showSaveDialog(log.rootPane)
    if (ret == JFileChooser.APPROVE_OPTION) {
      log.append("${fileChooser.selectedFile}\n")
    }
  }
  val p = JPanel(GridBagLayout())
  p.border = BorderFactory.createCompoundBorder(
    BorderFactory.createEmptyBorder(20, 20, 20, 20),
    BorderFactory.createTitledBorder("JFileChooser#showSaveDialog(...)")
  )
  p.add(button)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(log))
    it.preferredSize = Dimension(320, 240)
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
