package example

import java.awt.*
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import javax.swing.*
import javax.swing.JSpinner.DefaultEditor

fun makeUI(): Component {
  val tabs1 = makeTabbedPane()
  val menu = JMenu("JMenu")
  val bg1 = ButtonGroup()
  val handler1 = ItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      tabs1.tabPlacement = TabPlacement.valueOf(bg1.selection.actionCommand).placement
    }
  }
  TabPlacement.values().forEach { tp ->
    val item = JRadioButtonMenuItem(tp.name, tp == TabPlacement.TOP).also {
      it.addItemListener(handler1)
      it.actionCommand = tp.name
    }
    menu.add(item)
    bg1.add(item)
  }
  val mb = JMenuBar().also {
    it.add(menu)
  }

  val tabs2 = makeTabbedPane()
  val bg2 = ButtonGroup()
  val handler2 = ItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      tabs2.tabPlacement = TabPlacement.valueOf(bg2.selection.actionCommand).placement
    }
  }
  val box = Box.createHorizontalBox()
  TabPlacement.values().forEach { tp ->
    val radio = JRadioButton(tp.name, tp == TabPlacement.TOP).also {
      it.addItemListener(handler2)
      it.actionCommand = tp.name
    }
    box.add(radio)
    bg2.add(radio)
  }

  val tabs3 = makeTabbedPane()
  val combo = JComboBox(TabPlacement.values())
  combo.addItemListener { e ->
    (e.item as? TabPlacement)?.takeIf { e.stateChange == ItemEvent.SELECTED }?.also {
      tabs3.tabPlacement = it.placement
    }
  }

  val tabs4 = makeTabbedPane()
  val model4 = SpinnerListModel(TabPlacement.values())
  val spinner = makeSpinner(model4)
  spinner.addChangeListener {
    (model4.value as? TabPlacement)?.also {
      tabs4.tabPlacement = it.placement
    }
  }

  return JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT).also {
    it.addTab("JRadioButtonMenuItem", makePanel(tabs1, mb))
    it.addTab("JRadioButton", makePanel(tabs2, box))
    it.addTab("JComboBox", makePanel(tabs3, combo))
    it.addTab("JSpinner", makePanel(tabs4, spinner))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makePanel(tabs: JTabbedPane, c: JComponent?): JPanel {
  val p = JPanel(BorderLayout())
  if (c != null) {
    p.add(c, BorderLayout.NORTH)
  }
  p.add(tabs)
  return p
}

private fun makeTabbedPane(): JTabbedPane {
  val tabs = JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT)
  tabs.addTab("JTree", JScrollPane(JTree()))
  tabs.addTab("JTable", JScrollPane(JTable(5, 5)))
  tabs.addTab("JSplitPane", JSplitPane())
  tabs.addTab("JLabel", JLabel("JLabel"))
  tabs.addTab("JButton", JButton("JButton"))
  return tabs
}

private fun makeSpinner(model: SpinnerListModel): JSpinner {
  val spinner = object : JSpinner(model) {
    override fun getNextValue() = super.getPreviousValue()

    override fun getPreviousValue() = super.getNextValue()
  }
  (spinner.editor as DefaultEditor).textField.isEditable = false
  return spinner
}

private enum class TabPlacement(val placement: Int) {
  TOP(SwingConstants.TOP),
  LEFT(SwingConstants.LEFT),
  BOTTOM(SwingConstants.BOTTOM),
  RIGHT(SwingConstants.RIGHT)
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
