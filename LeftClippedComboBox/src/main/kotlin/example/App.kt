package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI() = JPanel(BorderLayout()).also {
  val model = makeComboBoxModel()
  val combo = JComboBox(model)
  initComboBoxRenderer(combo)

  it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  it.add(makeTitledPanel("Left Clip JComboBox", combo), BorderLayout.NORTH)
  it.add(makeTitledPanel("Default JComboBox", JComboBox(model)), BorderLayout.SOUTH)
  it.preferredSize = Dimension(320, 240)
}

private fun getArrowButton(c: Container) = c.components.filterIsInstance<JButton>().firstOrNull()

private fun makeTitledPanel(title: String, c: Component) = Box.createVerticalBox().also {
  it.border = BorderFactory.createTitledBorder(title)
  it.add(Box.createVerticalStrut(2))
  it.add(c)
}

private fun makeComboBoxModel() = DefaultComboBoxModel<String>().also {
  it.addElement("1234567890123456789012/3456789012345678901234567890/12345678901234567890.jpg")
  it.addElement("abc.tif")
  it.addElement("\\0123456789\\0123456789\\0123456789.avi")
  it.addElement("0123456789.pdf")
  it.addElement("c:/12312343245/643667345624523451/324513/41234125/134513451345135125.mpg")
  it.addElement("http://localhost/1234567890123456789/3456789012345/678901234567894567890.jpg")
}

private fun initComboBoxRenderer(combo: JComboBox<String>) {
  combo.renderer = object : DefaultListCellRenderer() {
    override fun getListCellRendererComponent(
      list: JList<*>,
      value: Any?,
      index: Int,
      isSelected: Boolean,
      cellHasFocus: Boolean
    ): Component {
      val c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
      if (c is JLabel) {
        val width = getAvailableWidth(combo, index)
        c.text = getLeftClippedText(value?.toString() ?: "", c.getFontMetrics(c.font), width)
      }
      return c
    }

    private fun getAvailableWidth(combo: JComboBox<String>, index: Int): Int {
      var itb = 0
      var ilr = 0
      var insets = insets
      itb += insets.top + insets.bottom
      ilr += insets.left + insets.right
      insets = combo.insets
      itb += insets.top + insets.bottom
      ilr += insets.left + insets.right
      var availableWidth = combo.width - ilr
      if (index < 0) {
        // @see BasicComboBoxUI#rectangleForCurrentValue
        val maxButtonHeight = combo.height - itb
        availableWidth -= getArrowButton(combo)?.width ?: maxButtonHeight
        (combo.editor.editorComponent as? JTextField)?.margin?.also {
          availableWidth -= it.left + it.right
        }
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
