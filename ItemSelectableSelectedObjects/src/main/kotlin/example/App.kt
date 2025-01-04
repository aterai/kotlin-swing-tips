package example

import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.EventQueue
import java.awt.Toolkit
import java.awt.event.ActionEvent
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.AbstractAction
import javax.swing.AbstractButton
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.ButtonGroup
import javax.swing.ComboBoxModel
import javax.swing.DefaultComboBoxModel
import javax.swing.DefaultListCellRenderer
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.KeyStroke
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.WindowConstants
import javax.swing.plaf.basic.ComboPopup

private val log = JTextArea()

fun makeUI(): Component {
  val listener = ItemListener { e ->
    val item = e.item
    val selectable = e.itemSelectable
    if (e.stateChange == ItemEvent.SELECTED) {
      log.append("Item: $item\n")
      log.append("ItemSelectable: ${selectable.javaClass.name}\n")
    }
    log.append("SelectedObjects:")
    selectable
      ?.selectedObjects
      ?.forEach {
        var str = it.toString()
        if (it is AbstractButton) {
          str = it.text
        }
        log.append(" $str")
      }
    log.append("\n----\n")
  }

  val p1 = JPanel()
  val group = ButtonGroup()
  listOf("JRadioButton1", "JRadioButton2")
    .map { JRadioButton(it) }
    .forEach {
      it.addItemListener(listener)
      group.add(it)
      p1.add(it)
    }

  val p2 = JPanel()
  listOf("JCheckBox1", "JCheckBox2", "JCheckBox3")
    .map { GroupCheckBox(it) }
    .forEach {
      it.addItemListener(listener)
      p2.add(it)
    }

  val model = arrayOf("One", "Tow", "Three")
  val combo1 = JComboBox(model)
  combo1.addItemListener(listener)

  val combo2 = CheckedComboBox(makeModel())
  combo2.addItemListener(listener)

  val box = Box.createVerticalBox()
  box.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
  box.add(p1)
  box.add(p2)
  box.add(combo1)
  box.add(Box.createVerticalStrut(2))
  box.add(combo2)
  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.add(JScrollPane(log))
    it.preferredSize = Dimension(320, 240)
  }
}

private class GroupCheckBox(
  title: String?,
) : JCheckBox(title) {
  override fun getSelectedObjects(): Array<Any> =
    parent
      .components
      .filterIsInstance<AbstractButton>()
      .filter { it.isSelected }
      .toTypedArray()
}

private fun makeModel(): ComboBoxModel<CheckItem> {
  val m = arrayOf(
    CheckItem("One", false),
    CheckItem("Tow", true),
    CheckItem("Three", false),
  )
  return DefaultComboBoxModel(m)
}

private data class CheckItem(
  val title: String,
  val isSelected: Boolean,
) {
  override fun toString() = title
}

private open class CheckedComboBox(
  model: ComboBoxModel<CheckItem>,
) : JComboBox<CheckItem>(model) {
  protected var keepOpen = false
  private val panel = JPanel(BorderLayout())

  override fun getPreferredSize() = Dimension(200, 20)

  override fun updateUI() {
    setRenderer(null)
    super.updateUI()
    val a = getAccessibleContext().getAccessibleChild(0)
    if (a is ComboPopup) {
      a.list.addMouseListener(object : MouseAdapter() {
        override fun mousePressed(e: MouseEvent) {
          val list = e.component as? JList<*> ?: return
          if (SwingUtilities.isLeftMouseButton(e)) {
            keepOpen = true
            updateItem(list.locationToIndex(e.point))
          }
        }
      })
    }
    val renderer = DefaultListCellRenderer()
    val check = JCheckBox()
    check.isOpaque = false
    setRenderer { list, value, index, isSelected, cellHasFocus ->
      panel.removeAll()
      val c = renderer.getListCellRendererComponent(
        list,
        value,
        index,
        isSelected,
        cellHasFocus,
      )
      if (index < 0) {
        c.foreground = list.foreground
        (c as? JLabel)?.also {
          it.text = selectedObjects.joinToString { o -> o.toString() }
          it.isOpaque = false
        }
        panel.isOpaque = false
      } else {
        check.isSelected = value.isSelected
        panel.add(check, BorderLayout.WEST)
        panel.isOpaque = true
        panel.background = c.background
      }
      panel.add(c)
      panel
    }
    initActionMap()
  }

  protected fun initActionMap() {
    getInputMap(JComponent.WHEN_FOCUSED).put(
      KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0),
      "checkbox-select",
    )
    val action = object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent) {
        (getAccessibleContext().getAccessibleChild(0) as? ComboPopup)?.also {
          updateItem(it.list.selectedIndex)
        }
      }
    }
    actionMap.put("checkbox-select", action)
  }

  protected fun updateItem(index: Int) {
    val item = getItemAt(index)
    if (isPopupVisible && item != null) {
      removeItemAt(index)
      insertItemAt(CheckItem(item.title, !item.isSelected), index)
      selectedIndex = index
    }
  }

  override fun setPopupVisible(v: Boolean) {
    if (keepOpen) {
      keepOpen = false
    } else {
      super.setPopupVisible(v)
    }
  }

  override fun getSelectedObjects() = // : Array<Any> {
    (0..<itemCount)
      .toList()
      .map { model.getElementAt(it) }
      .filter(CheckItem::isSelected)
      .toTypedArray()

//  protected fun getCheckedItemString(
//    model: ListModel<out CheckItem>,
//  ) = (0..<itemCount)
//    .asSequence()
//    .map { model.getElementAt(it) }
//    .filter { it.isSelected }
//    .map { it.toString() }
//    .sorted()
//    .joinToString()
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
