package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.InputEvent
import java.beans.PropertyChangeEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.LayerUI

class MainPanel : JPanel(BorderLayout()) {
  private val logger = JTextArea()
  private val cancel = JButton("cancel")
  private val button = JButton("Stop 5sec")
  private val layerUI = DisableInputLayerUI<JComponent>()
  @Transient
  private var worker: Thread? = null

  init {
    cancel.setEnabled(false)
    cancel.addActionListener { worker?.interrupt() }

    button.addActionListener {
      setInputBlock(true)
      val loop = Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop()
      worker = object : Thread() {
        override fun run() {
          var msg = "Done"
          try {
            Thread.sleep(5000)
          } catch (ex: InterruptedException) {
            msg = "Interrupted"
          }
          append(msg)
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
    add(JLayer(p, layerUI), BorderLayout.NORTH)
    add(JScrollPane(logger))
    add(cancel, BorderLayout.SOUTH)
    setPreferredSize(Dimension(320, 240))
  }

  fun setInputBlock(flg: Boolean) {
    layerUI.setInputBlock(flg)
    cancel.setEnabled(flg)
  }

  fun append(str: String) {
    logger.append(str + "\n")
  }
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
    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .5f))
    g2.setPaint(Color.GRAY.brighter())
    g2.fillRect(0, 0, c.getWidth(), c.getHeight())
    g2.dispose()
  }

  override fun installUI(c: JComponent) {
    super.installUI(c)
    val layer = c as? JLayer<*> ?: return
    layer.getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR))
    layer.setLayerEventMask(
        AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK
        or AWTEvent.MOUSE_WHEEL_EVENT_MASK or AWTEvent.KEY_EVENT_MASK
        or AWTEvent.FOCUS_EVENT_MASK or AWTEvent.COMPONENT_EVENT_MASK)
  }

  override fun uninstallUI(c: JComponent) {
    (c as? JLayer<*>)?.setLayerEventMask(0)
    super.uninstallUI(c)
  }

  override fun eventDispatched(e: AWTEvent, l: JLayer<out V>) {
    // if (running && e is InputEvent) {
    //   e.consume()
    // }
    (e as? InputEvent)?.takeIf { running }?.consume()
  }

  override fun applyPropertyChange(e: PropertyChangeEvent, l: JLayer<out V>) {
    if (e.getPropertyName() == CMD_REPAINT) {
      l.getGlassPane().setVisible(e.getNewValue() as Boolean)
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
