package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val panel = JPanel(BorderLayout())
  val check = JCheckBox("0x22_FF_00_00")
  val button = JButton("Stop 5sec")
  button.addActionListener {
    val w = SwingUtilities.getWindowAncestor(panel.rootPane)
    val dialog = JDialog(w, Dialog.ModalityType.APPLICATION_MODAL)
    dialog.isUndecorated = true
    dialog.bounds = w.bounds
    dialog.cursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
    val color = if (check.isSelected) 0x22_FF_00_00 else 0x01_00_00_00
    dialog.background = Color(color, true)

    object : BackgroundTask() {
      override fun done() {
        if (panel.isDisplayable) {
          dialog.isVisible = false
        }
      }
    }.execute()
    dialog.isVisible = true
  }
  val p = JPanel()
  p.add(check)
  p.add(JTextField(10))
  p.add(button)

  panel.add(p, BorderLayout.NORTH)
  panel.add(JScrollPane(JTextArea(100, 80)))
  panel.preferredSize = Dimension(320, 240)
  return panel
}

open class BackgroundTask : SwingWorker<String, Unit?>() {
  @Throws(InterruptedException::class)
  override fun doInBackground(): String {
    Thread.sleep(5_000)
    return "Done"
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
