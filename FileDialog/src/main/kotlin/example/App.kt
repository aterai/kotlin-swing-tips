package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import javax.swing.* // ktlint-disable no-wildcard-imports

private val log = JTextArea()

fun makeUI(): Component {
  val button1 = JButton("FileDialog(Frame)")
  button1.addActionListener {
    // Window w = SwingUtilities.getWindowAncestor(button1)
    // Frame frame = new Frame(w.getGraphicsConfiguration())
    val frame = JOptionPane.getFrameForComponent(button1)
    // Frame frame = null
    val fd = FileDialog(frame, "title")
    // fd.setLocation(500, 500)
    fd.title = "FileDialog(Frame frame, String title)"
    fd.directory = System.getProperty("user.home")

    val wl = object : WindowAdapter() {
      override fun windowOpened(e: WindowEvent) {
        append("windowOpened")
        val w = e.window
        append("FileDialog: " + fd.location)
        append("Window: " + w.location)
        fd.title = "windowOpened"
        // fd.setLocation(500, 500)
        val d = SwingUtilities.getRoot(fd) as? Dialog ?: return
        append("fd == SwingUtilities.getRoot(fd): " + (d == fd))
        append("fd == w: " + (w == fd))
      }
    }
    fd.addWindowListener(wl)
    fd.isVisible = true
    if (fd.file != null) {
      // append(fd.getDirectory() + fd.getFile())
      val file = File(fd.directory, fd.file)
      append(file.absolutePath)
    }
  }

  val button2 = JButton("FileDialog(Dialog)")
  button2.addActionListener {
    val dialog = Dialog(SwingUtilities.getWindowAncestor(button2))
    val fd = FileDialog(dialog, "FileDialog(Dialog dialog, String title)")
    // fd.setDirectory(System.getProperty("user.home"))
    fd.isVisible = true
    if (fd.file != null) {
      val file = File(fd.directory, fd.file)
      append(file.absolutePath)
    }
  }

  val p = JPanel().also {
    it.border = BorderFactory.createTitledBorder("FileDialog")
    it.add(button1)
    it.add(button2)
  }
  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(log))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun append(str: String) {
  log.append(str + "\n")
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
