package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.util.Random
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.Timer

fun makeUI(): Component {
  val c1 = JPanel(GridLayout(10, 10))
  val c2 = JPanel(GridLayout(10, 10))
  val random = Random()
  val timer = Timer(16, null)
  for (i in 0 until 100) {
    c1.add(Tile1(random))
    c2.add(Tile2(random, timer))
  }
  c2.addHierarchyListener { e ->
    if (e.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L) {
      if (e.component.isShowing) {
        timer.start()
      } else {
        timer.stop()
      }
    }
  }

  val tabs = JTabbedPane()
  tabs.addTab("Timer: 100", c1)
  tabs.addTab("Timer: 1, ActionListener: 100", c2)
  tabs.addTab("Timer: 1, ActionListener: 1", TilePanel(random))

  return JPanel(BorderLayout()).also {
    it.add(tabs)
    it.preferredSize = Dimension(320, 240)
  }
}

private class Tile1(rnd: Random) : JComponent(), HierarchyListener {
  private var red = 0
  private val timer: Timer

  init {
    addHierarchyListener(this)
    timer = Timer(16) {
      red = rnd.nextInt(255)
      repaint()
    }
  }

  override fun hierarchyChanged(e: HierarchyEvent) {
    if (e.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L) {
      if (e.component.isShowing) {
        timer.start()
      } else {
        timer.stop()
      }
    }
  }

  override fun getPreferredSize() = Dimension(10, 10)

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    if (isVisible) {
      if (!timer.isRunning) {
        timer.start()
      }
    } else {
      timer.stop()
    }
    g.color = Color(red, 255 - red, 0)
    g.fillRect(0, 0, width, height)
  }
}

private class Tile2(rnd: Random, timer: Timer) : JComponent() {
  private var red = 0

  init {
    timer.addActionListener {
      red = rnd.nextInt(255)
      repaint()
    }
  }

  override fun getPreferredSize() = Dimension(10, 10)

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    g.color = Color(red, 255 - red, 0)
    g.fillRect(0, 0, width, height)
  }
}

private class TilePanel(rnd: Random) : JPanel(GridLayout(10, 10)) {
  init {
    for (i in 0 until 100) {
      val l = JLabel()
      l.isOpaque = true
      add(l)
    }
    val timer = Timer(16) {
      for (i in 0 until 100) {
        val c = getComponent(i)
        val red = rnd.nextInt(256)
        c.background = Color(red, 255 - red, 0)
      }
    }
    addHierarchyListener { e ->
      if (e.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L) {
        if (e.component.isShowing) {
          timer.start()
        } else {
          timer.stop()
        }
      }
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
