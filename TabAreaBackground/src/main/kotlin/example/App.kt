package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ItemEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val map = mutableMapOf<String, Color>()
  map["TabbedPane.darkShadow"] = Color.GRAY
  map["TabbedPane.light"] = Color.GRAY
  map["TabbedPane.tabAreaBackground"] = Color.GRAY
  map["TabbedPane.unselectedBackground"] = Color.GRAY
  map["TabbedPane.shadow"] = Color.GRAY
  map["TabbedPane.highlight"] = Color.GRAY
  map["TabbedPane.focus"] = Color.WHITE
  map["TabbedPane.contentAreaColor"] = Color.WHITE
  map["TabbedPane.selected"] = Color.WHITE
  map["TabbedPane.selectHighlight"] = Color.WHITE
  // Maybe "TabbedPane.borderHightlightColor" is a typo,
  // but this is defined in MetalTabbedPaneUI
  map["TabbedPane.borderHightlightColor"] = Color.WHITE
  map.forEach { (key, value) ->
    UIManager.put(key, value)
  }

  val c = GridBagConstraints()
  c.anchor = GridBagConstraints.LINE_START
  c.gridx = GridBagConstraints.REMAINDER

  val combo = makeComboBox(map)
  val opaque = JCheckBox("JTabbedPane#setOpaque", true)

  val p = JPanel(GridBagLayout())
  p.add(opaque, c)
  p.add(combo, c)

  val tabs = makeTabbedPane()
  EventQueue.invokeLater { updateTabColor(tabs, tabs.selectedIndex, -1) }

  opaque.addActionListener { e ->
    tabs.isOpaque = (e.source as? JCheckBox)?.isSelected == true
    tabs.repaint()
  }
  combo.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      map.forEach { (key, value) ->
        UIManager.put(key, value)
      }
      UIManager.put(e.item, Color.GREEN)
      tabs.updateUI()
    }
  }
  tabs.addTab("JTree", JScrollPane(JTree()))
  tabs.addTab("JTextArea", JScrollPane(JTextArea()))
  tabs.addTab("JButton", JButton("button"))
  tabs.addTab("JPanel", p)
  tabs.setMnemonicAt(0, KeyEvent.VK_T)
  tabs.setMnemonicAt(1, KeyEvent.VK_A)
  tabs.setMnemonicAt(2, KeyEvent.VK_B)
  tabs.setMnemonicAt(3, KeyEvent.VK_P)
  tabs.preferredSize = Dimension(320, 240)
  return tabs
}

private fun makeTabbedPane(): JTabbedPane {
  val tabs = JTabbedPane()
  tabs.isOpaque = true
  tabs.addChangeListener { updateTabColor(tabs, tabs.selectedIndex, -1) }
  tabs.addMouseMotionListener(object : MouseAdapter() {
    override fun mouseMoved(e: MouseEvent) {
      updateTabColor(tabs, tabs.selectedIndex, tabs.indexAtLocation(e.x, e.y))
    }
  })
  return tabs
}

private fun updateTabColor(t: JTabbedPane, si: Int, tgt: Int) {
  for (i in 0 until t.tabCount) {
    t.setForegroundAt(i, getTabTabForeground(i, si, tgt))
  }
}

private fun getTabTabForeground(i: Int, selected: Int, cursor: Int) = when (i) {
  selected -> Color.BLACK
  cursor -> Color.ORANGE
  else -> Color.WHITE
}

private fun makeComboBox(map: Map<String, Color>): JComboBox<String> {
  val model = DefaultComboBoxModel<String>()
  model.addElement("gray-white")
  map.forEach { (key, _) ->
    model.addElement(key)
  }
  return JComboBox(model)
}

fun main() {
  EventQueue.invokeLater {
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
