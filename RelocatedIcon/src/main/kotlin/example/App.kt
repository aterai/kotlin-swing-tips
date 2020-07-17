package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val check = JCheckBox("Icons should be relocated", true)
  val desktop = JDesktopPane()
  desktop.desktopManager = ReIconifyDesktopManager()
  desktop.addComponentListener(object : ComponentAdapter() {
    override fun componentResized(e: ComponentEvent) {
      if (!check.isSelected) {
        return
      }
      doReIconify(e.component as? JDesktopPane)
    }
  })
  val button = JButton("relocate")
  button.addActionListener { doReIconify(desktop) }

  val count = AtomicInteger()
  val addButton = JButton("add")
  addButton.addActionListener {
    val n = count.getAndIncrement()
    val f = createFrame("#$n", n * 10, n * 10)
    desktop.add(f)
    desktop.desktopManager.activateFrame(f)
  }

  val toolbar = JToolBar("toolbar")
  toolbar.isFloatable = false
  toolbar.add(addButton)
  toolbar.addSeparator()
  toolbar.add(button)
  toolbar.addSeparator()
  toolbar.add(check)
  addIconifiedFrame(desktop, createFrame("Frame", 30, 10))
  addIconifiedFrame(desktop, createFrame("Frame", 50, 30))

  return JPanel(BorderLayout()).also {
    it.add(desktop)
    it.add(toolbar, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

fun doReIconify(desktop: JDesktopPane?) {
  val dm = desktop?.desktopManager
  if (dm is ReIconifyDesktopManager) {
    for (f in desktop.allFrames) {
      if (f.isIcon) {
        dm.reIconifyFrame(f)
      }
    }
  }
}

fun createFrame(t: String?, x: Int, y: Int): JInternalFrame {
  val f = JInternalFrame(t, false, true, true, true)
  f.setSize(200, 100)
  f.setLocation(x, y)
  f.isVisible = true
  return f
}

fun addIconifiedFrame(desktop: JDesktopPane, f: JInternalFrame) {
  desktop.add(f)
  runCatching {
    f.isIcon = true
  }
}

private class ReIconifyDesktopManager : DefaultDesktopManager() {
  fun reIconifyFrame(f: JInternalFrame) {
    deiconifyFrame(f)
    val r = getBoundsForIconOf(f)
    iconifyFrame(f)
    f.desktopIcon.bounds = r
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
