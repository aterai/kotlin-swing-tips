package example

import java.awt.*
import java.io.File
import javax.swing.*

fun makeUI(): Component {
  val p = JPanel(GridLayout(3, 1, 10, 10))
  val log = JTextArea()

  val deviceName = "con.txt"
  val b1 = JButton("c:/$deviceName")
  b1.addActionListener {
    val file = File(deviceName)
    runCatching {
      if (file.createNewFile()) {
        log.append("the named file does not exist and was successfully created.\n")
      } else {
        log.append("the named file already exists.\n")
      }
    }.onFailure {
      val obj = arrayOf(it.message)
      JOptionPane.showMessageDialog(p.rootPane, obj, "Error1", JOptionPane.INFORMATION_MESSAGE)
    }
  }
  val p1 = makeTitledPanel("IOException: before 1.5", b1)

  val b2 = JButton("c:/$deviceName:getCanonicalPath")
  b2.addActionListener {
    val file = File(deviceName)
    if (!isCanonicalPath(file)) {
      val obj = arrayOf(file.absolutePath + " is not a canonical path.")
      JOptionPane.showMessageDialog(p.rootPane, obj, "Error2", JOptionPane.INFORMATION_MESSAGE)
    }
  }
  val p2 = makeTitledPanel("getCanonicalPath: before 1.5", b2)

  val b3 = JButton("c:/$deviceName:isFile")
  b3.addActionListener {
    val file = File(deviceName)
    if (!file.isFile) {
      val obj = arrayOf(file.absolutePath + " is not a file.")
      JOptionPane.showMessageDialog(p.rootPane, obj, "Error3", JOptionPane.INFORMATION_MESSAGE)
    }
  }
  val p3 = makeTitledPanel("isFile: JDK 1.5+", b3)

  p.add(p1)
  p.add(p2)
  p.add(p3)
  return JPanel(BorderLayout(10, 10)).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(log))
    it.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
    it.preferredSize = Dimension(320, 240)
  }
}

// Before 1.5: file.canonicalPath == null
fun isCanonicalPath(file: File?) = runCatching {
  if (file == null || file.canonicalPath == null || !file.isFile) {
    return false
  }
}.isSuccess

private fun makeTitledPanel(title: String, c: Component) = JPanel(BorderLayout()).also {
  it.border = BorderFactory.createTitledBorder(title)
  it.add(c)
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
