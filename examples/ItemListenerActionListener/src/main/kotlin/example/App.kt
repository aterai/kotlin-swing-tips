package example

import java.awt.*
import java.awt.event.ActionListener
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import java.time.DayOfWeek
import javax.swing.*

fun makeUI(): Component {
  val textArea = JTextArea()
  val p = JPanel(GridLayout(0, 1))
  p.add(makeComboBox(textArea))
  p.add(makeCheckBoxes(textArea))
  p.add(makeRadioButtons(textArea))

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(textArea))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeComboBox(textArea: JTextArea): Component {
  val combo = JComboBox(DayOfWeek.entries.toTypedArray())
  combo.addItemListener { e ->
    val c = e.itemSelectable
    val dow = (e.item as? DayOfWeek)?.toString()
    val b = e.stateChange == ItemEvent.SELECTED
    print(textArea, e, c.javaClass, b, dow)
  }
  combo.addActionListener { e ->
    val c = e.source
    val dow = combo.getItemAt(combo.selectedIndex)?.toString()
    val b = "comboBoxChanged" == e.actionCommand
    print(textArea, e, c.javaClass, b, dow)
  }

  val button0 = JButton("0")
  button0.addActionListener { combo.setSelectedIndex(0) }

  val button1 = JButton("clear(-1)")
  button1.addActionListener { combo.setSelectedIndex(-1) }

  val p = JPanel()
  listOf(Box.createVerticalStrut(40), combo, button0, button1).forEach { p.add(it) }
  return p
}

private fun makeCheckBoxes(textArea: JTextArea): Component {
  val il = ItemListener { e ->
    val c = e.itemSelectable
    val b = e.stateChange == ItemEvent.SELECTED
    val ac = (c as? AbstractButton)?.actionCommand
    print(textArea, e, c.javaClass, b, ac)
  }
  val al = ActionListener { e ->
    val c = e.source
    val b = (c as? AbstractButton)?.isSelected == true
    val ac = (c as? AbstractButton)?.actionCommand
    print(textArea, e, c.javaClass, b, ac)
  }

  val list = mutableListOf<AbstractButton>()
  val p = Box.createHorizontalBox()
  DayOfWeek.entries.map { JCheckBox(it.toString()) }.forEach {
    list.add(it)
    it.addItemListener(il)
    it.addActionListener(al)
    p.add(it)
  }

  val button0 = JButton("clear")
  button0.addActionListener {
    list.forEach { it.isSelected = false }
  }
  val button1 = JButton("all")
  button1.addActionListener {
    list.forEach { it.isSelected = true }
  }
  p.add(button0)
  p.add(button1)
  return JScrollPane(p)
}

private fun makeRadioButtons(textArea: JTextArea): Component {
  val bg = ButtonGroup()
  val il = ItemListener { e ->
    val c = e.itemSelectable
    val b = e.stateChange == ItemEvent.SELECTED
    val ac = bg.selection?.actionCommand
    print(textArea, e, c.javaClass, b, ac)
  }
  val al = ActionListener { e ->
    val c = e.source
    val b = (c as? AbstractButton)?.isSelected == true
    print(textArea, e, c.javaClass, b, e.actionCommand)
  }

  val p = Box.createHorizontalBox()
  DayOfWeek.entries.map { JRadioButton(it.toString()) }.forEach {
    it.addItemListener(il)
    it.addActionListener(al)
    p.add(it)
    bg.add(it)
  }

  val button0 = JButton("clear")
  button0.addActionListener { bg.clearSelection() }

  val button1 = JButton("MONDAY")
  button1.addActionListener {
    bg.elements
      .toList()
      .firstOrNull()
      ?.let { it.isSelected = !it.isSelected }
  }
  p.add(button0)
  p.add(button1)
  return JScrollPane(p)
}

private fun print(
  log: JTextArea,
  e: AWTEvent,
  clz: Class<*>,
  isSelected: Boolean,
  cmd: Any?,
) {
  val l = e.javaClass.simpleName
  val s = if (isSelected) "SELECTED" else "DESELECTED"
  log.append("%-14s %s %-10s %s%n".format(l, clz.simpleName, s, cmd ?: "NULL"))
  log.caretPosition = log.document.length
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
