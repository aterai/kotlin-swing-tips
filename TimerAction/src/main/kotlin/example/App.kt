package example

import java.awt.*
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import javax.swing.*
import javax.swing.Timer

fun makeUI(): Component {
  val c1 = JPanel(GridLayout(10, 10))
  val c2 = JPanel(GridLayout(10, 10))
  val intRange = 0..255
  val timer = Timer(16, null)
  for (i in 0..<100) {
    c1.add(Tile1(intRange))
    c2.add(Tile2(intRange, timer))
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
  tabs.addTab("Timer: 1, ActionListener: 1", TilePanel(intRange))

  return JPanel(BorderLayout()).also {
    it.add(tabs)
    it.preferredSize = Dimension(320, 240)
  }
}

private class Tile1(rng: IntRange) : JComponent(), HierarchyListener {
  private var red = 0
  private val timer: Timer

  init {
    addHierarchyListener(this)
    timer = Timer(16) {
      red = rng.random()
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

private class Tile2(val rng: IntRange, timer: Timer) : JComponent() {
  private var red = 0

  init {
    timer.addActionListener {
      red = rng.random()
      repaint()
    }
  }

  override fun getPreferredSize() = Dimension(10, 10)

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    g.color = Color(red, rng.last - red, 0)
    g.fillRect(0, 0, width, height)
  }
}

private class TilePanel(rng: IntRange) : JPanel(GridLayout(10, 10)) {
  init {
    for (i in 0..<100) {
      val l = JLabel()
      l.isOpaque = true
      add(l)
    }
    val timer = Timer(16) {
      for (i in 0..<100) {
        val c = getComponent(i)
        val red = rng.random()
        c.background = Color(red, rng.last - red, 0)
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
