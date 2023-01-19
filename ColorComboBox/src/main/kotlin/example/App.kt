package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val combo01 = AlternateRowColorComboBox<String>(makeModel())

  val combo02 = AlternateRowColorComboBox<String>(makeModel())
  combo02.isEditable = true

  val box = Box.createVerticalBox().also {
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.add(makeTitledPanel("setEditable(false)", combo01))
    it.add(Box.createVerticalStrut(5))
    it.add(makeTitledPanel("setEditable(true)", combo02))
  }

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(title: String, c: Component) = JPanel(BorderLayout()).also {
  it.border = BorderFactory.createTitledBorder(title)
  it.add(c)
}

private fun makeModel() = DefaultComboBoxModel<String>().also {
  it.addElement("aaa")
  it.addElement("aaa111")
  it.addElement("aaa222bb")
  it.addElement("1234123512351234")
  it.addElement("bbb1")
  it.addElement("bbb12")
}

private class AlternateRowColorComboBox<E>(model: ComboBoxModel<E>) : JComboBox<E>(model) {
  private var itemColorListener: ItemListener? = null

  // constructor() : super()

  // constructor(model: ComboBoxModel<E>) : super(model)

  // constructor(items: Array<E>) : super(items)

  override fun setEditable(flag: Boolean) {
    super.setEditable(flag)
    if (flag) {
      val editor = getEditor().editorComponent
      (editor as? JComponent)?.isOpaque = true
      editor.background = getAlternateRowColor(selectedIndex)
    }
  }

  override fun updateUI() {
    removeItemListener(itemColorListener)
    setRenderer(null)
    super.updateUI()
    val renderer = getRenderer()
    setRenderer { list, value, index, isSelected, cellHasFocus ->
      renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus).also {
        (it as? JComponent)?.isOpaque = true
        if (!isSelected) {
          it.background = getAlternateRowColor(index)
        }
      }
    }
    itemColorListener = ItemListener { e ->
      val cb = e.itemSelectable
      if (e.stateChange == ItemEvent.SELECTED && cb is JComboBox<*>) {
        val rc = getAlternateRowColor(cb.selectedIndex)
        if (cb.isEditable) {
          cb.editor.editorComponent.background = rc
        } else {
          cb.background = rc
        }
      }
    }
    addItemListener(itemColorListener)
    EventQueue.invokeLater {
      (getEditor().editorComponent as? JComponent)?.also {
        it.isOpaque = true
        it.background = getAlternateRowColor(selectedIndex)
      }
    }
  }

  private fun getAlternateRowColor(idx: Int) = if (idx % 2 == 0) Color(0xE1_FF_E1) else Color.WHITE
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
