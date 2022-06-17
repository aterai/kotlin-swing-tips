package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.KeyEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val button = JButton("Stop 5sec")
  button.addActionListener { e ->
    button.rootPane.glassPane.isVisible = true
    val c = e.source as? Component
    c?.isEnabled = false
    object : BackgroundTask() {
      public override fun done() {
        if (!button.isDisplayable) {
          cancel(true)
        } else {
          button.rootPane.glassPane.isVisible = false
          c?.isEnabled = true
        }
      }
    }.execute()
  }

  EventQueue.invokeLater {
    val gp = LockingGlassPane()
    gp.isVisible = false
    button.rootPane.glassPane = gp
  }

  val box = Box.createHorizontalBox()
  box.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

  val b = JButton("Button&Mnemonic")
  b.mnemonic = KeyEvent.VK_B

  val t = JTextField("TextField&ToolTip")
  t.toolTipText = "ToolTip"

  box.add(b)
  box.add(Box.createHorizontalStrut(5))
  box.add(t)
  box.add(Box.createHorizontalStrut(5))

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.add(button, BorderLayout.SOUTH)
    it.add(JScrollPane(JTree()))
    it.preferredSize = Dimension(320, 240)
  }
}

private open class BackgroundTask : SwingWorker<String, Void>() {
  @Throws(InterruptedException::class)
  public override fun doInBackground(): String {
    Thread.sleep(5000)
    return "Done"
  }
}

private class LockingGlassPane : JPanel() {
  override fun updateUI() {
    super.updateUI()
    isOpaque = false
    cursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
  }

  override fun setVisible(isVisible: Boolean) {
    val oldVisible = isVisible()
    super.setVisible(isVisible)
    rootPane?.takeIf { isVisible() != oldVisible }?.layeredPane?.isVisible = !isVisible
  }

  override fun paintComponent(g: Graphics) {
    rootPane?.layeredPane?.print(g)
    super.paintComponent(g)
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
