package example

import java.awt.*
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.net.ServerSocket
import javax.swing.*

fun makeUI() = JPanel(BorderLayout()).also {
  it.add(JScrollPane(JTree()))
  it.preferredSize = Dimension(320, 240)
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    }.onFailure {
      it.printStackTrace()
      Toolkit.getDefaultToolkit().beep()
    }
    val loop = Toolkit.getDefaultToolkit().systemEventQueue.createSecondaryLoop()
    runCatching {
      ServerSocket(38_765).use {
        val frame = JFrame().apply {
          defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
          isResizable = false
          contentPane.add(makeUI())
          pack()
          setLocationRelativeTo(null)
          isVisible = true
        }
        frame.addWindowListener(object : WindowAdapter() {
          override fun windowClosing(e: WindowEvent) {
            loop.exit()
          }
        })
        loop.enter()
      }
    }.onFailure {
      JOptionPane.showMessageDialog(null, "An instance of the application is already running...")
    }
  }
}
