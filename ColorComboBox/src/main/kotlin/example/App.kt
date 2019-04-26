package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  init {
    val combo01 = AlternateRowColorComboBox<String>(makeModel())

    val combo02 = AlternateRowColorComboBox<String>(makeModel())
    combo02.setEditable(true)

    add(Box.createVerticalBox().also {
      it.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
      it.add(makeTitledPanel("setEditable(false)", combo01))
      it.add(Box.createVerticalStrut(5))
      it.add(makeTitledPanel("setEditable(true)", combo02))
    }, BorderLayout.NORTH)
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeTitledPanel(title: String, c: Component) = JPanel(BorderLayout()).also {
    it.setBorder(BorderFactory.createTitledBorder(title))
    it.add(c)
  }

  private fun makeModel() = DefaultComboBoxModel<String>().also {
    it.addElement("aaaa")
    it.addElement("aaaabbb")
    it.addElement("aaaabbbcc")
    it.addElement("1234123512351234")
    it.addElement("bbb1")
    it.addElement("bbb12")
  }
}

internal class AlternateRowColorComboBox<E> : JComboBox<E> {
  @Transient
  private var itemColorListener: ItemListener? = null

  constructor() : super()

  constructor(model: ComboBoxModel<E>) : super(model)

  constructor(items: Array<E>) : super(items)

  override fun setEditable(flag: Boolean) {
    super.setEditable(flag)
    if (flag) {
      val field = getEditor().getEditorComponent() as JTextField
      field.setOpaque(true)
      field.setBackground(getAlternateRowColor(getSelectedIndex()))
    }
  }

  override fun updateUI() {
    removeItemListener(itemColorListener)
    super.updateUI()
    setRenderer(object : DefaultListCellRenderer() {
      override fun getListCellRendererComponent(
        list: JList<*>,
        value: Any?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
      ): Component {
        val c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
        c.setOpaque(true)
        if (!isSelected) {
          c.setBackground(getAlternateRowColor(index))
        }
        return c
      }
    })
    itemColorListener = ItemListener { e ->
      if (e.getStateChange() == ItemEvent.SELECTED) {
        val cb = e.getItemSelectable() as JComboBox<*>
        val rc = getAlternateRowColor(cb.getSelectedIndex())
        if (cb.isEditable()) {
          (cb.getEditor().getEditorComponent() as? JTextField)?.setBackground(rc)
        } else {
          cb.setBackground(rc)
        }
      }
    }
    addItemListener(itemColorListener)
    EventQueue.invokeLater {
      (getEditor().getEditorComponent() as? JTextField)?.also {
        it.setOpaque(true)
        it.setBackground(getAlternateRowColor(getSelectedIndex()))
      }
    }
  }

  protected fun getAlternateRowColor(idx: Int) = if (idx % 2 == 0) Color(0xE1_FF_E1) else Color.WHITE
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
