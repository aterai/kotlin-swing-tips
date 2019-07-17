package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI() = JPanel(BorderLayout()).also {
  val model = makeComboBoxModel()
  val combo = JComboBox(model)
  initComboBoxRenderer(combo)

  it.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
  it.add(makeTitledPanel("Left Clip JComboBox", combo), BorderLayout.NORTH)
  it.add(makeTitledPanel("Default JComboBox", JComboBox(model)), BorderLayout.SOUTH)
  it.setPreferredSize(Dimension(320, 240))
}

private fun getArrowButton(c: Container) = c.getComponents().filterIsInstance(JButton::class.java).firstOrNull()

private fun makeTitledPanel(title: String, c: Component) = Box.createVerticalBox().also {
  it.setBorder(BorderFactory.createTitledBorder(title))
  it.add(Box.createVerticalStrut(2))
  it.add(c)
}

private fun makeComboBoxModel() = DefaultComboBoxModel<String>().also {
  it.addElement("1234567890123456789012/3456789012345678901234567890123/456789012345678901234567890.jpg")
  it.addElement("aaaa.tif")
  it.addElement("\\asdfsadfs\\afsdfasdf\\asdfasdfasd.avi")
  it.addElement("aaaabbbcc.pdf")
  it.addElement("c:/b12312343245/643667345624523451/324513/41234125/134513451345135125123412341bb1.mpg")
  it.addElement("http://localhost/1234567890123456789/345678901234567890123456789/456789012345678901234567890.jpg")
}

private fun initComboBoxRenderer(combo: JComboBox<String>) {
  combo.setRenderer(object : DefaultListCellRenderer() {
    override fun getListCellRendererComponent(
      list: JList<*>,
      value: Any?,
      index: Int,
      isSelected: Boolean,
      cellHasFocus: Boolean
    ): Component {
      super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
      val width = getAvailableWidth(combo, index)
      setText(getLeftClippedText(value?.toString() ?: "", getFontMetrics(getFont()), width))
      return this
    }

    private fun getAvailableWidth(combo: JComboBox<String>, index: Int): Int {
      var itb = 0
      var ilr = 0
      var insets = getInsets()
      itb += insets.top + insets.bottom
      ilr += insets.left + insets.right
      insets = combo.getInsets()
      itb += insets.top + insets.bottom
      ilr += insets.left + insets.right
      var availableWidth = combo.getWidth() - ilr
      if (index < 0) {
        // @see BasicComboBoxUI#rectangleForCurrentValue
        availableWidth -= getArrowButton(combo)?.getWidth() ?: combo.getHeight() - itb
        insets = (combo.getEditor().getEditorComponent() as JTextField).getMargin()
        availableWidth -= insets.left + insets.right
      }
      return availableWidth
    }

    private fun getLeftClippedText(text: String, fm: FontMetrics, availableWidth: Int): String {
      if (fm.stringWidth(text) <= availableWidth) {
        return text
      }
      val dots = "..."
      var textWidth = fm.stringWidth(dots)
      val len = text.length
      // @see Unicode surrogate programming with the Java language
      // https://www.ibm.com/developerworks/library/j-unicode/index.html
      // https://www.ibm.com/developerworks/jp/ysl/library/java/j-unicode_surrogate/index.html
      val acp = IntArray(text.codePointCount(0, len))
      var j = acp.size
      var i = len
      while (i > 0) {
        val cp = text.codePointBefore(i)
        textWidth += fm.charWidth(cp)
        if (textWidth > availableWidth) {
          break
        }
        acp[--j] = cp
        i = text.offsetByCodePoints(i, -1)
      }
      return dots + String(acp, j, acp.size - j)
    }
  })
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
