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
    fd.setTitle("FileDialog(Frame frame, String title)")
    fd.setDirectory(System.getProperty("user.home"))
    // frame.addWindowListener(new WindowAdapter() {
    fd.addWindowListener(object : WindowAdapter() {
      override fun windowOpened(e: WindowEvent) {
        append("windowOpened")
        val w = e.getWindow()
        append("FileDialog: " + fd.getLocation())
        append("Window: " + w.getLocation())
        fd.setTitle("windowOpened")
        // fd.setLocation(500, 500)
        val d = SwingUtilities.getRoot(fd) as? Dialog ?: return
        append("fd == SwingUtilities.getRoot(fd): " + (d == fd))
        append("fd == w: " + (w == fd))
      }
    })
    fd.setVisible(true)
    if (fd.getFile() != null) {
      // append(fd.getDirectory() + fd.getFile())
      val file = File(fd.getDirectory(), fd.getFile())
      append(file.getAbsolutePath())
    }
  }

  val button2 = JButton("FileDialog(Dialog)")
  button2.addActionListener {
    val dialog = Dialog(SwingUtilities.getWindowAncestor(button2))
    val fd = FileDialog(dialog, "FileDialog(Dialog dialog, String title)")
    // fd.setDirectory(System.getProperty("user.home"))
    fd.setVisible(true)
    if (fd.getFile() != null) {
      val file = File(fd.getDirectory(), fd.getFile())
      append(file.getAbsolutePath())
    }
  }

  val p = JPanel().also {
    it.setBorder(BorderFactory.createTitledBorder("FileDialog"))
    it.add(button1)
    it.add(button2)
  }
  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(log))
    it.setPreferredSize(Dimension(320, 240))
  }
}

fun append(str: String) {
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(makeUI())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
