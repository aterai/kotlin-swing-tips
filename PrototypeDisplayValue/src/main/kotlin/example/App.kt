package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

private val TITLE = "M".repeat(20)

fun makeUI(): Component {
  val model1 = DefaultComboBoxModel(arrayOf("a", "b", "c"))
  val combo1 = JComboBox(model1)
  combo1.isEditable = false

  val combo2 = JComboBox(model1)
  combo2.prototypeDisplayValue = TITLE

  val combo3 = JComboBox(model1)
  combo3.prototypeDisplayValue = TITLE
  combo3.isEditable = true

  val arrayOfWebSites = arrayOf(
    WebSite("a", ColorIcon(Color.RED)),
    WebSite("b", ColorIcon(Color.GREEN)),
    WebSite("c", ColorIcon(Color.BLUE))
  )
  val model2 = DefaultComboBoxModel(arrayOfWebSites)

  val combo4 = JComboBox(model2)
  combo4.renderer = SiteListCellRenderer()

  val combo5 = JComboBox(model2)
  combo5.renderer = SiteListCellRenderer()
  combo5.prototypeDisplayValue = WebSite(TITLE, ColorIcon(Color.GRAY))

  val combo6 = JComboBox<WebSite>()
  combo6.renderer = SiteListCellRenderer()
  combo6.prototypeDisplayValue = WebSite(TITLE, ColorIcon(Color.GRAY))

  return JPanel().also {
    val layout = SpringLayout()
    it.layout = layout
    layout.putConstraint(SpringLayout.WEST, combo1, 10, SpringLayout.WEST, it)
    layout.putConstraint(SpringLayout.WEST, combo2, 10, SpringLayout.WEST, it)
    layout.putConstraint(SpringLayout.WEST, combo3, 10, SpringLayout.WEST, it)
    layout.putConstraint(SpringLayout.WEST, combo4, 10, SpringLayout.WEST, it)
    layout.putConstraint(SpringLayout.WEST, combo5, 10, SpringLayout.WEST, it)
    layout.putConstraint(SpringLayout.WEST, combo6, 10, SpringLayout.WEST, it)
    layout.putConstraint(SpringLayout.NORTH, combo1, 10, SpringLayout.NORTH, it)
    layout.putConstraint(SpringLayout.NORTH, combo2, 10, SpringLayout.SOUTH, combo1)
    layout.putConstraint(SpringLayout.NORTH, combo3, 10, SpringLayout.SOUTH, combo2)
    layout.putConstraint(SpringLayout.NORTH, combo4, 10, SpringLayout.SOUTH, combo3)
    layout.putConstraint(SpringLayout.NORTH, combo5, 10, SpringLayout.SOUTH, combo4)
    layout.putConstraint(SpringLayout.NORTH, combo6, 10, SpringLayout.SOUTH, combo5)
    it.add(combo1)
    it.add(combo2)
    it.add(combo3)
    it.add(combo4)
    it.add(combo5)
    it.add(combo6)
    it.preferredSize = Dimension(320, 240)
  }
}

private open class WebSite(val title: String, val favicon: Icon)

private class ColorIcon(private val color: Color) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = color
    g2.fillOval(4, 4, iconWidth - 8, iconHeight - 8)
    g2.dispose()
  }

  override fun getIconWidth() = 24

  override fun getIconHeight() = 24
}

private class SiteListCellRenderer<E : WebSite> : JLabel(), ListCellRenderer<E> {
  override fun getListCellRendererComponent(
    list: JList<out E>,
    value: E?,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean
  ): Component {
    isOpaque = index >= 0
    isEnabled = list.isEnabled
    font = list.font
    if (value != null) {
      text = value.title
      icon = value.favicon
    }
    if (isSelected) {
      background = list.selectionBackground
      setForeground(list.selectionForeground)
    } else {
      background = list.background
      setForeground(list.foreground)
    }
    return this
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
