package example

import java.awt.*
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import javax.swing.*
import javax.swing.JSpinner.DefaultEditor

fun createUI() = JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT).also {
  it.addTab("JRadioButtonMenuItem", createRadioButtonMenuItemPanel())
  it.addTab("JRadioButton", createRadioButtonPanel())
  it.addTab("JComboBox", createComboBoxPanel())
  it.addTab("JSpinner", createSpinnerPanel())
  it.preferredSize = Dimension(320, 240)
}

private fun createRadioButtonMenuItemPanel(): JPanel {
  val tabs = createTabbedPane()
  val menu = JMenu("JMenu")
  val buttonGroup = ButtonGroup()
  val handler = ItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      val cmd = buttonGroup.selection.actionCommand
      tabs.tabPlacement = TabPlacement.valueOf(cmd).placement
    }
  }
  TabPlacement.entries.forEach { tp ->
    val item = JRadioButtonMenuItem(tp.name, tp == TabPlacement.TOP).also {
      it.addItemListener(handler)
      it.actionCommand = tp.name
    }
    menu.add(item)
    buttonGroup.add(item)
  }
  val mb = JMenuBar().also {
    it.add(menu)
  }
  return createPanel(tabs, mb)
}

private fun createRadioButtonPanel(): JPanel {
  val tabs = createTabbedPane()
  val buttonGroup = ButtonGroup()
  val handler = ItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      val cmd = buttonGroup.selection.actionCommand
      tabs.tabPlacement = TabPlacement.valueOf(cmd).placement
    }
  }
  val box = Box.createHorizontalBox()
  TabPlacement.entries.forEach { tp ->
    val radio = JRadioButton(tp.name, tp == TabPlacement.TOP).also {
      it.addItemListener(handler)
      it.actionCommand = tp.name
    }
    box.add(radio)
    buttonGroup.add(radio)
  }
  return createPanel(tabs, box)
}

private fun createComboBoxPanel(): JPanel {
  val tabs = createTabbedPane()
  val combo = JComboBox(TabPlacement.entries.toTypedArray())
  combo.addItemListener { e ->
    (e.item as? TabPlacement)?.takeIf { e.stateChange == ItemEvent.SELECTED }?.also {
      tabs.tabPlacement = it.placement
    }
  }
  return createPanel(tabs, combo)
}

private fun createSpinnerPanel(): JPanel {
  val tabs = createTabbedPane()
  val model = SpinnerListModel(TabPlacement.entries.toTypedArray())
  val spinner = createSpinner(model)
  spinner.addChangeListener {
    (model.value as? TabPlacement)?.also {
      tabs.tabPlacement = it.placement
    }
  }
  return createPanel(tabs, spinner)
}

private fun createPanel(
  tabs: JTabbedPane,
  c: JComponent?,
): JPanel {
  val p = JPanel(BorderLayout())
  if (c != null) {
    p.add(c, BorderLayout.NORTH)
  }
  p.add(tabs)
  return p
}

private fun createTabbedPane(): JTabbedPane {
  val tabs = JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT)
  tabs.addTab("JTree", JScrollPane(JTree()))
  tabs.addTab("JTable", JScrollPane(JTable(5, 5)))
  tabs.addTab("JSplitPane", JSplitPane())
  tabs.addTab("JLabel", JLabel("JLabel"))
  tabs.addTab("JButton", JButton("JButton"))
  return tabs
}

private fun createSpinner(model: SpinnerListModel): JSpinner {
  val spinner = object : JSpinner(model) {
    override fun getNextValue() = super.getPreviousValue()

    override fun getPreviousValue() = super.getNextValue()
  }
  (spinner.editor as? DefaultEditor)?.textField?.isEditable = false
  return spinner
}

private enum class TabPlacement(
  val placement: Int,
) {
  TOP(SwingConstants.TOP),
  LEFT(SwingConstants.LEFT),
  BOTTOM(SwingConstants.BOTTOM),
  RIGHT(SwingConstants.RIGHT),
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
