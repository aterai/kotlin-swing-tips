package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  init {
    val fileChooser = object : JFileChooser() {
      override fun approveSelection() {
        val f = getSelectedFile()
        if (f.exists() && getDialogType() == SAVE_DIALOG) {
          val m = "<html>%s already exists.<br>Do you want to replace it?".format(f.getAbsolutePath())
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
      val ret = fileChooser.showSaveDialog(rootPane)
      if (ret == JFileChooser.APPROVE_OPTION) {
        val file = fileChooser.getSelectedFile()
        println(file)
      }
    }
    val p = JPanel(GridBagLayout())
    p.setBorder(BorderFactory.createTitledBorder("JFileChooser#showSaveDialog(...)"))
    p.add(button)
    add(p)
    setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20))
    setPreferredSize(Dimension(320, 240))
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
