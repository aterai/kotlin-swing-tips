package example

import java.awt.*
import java.awt.event.InputEvent
import javax.swing.*
import javax.swing.plaf.LayerUI

fun makeUI(): Component {
  val tabs0 = makeTabbedPane()
  tabs0.isEnabled = false
  tabs0.border = BorderFactory.createTitledBorder("setEnabled(false)")

  val tabs2 = makeTabbedPane()
  tabs2.isEnabled = false
  for (i in 0 until tabs2.tabCount) {
    tabs2.setTabComponentAt(i, JLabel(tabs2.getTitleAt(i)))
    tabs2.border = BorderFactory.createTitledBorder("setTabComponentAt(...)")
  }

  val tabs3 = makeTabbedPane()
  tabs3.border = BorderFactory.createTitledBorder("DisableInputLayerUI()")
  val p = JPanel(GridLayout(0, 1, 0, 5))
  p.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

  listOf(tabs0, tabs2).forEach { p.add(it) }
  p.add(JLayer(tabs3, DisableInputLayerUI()))

  val button = JButton("next")
  button.addActionListener {
    val i = tabs0.selectedIndex + 1
    val next = if (i >= tabs0.tabCount) 0 else i
    listOf(tabs0, tabs2, tabs3).forEach { it.selectedIndex = next }
  }

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(button, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTabbedPane(): JTabbedPane {
  val tabs = object : JTabbedPane() {
    override fun getPreferredSize() = super.getPreferredSize()?.also {
      it.height = 70
    }
  }
  for (i in 0 until 4) {
    val title = "Step$i"
    tabs.addTab(title, JTextField(title))
  }
  tabs.isFocusable = false
  return tabs
}

private class DisableInputLayerUI : LayerUI<Component>() {
  override fun installUI(c: JComponent) {
    super.installUI(c)
    (c as? JLayer<*>)?.layerEventMask = AWTEvent.KEY_EVENT_MASK or AWTEvent.MOUSE_EVENT_MASK or
      AWTEvent.MOUSE_MOTION_EVENT_MASK or AWTEvent.MOUSE_WHEEL_EVENT_MASK or
      AWTEvent.FOCUS_EVENT_MASK or AWTEvent.COMPONENT_EVENT_MASK
  }

  override fun uninstallUI(c: JComponent) {
    (c as? JLayer<*>)?.layerEventMask = 0
    super.uninstallUI(c)
  }

  override fun eventDispatched(e: AWTEvent, l: JLayer<out Component>) {
    if (e is InputEvent && l.view == e.source) {
      e.consume()
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
