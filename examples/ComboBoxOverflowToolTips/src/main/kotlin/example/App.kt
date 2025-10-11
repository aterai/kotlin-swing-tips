package example

import java.awt.*
import javax.swing.*

fun makeUI() = JPanel(BorderLayout()).also {
  val m = makeComboBoxModel()
  val p1 = makeTitledPanel("Overflow ToolTip JComboBox", ToolTipComboBox(m))
  val p2 = makeTitledPanel("Default JComboBox", JComboBox(m))
  it.add(p1, BorderLayout.NORTH)
  it.add(p2, BorderLayout.SOUTH)
  it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  it.preferredSize = Dimension(320, 240)
}

private fun makeTitledPanel(
  title: String,
  c: Component,
): Component {
  val box = Box.createVerticalBox()
  box.border = BorderFactory.createTitledBorder(title)
  box.add(Box.createVerticalStrut(2))
  box.add(c)
  return box
}

private fun makeComboBoxModel() = DefaultComboBoxModel<String>().also {
  it.addElement("0123456789/0123456789/0123456789/0123456789/01.jpg")
  it.addElement("abc.tif")
  it.addElement("aaa-bbb-ccc.pdf")
  it.addElement("c:/0123456789/0123456789/0123456789/0123456789/02.mpg")
  it.addElement("http://localhost/0123456789/0123456789/0123456789/0123456789/03.png")
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

class ToolTipComboBox<E>(model: ComboBoxModel<E>) : JComboBox<E>(model) {
  override fun updateUI() {
    setRenderer(null)
    super.updateUI()
    val renderer = getRenderer()
    val combo = this
    val arrowButton = getArrowButton(combo)
    setRenderer { list, value, index, isSelected, cellHasFocus ->
      val c = renderer.getListCellRendererComponent(
        list,
        value,
        index,
        isSelected,
        cellHasFocus,
      )
      (c as? JComponent)?.also {
        val rect = SwingUtilities.calculateInnerArea(combo, null)
        val i = it.insets
        var availableWidth = rect.width - i.top - i.bottom
        val str = value?.toString() ?: ""
        val fm = it.getFontMetrics(it.font)
        val toolTipTxt = if (fm.stringWidth(str) > availableWidth) str else null
        it.toolTipText = toolTipTxt
        if (index < 0) {
          val buttonSize = arrowButton?.width ?: rect.height
          availableWidth -= buttonSize
          (combo.getEditor().editorComponent as? JTextField)?.also { editor ->
            val margin = editor.margin
            availableWidth -= margin.left + margin.right
            combo.toolTipText = if (fm.stringWidth(str) > availableWidth) str else null
          }
        }
      }
    }
  }

  private fun getArrowButton(combo: Container): JButton? =
    combo.components?.firstOrNull { it is JButton } as? JButton
}
