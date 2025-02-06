package example

import java.awt.*
import java.awt.event.InputEvent
import java.beans.PropertyChangeEvent
import javax.swing.*
import javax.swing.plaf.LayerUI

fun makeUI(): Component {
  val layerUI = DisableInputLayerUI<Component>()
  val stopper = Timer(5000) { layerUI.stop() }
  val button = JButton("Stop 5sec")
  button.addActionListener {
    layerUI.start()
    if (!stopper.isRunning) {
      stopper.start()
    }
  }

  val p = JPanel()
  p.add(JCheckBox())
  p.add(JTextField(10))
  p.add(button)
  stopper.isRepeats = false

  return JPanel(BorderLayout()).also {
    it.add(JLayer(p, layerUI), BorderLayout.NORTH)
    it.add(JScrollPane(JTextArea("JTextArea")))
    it.preferredSize = Dimension(320, 240)
  }
}

private class DisableInputLayerUI<V : Component> : LayerUI<V>() {
  private var running = false

  fun start() {
    if (running) {
      return
    }
    running = true
    firePropertyChange(CMD_REPAINT, false, true)
  }

  fun stop() {
    running = false
    firePropertyChange(CMD_REPAINT, true, false)
  }

  override fun paint(
    g: Graphics,
    c: JComponent,
  ) {
    super.paint(g, c)
    if (!running) {
      return
    }
    val g2 = g.create() as? Graphics2D ?: return
    g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .5f)
    g2.paint = Color.GRAY
    g2.fillRect(0, 0, c.width, c.height)
    g2.dispose()
  }

  override fun installUI(c: JComponent) {
    super.installUI(c)
    (c as? JLayer<*>)?.also {
      it.glassPane.cursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
      it.layerEventMask = AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK or
        AWTEvent.MOUSE_WHEEL_EVENT_MASK or AWTEvent.KEY_EVENT_MASK or
        AWTEvent.FOCUS_EVENT_MASK or AWTEvent.COMPONENT_EVENT_MASK
    }
  }

  override fun uninstallUI(c: JComponent) {
    (c as? JLayer<*>)?.layerEventMask = 0
    super.uninstallUI(c)
  }

  override fun eventDispatched(
    e: AWTEvent,
    l: JLayer<out V>,
  ) {
    if (running && e is InputEvent) {
      e.consume()
    }
  }

  override fun applyPropertyChange(
    e: PropertyChangeEvent,
    l: JLayer<out V>,
  ) {
    if (CMD_REPAINT == e.propertyName) {
      l.glassPane.isVisible = e.newValue as? Boolean == true
    }
  }

  companion object {
    private const val CMD_REPAINT = "repaint"
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
