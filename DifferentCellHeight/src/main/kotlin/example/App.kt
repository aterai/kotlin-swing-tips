package example

import java.awt.*
import javax.swing.*
import javax.swing.border.Border

fun makeUI(): Component {
  val lm = listOf(
    "111",
    "222222",
    "333333333",
    "444444444444",
  )
  val m = DefaultListModel<String>()
  m.addElement(lm[0])
  m.addElement(lm.slice(0..1).joinToString(separator = "\n"))
  m.addElement(lm.slice(0..2).joinToString(separator = "\n"))
  m.addElement(lm.joinToString(separator = "\n"))
  val list = object : JList<String>(m) {
    override fun updateUI() {
      cellRenderer = null
      super.updateUI()
      cellRenderer = TextAreaRenderer()
      if (fixedCellHeight != -1) {
        fixedCellHeight = -1
      }
    }
  }
  return JPanel(GridLayout(1, 0)).also {
    it.add(JScrollPane(JList(m)))
    it.add(JScrollPane(list))
    it.preferredSize = Dimension(320, 240)
  }
}

private class TextAreaRenderer<E> : ListCellRenderer<E> {
  private var noFocusBorder: Border? = null
  private var focusBorder: Border? = null
  private val renderer = object : JTextArea() {
    override fun updateUI() {
      super.updateUI()
      focusBorder = UIManager.getBorder("List.focusCellHighlightBorder")
      noFocusBorder = getNoFocusBorder(focusBorder)
    }
  }

  private fun getNoFocusBorder(focusBorder: Border): Border {
    val b = UIManager.getBorder("List.noFocusBorder")
    return b ?: focusBorder.getBorderInsets(renderer).let {
      BorderFactory.createEmptyBorder(it.top, it.left, it.bottom, it.right)
    }
  }

  override fun getListCellRendererComponent(
    list: JList<out E>,
    value: E?,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean,
  ): Component {
    renderer.text = value?.toString() ?: ""
    if (isSelected) {
      renderer.background = Color(list.selectionBackground.rgb) // Nimbus
      renderer.foreground = list.selectionForeground
    } else {
      renderer.background = if (index % 2 == 0) EVEN_COLOR else list.background
      renderer.foreground = list.foreground
    }
    renderer.border = if (cellHasFocus) focusBorder else noFocusBorder
    return renderer
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
