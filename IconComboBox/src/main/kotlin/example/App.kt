package example

import java.awt.*
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.*
import kotlin.math.roundToInt

fun makeUI() = JPanel(BorderLayout()).also {
  val path = "example/16x16.png"
  val cl = Thread.currentThread().contextClassLoader
  val img = cl.getResource(path)?.openStream()?.use(ImageIO::read) ?: makeMissingImage()
  val icon = ImageIcon(img)

  val combo02 = JComboBox(makeModel())
  initComboBoxRenderer(combo02, icon)

  val combo03 = JComboBox(makeModel())
  combo03.isEditable = true
  initComboBoxRenderer(combo03, icon)

  val combo05 = object : JComboBox<String>(makeModel()) {
    override fun updateUI() {
      border = null
      setRenderer(null)
      super.updateUI()
      setEditable(true)
      initComboBoxRenderer(this, icon)
      initIconComboBorder1(this, icon)
    }
  }

  val combo06 = object : JComboBox<String>(makeModel()) {
    override fun updateUI() {
      border = null
      setRenderer(null)
      super.updateUI()
      setEditable(true)
      initComboBoxRenderer(this, icon)
      initIconComboBorder2(this, icon)
    }
  }

  val box = Box.createVerticalBox()
  box.add(makeTitledPanel("setEditable(false)", combo02))
  box.add(Box.createVerticalStrut(5))
  box.add(makeTitledPanel("setEditable(true)", combo03, combo05, combo06))

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeModel() = DefaultComboBoxModel<String>().also {
  it.addElement("aaa")
  it.addElement("aaa, bbb")
  it.addElement("aaa, bbb, cc")
  it.addElement("ccc, ccc, ccc, ccc, ccc")
  it.addElement("bbb1")
  it.addElement("bbb12")
}

private fun makeTitledPanel(
  title: String,
  vararg list: Component,
): Component {
  val p = JPanel(GridBagLayout())
  p.border = BorderFactory.createTitledBorder(title)
  val c = GridBagConstraints()
  c.fill = GridBagConstraints.HORIZONTAL
  c.insets = Insets(5, 5, 5, 5)
  c.weightx = 1.0
  c.gridx = GridBagConstraints.REMAINDER
  for (cmp in list) {
    p.add(cmp, c)
  }
  return p
}

private fun initIconComboBorder1(
  comboBox: JComboBox<*>,
  icon: Icon,
) {
  val c = comboBox.editor.editorComponent as? JTextField ?: return
  val wrappedIcon = object : Icon {
    override fun paintIcon(
      c: Component,
      g: Graphics,
      x: Int,
      y: Int,
    ) {
      val g2 = g.create() as? Graphics2D ?: return
      val ih = icon.iconHeight
      val ch = iconHeight
      val yy = ((ch - ih) / 2f).roundToInt().coerceAtLeast(0)
      icon.paintIcon(c, g2, 0, yy)
      g2.dispose()
    }

    override fun getIconWidth() = icon.iconWidth

    override fun getIconHeight() = c.preferredSize.height - c.insets.top - c.insets.bottom
  }
  val b1 = BorderFactory.createMatteBorder(0, icon.iconWidth, 0, 0, wrappedIcon)
  val b2 = BorderFactory.createEmptyBorder(0, 5, 0, 0)
  val b3 = BorderFactory.createCompoundBorder(b1, b2)
  c.border = BorderFactory.createCompoundBorder(c.border, b3)
}

private fun initIconComboBorder2(
  comboBox: JComboBox<*>,
  icon: Icon,
) {
  EventQueue.invokeLater {
    val margin = BorderFactory.createEmptyBorder(0, icon.iconWidth + 2, 0, 2)
    (comboBox.editor.editorComponent as? JTextField)?.also {
      val b = it.border
      it.border = BorderFactory.createCompoundBorder(b, margin)
      val label = JLabel(icon)
      label.cursor = Cursor.getDefaultCursor()
      label.border = BorderFactory.createEmptyBorder()
      it.add(label)
      val ih = icon.iconHeight
      val ch = comboBox.preferredSize.height
      val yy = ((ch - ih) / 2f).roundToInt().coerceAtLeast(0)
      label.setBounds(b.getBorderInsets(it).left, yy, icon.iconWidth, icon.iconHeight)
    }
  }
}

private fun <E> initComboBoxRenderer(
  combo: JComboBox<E>,
  icon: Icon,
) {
  val renderer = combo.renderer
  combo.setRenderer { list, value, index, isSelected, cellHasFocus ->
    renderer
      .getListCellRendererComponent(
        list,
        value,
        index,
        isSelected,
        cellHasFocus,
      ).also {
        (it as? JLabel)?.icon = icon
      }
  }
}

private fun makeMissingImage(): Image {
  val missingIcon = UIManager.getIcon("html.missingImage")
  val w = missingIcon.iconWidth
  val h = missingIcon.iconHeight
  val bi = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
  val g2 = bi.createGraphics()
  missingIcon.paintIcon(null, g2, 8 - w / 2, 8 - h / 2)
  g2.dispose()
  return bi
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
