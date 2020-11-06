package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  runCatching {
    Thread.sleep(5000)
  }
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(JTree()))
    it.preferredSize = Dimension(320, 240)
  }
}

fun main() {
  runCatching {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
  }.onFailure {
    it.printStackTrace()
    Toolkit.getDefaultToolkit().beep()
  }

  val splashScreen = JWindow()
  EventQueue.invokeLater {
    println("splashScreen show start / EDT: " + EventQueue.isDispatchThread())
    val cl = Thread.currentThread().contextClassLoader
    val img = ImageIcon(cl.getResource("example/splash.png"))
    splashScreen.contentPane.add(JLabel(img))
    splashScreen.pack()
    splashScreen.setLocationRelativeTo(null)
    splashScreen.isVisible = true
    println("splashScreen show end")
  }
  println("createGUI start / EDT: " + EventQueue.isDispatchThread())

  val frame = JFrame().apply {
    defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    contentPane.add(makeUI())
    pack()
    setLocationRelativeTo(null)
    // isVisible = true
  }
  println("createGUI end")

  EventQueue.invokeLater {
    println("  splashScreen dispose start / EDT: " + EventQueue.isDispatchThread())
    splashScreen.dispose()
    println("  splashScreen dispose end")
    println("  frame show start / EDT: " + EventQueue.isDispatchThread())
    frame.isVisible = true
    println("  frame show end")
  }
}
