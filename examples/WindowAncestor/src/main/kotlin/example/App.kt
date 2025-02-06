package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val button = JButton("show frame title")
  button.addActionListener { e ->
    (SwingUtilities.getWindowAncestor(e.source as? Component) as? Frame)?.also {
      val msg = "parentFrame.getTitle(): " + it.title
      JOptionPane.showMessageDialog(it, msg, "title", JOptionPane.INFORMATION_MESSAGE)
    }
  }
  return JPanel(BorderLayout()).also {
    it.add(button)
    it.preferredSize = Dimension(320, 100)
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
    val frame1 = JFrame("frame1")
    frame1.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    frame1.contentPane.add(makeUI())
    frame1.pack()
    frame1.setLocationRelativeTo(null)

    val frame2 = JFrame("frame2")
    frame2.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    frame2.contentPane.add(makeUI())
    frame2.pack()

    val pt = frame1.location
    frame2.setLocation(pt.x, pt.y + frame1.size.height)
    frame1.isVisible = true
    frame2.isVisible = true
  }
}
