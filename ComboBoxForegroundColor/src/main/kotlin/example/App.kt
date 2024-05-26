package example

import java.awt.*
import java.io.Serializable
import java.util.Objects
import javax.swing.*

fun makeUI(): Component {
  val model = arrayOf(
    ColorItem(Color.RED, "Red"),
    ColorItem(Color.GREEN, "Green"),
    ColorItem(Color.BLUE, "Blue"),
    ColorItem(Color.CYAN, "Cyan"),
    ColorItem(Color.ORANGE, "Orange"),
    ColorItem(Color.MAGENTA, "Magenta"),
  )
  val combo00 = JComboBox(model)

  val combo01 = object : JComboBox<ColorItem>(model) {
    override fun updateUI() {
      setRenderer(null)
      super.updateUI()
      setRenderer(ComboForegroundRenderer(this))
    }
  }

  val combo02 = object : JComboBox<ColorItem>(model) {
    override fun updateUI() {
      super.updateUI()
      setRenderer(ComboHtmlRenderer())
    }
  }

  val box = Box.createVerticalBox()
  box.add(makeTitledPanel("default:", combo00))
  box.add(Box.createVerticalStrut(5))
  box.add(makeTitledPanel("setForeground:", combo01))
  box.add(Box.createVerticalStrut(5))
  box.add(makeTitledPanel("html tag:", combo02))
  box.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(
  title: String,
  c: Component,
): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
}

private data class ColorItem(val color: Color, val description: String) : Serializable {
  override fun hashCode() = Objects.hash(color, description)

  override fun equals(other: Any?) =
    this === other || other is ColorItem && equals(other)

  private fun equals(item: ColorItem) =
    item.color == color && item.toString() == description

  override fun toString() = description
}

private class ComboForegroundRenderer(
  private val combo: JComboBox<ColorItem>,
) : ListCellRenderer<ColorItem> {
  private val renderer: ListCellRenderer<in ColorItem> = DefaultListCellRenderer()
  private val selectedBackground = Color(0xF0_F5_FA)

  override fun getListCellRendererComponent(
    list: JList<out ColorItem>,
    value: ColorItem?,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean,
  ): Component {
    val ic = value?.color?.also {
      if (index < 0 && it != combo.foreground) {
        combo.foreground = it // Windows, Motif Look&Feel
        list.selectionForeground = it
        list.selectionBackground = selectedBackground
      }
    }
    val c = renderer.getListCellRendererComponent(
      list,
      value,
      index,
      isSelected,
      cellHasFocus,
    )
    c.foreground = ic
    c.background = if (isSelected) selectedBackground else list.background
    return c
  }
}

private class ComboHtmlRenderer : ListCellRenderer<ColorItem> {
  private val renderer: ListCellRenderer<in ColorItem> = DefaultListCellRenderer()
  private val selectedBackground = Color(0xF0_F5_FA)

  override fun getListCellRendererComponent(
    list: JList<out ColorItem>,
    value: ColorItem?,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean,
  ): Component {
    if (index < 0) {
      list.selectionBackground = selectedBackground
    }
    val c = renderer.getListCellRendererComponent(
      list,
      value,
      index,
      isSelected,
      cellHasFocus,
    )
    if (c is JLabel && value != null) {
      c.text = "<html><font color='#%06X'>%s".format(
        value.color.rgb and 0xFF_FF_FF,
        value.description,
      )
    }
    c.background = if (isSelected) selectedBackground else list.background
    return c
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
