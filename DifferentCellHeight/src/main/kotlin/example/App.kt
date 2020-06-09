package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.border.Border

fun makeUI(): Component {
  val m = DefaultListModel<String>()
  m.addElement("111")
  m.addElement("111\n222222")
  m.addElement("111\n222222\n333333333")
  m.addElement("111\n222222\n333333333\n444444444444")
  return JPanel(GridLayout(1, 0)).also {
    it.add(JScrollPane(makeList(m, false)))
    it.add(JScrollPane(makeList(m, true)))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun <E> makeList(model: ListModel<E>, hasTextAreaRenderer: Boolean): JList<E> {
  return object : JList<E>(model) {
    override fun updateUI() {
      cellRenderer = null
      super.updateUI()
      if (hasTextAreaRenderer) {
        cellRenderer = TextAreaRenderer<E>()
        if (fixedCellHeight != -1) {
          println(fixedCellHeight)
          fixedCellHeight = -1
        }
      }
    }
  }
}

private class TextAreaRenderer<E> : JTextArea(), ListCellRenderer<E> {
  @Transient private var noFocusBorder: Border? = null
  @Transient private var focusBorder: Border? = null

  override fun getListCellRendererComponent(
    list: JList<out E>,
    value: E?,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean
  ): Component {
    text = value?.toString() ?: ""
    if (isSelected) {
      background = Color(list.selectionBackground.rgb) // Nimbus
      setForeground(list.selectionForeground)
    } else {
      background = if (index % 2 == 0) EVEN_COLOR else list.background
      setForeground(list.foreground)
    }
    if (cellHasFocus) {
      setBorder(focusBorder)
    } else {
      setBorder(noFocusBorder)
    }
    return this
  }

  override fun updateUI() {
    super.updateUI()
    focusBorder = UIManager.getBorder("List.focusCellHighlightBorder")?.also {
      val i = it.getBorderInsets(this)
      noFocusBorder = UIManager.getBorder("List.noFocusBorder")
        ?: BorderFactory.createEmptyBorder(i.top, i.left, i.bottom, i.right)
    }
  }

  companion object {
    private val EVEN_COLOR = Color(0xE6_FF_E6)
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
