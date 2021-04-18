package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.InputEvent
import java.awt.event.ItemEvent
import java.awt.event.MouseAdapter
import java.beans.PropertyChangeEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.LayerUI

fun makeUI(): Component {
  val table = JTable(16, 4)
  table.autoResizeMode = JTable.AUTO_RESIZE_OFF
  val scroll = JScrollPane(table)
  val lock = JToggleButton("🔓")
  lock.border = BorderFactory.createEmptyBorder()
  lock.isContentAreaFilled = false
  lock.isFocusPainted = false
  lock.isFocusable = false

  val layerUI = DisableInputLayerUI<Component>()
  lock.addItemListener { e ->
    (e.itemSelectable as? AbstractButton)?.also {
      if (e.stateChange == ItemEvent.SELECTED) {
        it.text = "🔒"
        scrollLock(scroll, true)
        layerUI.setLocked(true)
      } else if (e.stateChange == ItemEvent.DESELECTED) {
        it.text = "🔓"
        scrollLock(scroll, false)
        layerUI.setLocked(false)
      }
    }
  }

  val verticalScrollBar = scroll.verticalScrollBar
  val verticalBox = JPanel(BorderLayout())
  verticalBox.isOpaque = false
  verticalBox.add(JLayer(verticalScrollBar, layerUI))
  verticalBox.add(lock, BorderLayout.SOUTH)
  val model = verticalScrollBar.model
  model.addChangeListener { e ->
    (e.source as? BoundedRangeModel)?.also {
      verticalBox.isVisible = it.maximum - it.minimum > it.extent
    }
  }
  verticalBox.isVisible = model.maximum - model.minimum > model.extent

  return JPanel(BorderLayout(0, 0)).also {
    it.add(scroll)
    it.add(verticalBox, BorderLayout.EAST)
    it.preferredSize = Dimension(320, 240)
  }
}

fun scrollLock(scroll: JScrollPane, lock: Boolean) {
  scroll.isWheelScrollingEnabled = !lock
  scroll.viewport.view.also {
    it.isEnabled = !lock
    it.isFocusable = !lock
  }
}

private class DisableInputLayerUI<V : Component> : LayerUI<V>() {
  private val mouseBlocker = object : MouseAdapter() { /* block mouse event */ }
  private var isBlocking = false

  fun setLocked(flag: Boolean) {
    firePropertyChange(CMD_REPAINT, isBlocking, flag)
    isBlocking = flag
  }

  override fun installUI(c: JComponent) {
    super.installUI(c)
    (c as? JLayer<*>)?.also {
      it.glassPane.addMouseListener(mouseBlocker)
      it.layerEventMask = (
        AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK
          or AWTEvent.MOUSE_WHEEL_EVENT_MASK or AWTEvent.KEY_EVENT_MASK
        )
    }
  }

  override fun uninstallUI(c: JComponent) {
    (c as? JLayer<*>)?.also {
      it.layerEventMask = 0
      it.glassPane.removeMouseListener(mouseBlocker)
    }
    super.uninstallUI(c)
  }

  override fun eventDispatched(e: AWTEvent, l: JLayer<out V>) {
    if (isBlocking && e is InputEvent) {
      e.consume()
    }
  }

  override fun applyPropertyChange(e: PropertyChangeEvent, l: JLayer<out V>) {
    if (CMD_REPAINT == e.propertyName) {
      l.glassPane.isVisible = e.newValue == true
    }
  }

  companion object {
    private const val CMD_REPAINT = "lock"
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
