package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI() = JPanel(BorderLayout()).also {
  val m = makeComboBoxModel()
  it.add(makeTitledPanel("Overflow ToolTip JComboBox", makeComboBox(m)), BorderLayout.NORTH)
  it.add(makeTitledPanel("Default JComboBox", JComboBox(m)), BorderLayout.SOUTH)
  it.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
  it.setPreferredSize(Dimension(320, 240))
}

private fun makeTitledPanel(title: String, c: Component) = Box.createVerticalBox().also {
  it.setBorder(BorderFactory.createTitledBorder(title))
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
      val r = renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
      val c = r as? JComponent ?: return@setRenderer r
      val rect = SwingUtilities.calculateInnerArea(combo, null)
      val i = c.getInsets()
      var availableWidth = rect.width - i.top - i.bottom

      val str = value?.toString() ?: ""
      val fm = c.getFontMetrics(c.getFont())
      c.setToolTipText(if (fm.stringWidth(str) > availableWidth) str else null)
      if (index < 0) {
        val buttonSize = arrowButton?.getWidth() ?: rect.height
        availableWidth -= buttonSize
        (combo.getEditor().getEditorComponent() as? JTextField)?.also {
          availableWidth -= it.getMargin().left + it.getMargin().right
          combo.setToolTipText(if (fm.stringWidth(str) > availableWidth) str else null)
        }
      }
      return@setRenderer c
    }
  }

  private fun getArrowButton(combo: Container): JButton? {
    return combo.getComponents()?.firstOrNull { it is JButton } as? JButton
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(makeUI())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
