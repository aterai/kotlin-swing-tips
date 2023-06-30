package example

import java.awt.*
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.util.prefs.Preferences
import javax.swing.*

fun makeUI(): Component {
  val handler = WindowPreferencesHandler()

  val clearButton = JButton("Preferences#clear() and JFrame#dispose()")
  clearButton.addActionListener { e ->
    runCatching {
      handler.prefs.clear()
      handler.prefs.flush()
    }.onFailure {
      Toolkit.getDefaultToolkit().beep()
    }
    SwingUtilities.getWindowAncestor(e.source as? Component)?.dispose()
  }

  val exitButton = JButton("exit")
  exitButton.addActionListener { e ->
    handler.saveLocation()
    SwingUtilities.getWindowAncestor(e.source as? Component)?.dispose()
  }

  val box = Box.createHorizontalBox().also {
    it.add(Box.createHorizontalGlue())
    it.add(clearButton)
    it.add(Box.createHorizontalStrut(2))
    it.add(exitButton)
  }

  EventQueue.invokeLater {
    (box.topLevelAncestor as? Window)?.also {
      it.addWindowListener(handler)
      it.addComponentListener(handler)
      handler.initWindowSizeAndLocation(it)
    }
  }

  return object : JPanel(BorderLayout()) {
    override fun getPreferredSize() = handler.dim
  }.also {
    it.add(JLabel("TEST"))
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.add(box, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private class WindowPreferencesHandler : WindowAdapter(), ComponentListener {
  val prefs: Preferences = Preferences.userNodeForPackage(javaClass)
  val dim = Dimension(320, 240)
  val pos = Point()

  fun initWindowSizeAndLocation(frame: Window) {
    val w = prefs.getInt(PREFIX + "dimw", dim.width)
    val h = prefs.getInt(PREFIX + "dimh", dim.height)
    dim.setSize(w, h)
    // setPreferredSize(dim)
    frame.pack()
    val screen = frame.graphicsConfiguration.bounds
    pos.setLocation(screen.centerX - dim.width / 2, screen.centerY - dim.height / 2)
    val x = prefs.getInt(PREFIX + "locx", pos.x)
    val y = prefs.getInt(PREFIX + "locy", pos.y)
    pos.setLocation(x, y)
    frame.setLocation(pos.x, pos.y)
  }

  fun saveLocation() {
    prefs.putInt(PREFIX + "locx", pos.x)
    prefs.putInt(PREFIX + "locy", pos.y)
    prefs.putInt(PREFIX + "dimw", dim.width)
    prefs.putInt(PREFIX + "dimh", dim.height)
    runCatching {
      prefs.flush()
    }.onFailure {
      Toolkit.getDefaultToolkit().beep()
    }
  }

  override fun componentHidden(e: ComponentEvent) {
    // not needed
  }

  override fun componentMoved(e: ComponentEvent) {
    val frame = e.component as? Frame
    if (frame?.extendedState == Frame.NORMAL) {
      val pt = frame.locationOnScreen
      if (pt.x < 0 || pt.y < 0) {
        return
      }
      pos.location = pt
    }
  }

  override fun componentResized(e: ComponentEvent) {
    val frame = e.component as? JFrame
    if (frame?.extendedState == Frame.NORMAL) {
      dim.size = frame.contentPane.size
    }
  }

  override fun componentShown(e: ComponentEvent) {
    // not needed
  }

  override fun windowClosing(e: WindowEvent) {
    saveLocation()
    e.window.dispose()
  }

  companion object {
    private const val PREFIX = "xxx_"
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
      // setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
