package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  object : SwingWorker<Void, Void>() {
    @Throws(InterruptedException::class)
    override fun doInBackground(): Void? {
      Thread.sleep(3_000)
      return null
    }
  }.execute()
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(JTree()))
    it.preferredSize = Dimension(320, 240)
  }
}

private open class BackgroundTask : SwingWorker<Void, Void>() {
  @Throws(InterruptedException::class)
  override fun doInBackground(): Void? {
    var current = 0
    val lengthOfTask = 120
    while (current < lengthOfTask && !isCancelled) {
      doSomething(100 * current++ / lengthOfTask)
    }
    return null
  }

  @Throws(InterruptedException::class)
  protected fun doSomething(progress: Int) {
    Thread.sleep(50)
    setProgress(progress)
  }
}

fun main() {
  println("main start / EDT: " + EventQueue.isDispatchThread())
  runCatching {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
  }.onFailure {
    it.printStackTrace()
    Toolkit.getDefaultToolkit().beep()
  }

  val frame = JFrame()
  val splashScreen = JDialog(frame, Dialog.ModalityType.DOCUMENT_MODAL)
  val progress = JProgressBar()
  println(splashScreen.modalityType)
  val cl = Thread.currentThread().contextClassLoader
  EventQueue.invokeLater {
    splashScreen.isUndecorated = true
    splashScreen.contentPane.add(JLabel(ImageIcon(cl.getResource("example/splash.png"))))
    splashScreen.contentPane.add(progress, BorderLayout.SOUTH)
    splashScreen.pack()
    splashScreen.setLocationRelativeTo(null)
    splashScreen.isVisible = true
  }
  val worker = object : BackgroundTask() {
    override fun done() {
      splashScreen.dispose()
    }
  }
  worker.addPropertyChangeListener { e ->
    if ("progress" == e.propertyName) {
      progress.value = (e.newValue as? Int ?: 0)
    }
  }
  worker.execute()

  frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
  frame.contentPane.add(makeUI())
  frame.pack()
  frame.setLocationRelativeTo(null)
  EventQueue.invokeLater { frame.isVisible = true }
  println("main end")
}
