package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val fileChooser = object : JFileChooser() {
    override fun approveSelection() {
      val f = getSelectedFile()
      if (f.exists() && getDialogType() == SAVE_DIALOG) {
        val path = f.getAbsolutePath()
        val m = "<html>$path already exists.<br>Do you want to replace it?"
        val rv = JOptionPane.showConfirmDialog(this, m, "Save As", JOptionPane.YES_NO_OPTION)
        if (rv != JOptionPane.YES_OPTION) {
          return
        }
      }
      super.approveSelection()
    }
  }
  val button = JButton("Override JFileChooser#approveSelection()")
  button.addActionListener {
    val ret = fileChooser.showSaveDialog(button.getRootPane())
    if (ret == JFileChooser.APPROVE_OPTION) {
      val file = fileChooser.getSelectedFile()
      println(file)
    }
  }
  val p = JPanel(GridBagLayout())
  p.setBorder(BorderFactory.createCompoundBorder(
    BorderFactory.createEmptyBorder(20, 20, 20, 20),
    BorderFactory.createTitledBorder("JFileChooser#showSaveDialog(...)")
  ))
  p.add(button)
  p.setPreferredSize(Dimension(320, 240))
  return p
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
