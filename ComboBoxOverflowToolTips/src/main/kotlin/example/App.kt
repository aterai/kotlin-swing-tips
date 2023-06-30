package example

import java.awt.*
import javax.swing.*

fun makeUI() = JPanel(BorderLayout()).also {
  val m = makeComboBoxModel()
  it.add(makeTitledPanel("Overflow ToolTip JComboBox", makeComboBox(m)), BorderLayout.NORTH)
  it.add(makeTitledPanel("Default JComboBox", JComboBox(m)), BorderLayout.SOUTH)
  it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  it.preferredSize = Dimension(320, 240)
}

private fun makeTitledPanel(title: String, c: Component) = Box.createVerticalBox().also {
  it.border = BorderFactory.createTitledBorder(title)
  it.add(Box.createVerticalStrut(2))
  it.add(c)
}

private fun makeComboBoxModel() = DefaultComboBoxModel<String>().also {
  it.addElement("0123456789/0123456789/0123456789/0123456789/01.jpg")
  it.addElement("abc.tif")
  it.addElement("aaa-bbb-ccc.pdf")
  it.addElement("c:/0123456789/0123456789/0123456789/0123456789/02.mpg")
  it.addElement("http://localhost/0123456789/0123456789/0123456789/0123456789/03.png")
}

private fun <E> makeComboBox(model: ComboBoxModel<E>) = object : JComboBox<E>(model) {
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
        cellHasFocus
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
          (combo.getEditor().editorComponent as? JTextField)?.also {
            availableWidth -= it.margin.left + it.margin.right
            combo.toolTipText = toolTipTxt
          }
        }
      }
    }
  }

  private fun getArrowButton(combo: Container): JButton? =
    combo.components?.firstOrNull { it is JButton } as? JButton
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
