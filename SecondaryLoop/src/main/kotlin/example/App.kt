package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.InputEvent
import java.beans.PropertyChangeEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.LayerUI

private val logger = JTextArea()
private val cancel = JButton("cancel")
private val button = JButton("Stop 5sec")
private val layerUI = DisableInputLayerUI<JComponent>()
@Transient
private var worker: Thread? = null

fun makeUI(): Component {
  cancel.isEnabled = false
  cancel.addActionListener { worker?.interrupt() }

  button.addActionListener {
    setInputBlock(true)
    val loop = Toolkit.getDefaultToolkit().systemEventQueue.createSecondaryLoop()
    worker = object : Thread() {
      override fun run() {
        append(runCatching { sleep(5000) }.fold(onSuccess = { "Done" }, onFailure = { "Interrupted" }))
        setInputBlock(false)
        loop.exit()
      }
    }
    worker?.start()
    if (!loop.enter()) {
      append("Error")
    }
  }

  val p = JPanel().also {
    it.add(JCheckBox())
    it.add(JTextField(10))
    it.add(button)
  }

  return JPanel(BorderLayout()).also {
    it.add(JLayer(p, layerUI), BorderLayout.NORTH)
    it.add(JScrollPane(logger))
    it.add(cancel, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

fun setInputBlock(flg: Boolean) {
  layerUI.setInputBlock(flg)
  cancel.isEnabled = flg
}

fun append(str: String) {
  logger.append(str + "\n")
}

internal class DisableInputLayerUI<V : JComponent> : LayerUI<V>() {
  private var running = false

  fun setInputBlock(block: Boolean) {
    firePropertyChange(CMD_REPAINT, running, block)
    running = block
  }

  override fun paint(g: Graphics, c: JComponent) {
    super.paint(g, c)
    if (!running) {
      return
    }
    val g2 = g.create() as Graphics2D
    g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .5f)
    g2.paint = Color.GRAY.brighter()
    g2.fillRect(0, 0, c.width, c.height)
    g2.dispose()
  }

  override fun installUI(c: JComponent) {
    super.installUI(c)
    val layer = c as? JLayer<*> ?: return
    layer.glassPane.cursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
    layer.layerEventMask = (AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK
      or AWTEvent.MOUSE_WHEEL_EVENT_MASK or AWTEvent.KEY_EVENT_MASK
      or AWTEvent.FOCUS_EVENT_MASK or AWTEvent.COMPONENT_EVENT_MASK)
  }

  override fun uninstallUI(c: JComponent) {
    (c as? JLayer<*>)?.layerEventMask = 0
    super.uninstallUI(c)
  }

  override fun eventDispatched(e: AWTEvent, l: JLayer<out V>) {
    // if (running && e is InputEvent) {
    //   e.consume()
    // }
    (e as? InputEvent)?.takeIf { running }?.consume()
  }

  override fun applyPropertyChange(e: PropertyChangeEvent, l: JLayer<out V>) {
    if (e.propertyName == CMD_REPAINT) {
      l.glassPane.isVisible = e.newValue as? Boolean ?: false
      l.repaint()
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
