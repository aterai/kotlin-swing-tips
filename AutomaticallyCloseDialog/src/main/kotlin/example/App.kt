package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionListener
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.Timer

class MainPanel : JPanel(BorderLayout()) {
  init {
    val textArea = JTextArea()
    val label = JLabel()
    label.addHierarchyListener(AutomaticallyCloseListener())
    val button = JButton("show")
    button.addActionListener {
      val p = getRootPane()
      val title = "Automatically close dialog"
      val r = JOptionPane.showConfirmDialog(
        p, label, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE
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
    p.setBorder(BorderFactory.createTitledBorder("HierarchyListener"))
    p.add(button)
    add(p, BorderLayout.NORTH)
    add(JScrollPane(textArea))
    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    setPreferredSize(Dimension(320, 240))
  }
}

class AutomaticallyCloseListener : HierarchyListener {
  private val atomicDown = AtomicInteger(SECONDS)
  private val timer = Timer(1000, null)
  private var listener: ActionListener? = null

  override fun hierarchyChanged(e: HierarchyEvent) {
    if (e.getChangeFlags() and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L) {
      val l = e.getComponent() as? JLabel ?: return
      if (l.isShowing()) {
        atomicDown.set(SECONDS)
        l.setText("Closing in $SECONDS seconds")
        timer.removeActionListener(listener)
        listener = ActionListener {
          val i = atomicDown.decrementAndGet()
          l.setText("Closing in $i seconds")
          if (i <= 0 && timer.isRunning()) {
            timer.stop()
            (l.getTopLevelAncestor() as? Window)?.dispose()
          }
        }
        timer.addActionListener(listener)
        timer.start()
      } else {
        if (timer.isRunning()) {
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
      contentPane.add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
