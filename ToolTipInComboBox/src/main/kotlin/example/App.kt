package example

import java.awt.*
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.plaf.LayerUI

fun makeUI(): Component {
  val model = DefaultComboBoxModel(arrayOf("aaa", "bb", "c"))
  val combo = makeComboBox(model)
  combo.isEditable = true
  val p = JPanel(GridLayout(0, 1, 5, 5))
  p.add(JLabel("setEditable(true)"))
  p.add(JLayer(combo, ToolTipLayerUI<JComboBox<*>>()))
  p.add(Box.createVerticalStrut(10))
  p.add(JLabel("setEditable(false)"))
  p.add(JLayer(makeComboBox(model), ToolTipLayerUI<JComboBox<*>>()))
  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.border = BorderFactory.createEmptyBorder(5, 20, 5, 20)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun <E> makeComboBox(model: ComboBoxModel<E>) = object : JComboBox<E>(model) {
  override fun updateUI() {
    setRenderer(null)
    super.updateUI()
    val renderer = getRenderer()
    setRenderer { list, value, index, isSelected, cellHasFocus ->
      renderer.getListCellRendererComponent(
        list,
        value,
        index,
        isSelected,
        cellHasFocus,
      ).also {
        (it as? JComponent)?.toolTipText = "Item$index: $value"
      }
    }
  }
}

private class ToolTipLayerUI<V : JComboBox<*>> : LayerUI<V>() {
  override fun installUI(c: JComponent) {
    super.installUI(c)
    (c as? JLayer<*>)?.layerEventMask = AWTEvent.MOUSE_MOTION_EVENT_MASK
  }

  override fun uninstallUI(c: JComponent) {
    (c as? JLayer<*>)?.layerEventMask = 0
    super.uninstallUI(c)
  }

  override fun processMouseMotionEvent(
    e: MouseEvent,
    l: JLayer<out V>,
  ) {
    l.view.toolTipText = if (e.component is JButton) {
      "ArrowButton"
    } else {
      "JComboBox:  ${l.view.selectedItem}"
    }
    super.processMouseMotionEvent(e, l)
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
