package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionListener
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.Timer

fun makeUI(): Component {
  val textArea = JTextArea()
  val label = JLabel()
  label.addHierarchyListener(AutomaticallyCloseListener())
  val button = JButton("show")
  button.addActionListener {
    val p = label.rootPane
    val title = "Automatically close dialog"
    val r = JOptionPane.showConfirmDialog(
      p,
      label,
      title,
      JOptionPane.OK_CANCEL_OPTION,
      JOptionPane.INFORMATION_MESSAGE
    )
    when (r) {
      JOptionPane.OK_OPTION -> textArea.append("OK\n")
      JOptionPane.CANCEL_OPTION -> textArea.append("Cancel\n")
      JOptionPane.CLOSED_OPTION -> textArea.append("Closed(automatically)\n")
      else -> textArea.append("----\n")
    }
    textArea.append("\n")
  }
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder("HierarchyListener")
  p.add(button)
  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(textArea))
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private class AutomaticallyCloseListener : HierarchyListener {
  private val atomicDown = AtomicInteger(SECONDS)
  private val timer = Timer(1000, null)
  private var listener: ActionListener? = null

  override fun hierarchyChanged(e: HierarchyEvent) {
    if (e.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L) {
      val l = e.component as? JLabel ?: return
      if (l.isShowing) {
        atomicDown.set(SECONDS)
        l.text = "Closing in $SECONDS seconds"
        timer.removeActionListener(listener)
        listener = ActionListener {
          val i = atomicDown.decrementAndGet()
          l.text = "Closing in $i seconds"
          if (i <= 0 && timer.isRunning) {
            timer.stop()
            (l.topLevelAncestor as? Window)?.dispose()
          }
        }
        timer.addActionListener(listener)
        timer.start()
      } else {
        if (timer.isRunning) {
          timer.stop()
        }
      }
    }
  }

  companion object {
    private const val SECONDS = 5
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
