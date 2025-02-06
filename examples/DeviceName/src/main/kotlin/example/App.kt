package example

import java.awt.*
import java.io.File
import javax.swing.*

private val log = JTextArea()
private const val DEVICE_NAME = "con.txt"

fun makeUI(): Component {
  val p = JPanel(GridLayout(3, 1, 10, 10))
  p.add(makeTitledPanel("IOException: before 1.5", makeButton1()))
  p.add(makeTitledPanel("getCanonicalPath: before 1.5", makeButton2()))
  p.add(makeTitledPanel("isFile: JDK 1.5+", makeButton3()))
  return JPanel(BorderLayout(10, 10)).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(log))
    it.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeButton1(): JButton {
  val b = JButton("c:/$DEVICE_NAME")
  b.addActionListener {
    val file = File(DEVICE_NAME)
    runCatching {
      if (file.createNewFile()) {
        log.append("the named file does not exist and was successfully created.\n")
      } else {
        log.append("the named file already exists.\n")
      }
    }.onFailure {
      val obj = arrayOf(it.message)
      JOptionPane.showMessageDialog(
        b.rootPane,
        obj,
        "Error1",
        JOptionPane.INFORMATION_MESSAGE,
      )
    }
  }
  return b
}

private fun makeButton2(): JButton {
  val b = JButton("c:/$DEVICE_NAME:getCanonicalPath")
  b.addActionListener {
    val file = File(DEVICE_NAME)
    if (!isCanonicalPath(file)) {
      val obj = arrayOf(file.absolutePath + " is not a canonical path.")
      JOptionPane.showMessageDialog(
        b.rootPane,
        obj,
        "Error2",
        JOptionPane.INFORMATION_MESSAGE,
      )
    }
  }
  return b
}

private fun makeButton3(): JButton {
  val b = JButton("c:/$DEVICE_NAME:isFile")
  b.addActionListener {
    val file = File(DEVICE_NAME)
    if (!file.isFile) {
      val obj = arrayOf(file.absolutePath + " is not a file.")
      JOptionPane.showMessageDialog(
        b.rootPane,
        obj,
        "Error3",
        JOptionPane.INFORMATION_MESSAGE,
      )
    }
  }
  return b
}

// Before 1.5: file.canonicalPath == null
fun isCanonicalPath(file: File?) = runCatching {
  if (file == null || file.canonicalPath == null || !file.isFile) {
    return false
  }
}.isSuccess

private fun makeTitledPanel(
  title: String,
  c: Component,
): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
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
