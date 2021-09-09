package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.jnlp.ServiceManager
import javax.jnlp.SingleInstanceListener
import javax.jnlp.SingleInstanceService
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI() = JPanel(BorderLayout()).also {
  it.add(JScrollPane(JTree()))
  it.preferredSize = Dimension(320, 240)
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
      val frame = JFrame()
      val sis = ServiceManager.lookup("javax.jnlp.SingleInstanceService")
      (sis as? SingleInstanceService)?.addSingleInstanceListener(object : SingleInstanceListener {
        private var count = 0
        override fun newActivation(args: Array<String>) {
          EventQueue.invokeLater {
            JOptionPane.showMessageDialog(frame, "already running: $count")
            frame.title = "title:$count"
            count++
          }
        }
      })
      frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      frame.contentPane.add(makeUI())
      frame.pack()
      frame.setLocationRelativeTo(null)
      frame.isVisible = true
    }.onFailure {
      it.printStackTrace()
      Toolkit.getDefaultToolkit().beep()
    }
  }
}
