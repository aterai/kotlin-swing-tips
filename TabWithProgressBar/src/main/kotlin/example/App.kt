package example

import java.awt.*
import java.awt.event.ActionEvent
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.*

fun makeUI(): Component {
  val tabbedPane = ProgressTabbedPane()

  val addAction = object : AbstractAction("Add") {
    private var count = 0

    override fun actionPerformed(e: ActionEvent) {
      val c = if (count % 2 == 0) JTree() else JLabel("Tab$count")
      tabbedPane.addTab("Title$count", c)
      tabbedPane.selectedIndex = tabbedPane.tabCount - 1
      count++
    }
  }

  val popup = JPopupMenu()
  popup.add(addAction)
  popup.addSeparator()
  popup.add("Close All").addActionListener { tabbedPane.removeAll() }

  tabbedPane.componentPopupMenu = popup
  tabbedPane.addTab("PopupMenu+addTab", JScrollPane(JTree()))

  return JPanel(BorderLayout()).also {
    it.add(tabbedPane)
    it.add(JButton(addAction), BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private class ProgressTabbedPane : JTabbedPane() {
  override fun addTab(title: String, content: Component) {
    super.addTab(title, JLabel("Loading..."))
    val bar = JProgressBar()
    val currentIndex = tabCount - 1
    val label = JLabel(title)
    val dim = label.preferredSize
    val w = 80.coerceAtLeast(dim.width)
    label.preferredSize = Dimension(w, dim.height)
    val tabInsets = UIManager.getInsets("TabbedPane.tabInsets")
    bar.preferredSize = Dimension(w, dim.height - tabInsets.top - 1)
    setTabComponentAt(currentIndex, bar)
    val worker = object : BackgroundTask() {
      override fun process(c: List<Int>) {
        if (!isDisplayable) {
          cancel(true)
        }
      }

      override fun done() {
        if (!isDisplayable) {
          cancel(true)
          return
        }
        label.toolTipText = runCatching {
          setTabComponentAt(currentIndex, label)
          setComponentAt(currentIndex, content)
          get()
        }.onFailure {
          if (it is InterruptedException) {
            Thread.currentThread().interrupt()
          }
        }.getOrNull() ?: "Exception"
      }
    }
    worker.addPropertyChangeListener(ProgressListener(bar))
    worker.execute()
  }
}

private open class BackgroundTask : SwingWorker<String, Int>() {
  @Throws(InterruptedException::class)
  override fun doInBackground(): String {
    val lengthOfTask = 120
    var total = 0
    var current = 0
    while (current < lengthOfTask) {
      total += doSomething()
      val v = 100 * current++ / lengthOfTask
      progress = v
      publish(v)
    }
    return "Done({$total}ms)"
  }

  @Throws(InterruptedException::class)
  protected fun doSomething(): Int {
    val iv = (1..20).random()
    Thread.sleep(iv.toLong())
    return iv
  }
}

private class ProgressListener(private val progressBar: JProgressBar) : PropertyChangeListener {
  init {
    progressBar.value = 0
  }

  override fun propertyChange(e: PropertyChangeEvent) {
    if ("progress" == e.propertyName) {
      progressBar.isIndeterminate = false
      progressBar.value = e.newValue as? Int ?: 0
    }
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
