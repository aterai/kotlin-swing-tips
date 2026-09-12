package example

import java.awt.*
import javax.swing.*
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import javax.swing.plaf.basic.ComboPopup

fun createUI(): Component {
  val c = GridBagConstraints()
  c.gridheight = 1
  c.gridwidth = 1
  c.anchor = GridBagConstraints.WEST
  c.insets = Insets(5, 5, 5, 5)

  c.gridx = 0
  c.gridy = 0
  c.weightx = 0.0
  val p = JPanel(GridBagLayout())
  p.add(JLabel("PreferredSize:"), c)

  c.gridx = 1
  c.weightx = 1.0
  p.add(IconComboBox(createModel()), c)

  c.gridx = 0
  c.gridy = 1
  c.weightx = 0.0
  p.add(JLabel("PopupMenuListener:"), c)

  c.gridx = 1
  c.weightx = 1.0
  p.add(IconWrapComboBox(createModel()), c)
  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private open class IconComboBox(
  model: ComboBoxModel<Icon>,
) : JComboBox<Icon>(model) {
  override fun getPreferredSize(): Dimension {
    val i = insets
    val w = PROTOTYPE.iconWidth * getColumnCount(itemCount, ROW_COUNT)
    val h = PROTOTYPE.iconHeight
    return Dimension(w + i.left + i.right, h + i.top + i.bottom)
  }

  override fun updateUI() {
    super.updateUI()
    maximumRowCount = ROW_COUNT
    prototypeDisplayValue = PROTOTYPE
    (accessibleContext.getAccessibleChild(0) as? ComboPopup)?.list?.also {
      it.layoutOrientation = JList.HORIZONTAL_WRAP
      it.visibleRowCount = ROW_COUNT
      it.fixedCellWidth = PROTOTYPE.iconWidth
      it.fixedCellHeight = PROTOTYPE.iconHeight
    }
  }

  companion object {
    val PROTOTYPE: Icon = ColorIcon(Color.DARK_GRAY)
    const val ROW_COUNT = 3

    // Number of columns needed to lay out itemCount cells in rowCount rows
    fun getColumnCount(
      itemCount: Int,
      rowCount: Int,
    ) = (itemCount + rowCount - 1) / rowCount
  }
}

private class IconWrapComboBox(
  model: ComboBoxModel<Icon>,
) : IconComboBox(model) {
  @Transient private var listener: PopupMenuListener? = null

  override fun getPreferredSize(): Dimension {
    val i = insets
    val w = PROTOTYPE.iconWidth
    val h = PROTOTYPE.iconHeight
    val buttonWidth = 20 // ???
    return Dimension(buttonWidth + w + i.left + i.right, h + i.top + i.bottom)
  }

  override fun updateUI() {
    setRenderer(null)
    removePopupMenuListener(listener)
    super.updateUI()
    val renderer = getRenderer()
    setRenderer { list, value, index, isSelected, cellHasFocus ->
      renderer
        .getListCellRendererComponent(
          list,
          value,
          index,
          isSelected,
          cellHasFocus,
        ).also {
          if (it is JLabel) {
            it.icon = value
            it.border = BorderFactory.createEmptyBorder()
          }
        }
    }
    listener = WidePopupMenuListener(ROW_COUNT, PROTOTYPE)
    addPopupMenuListener(listener)
  }
}

private class WidePopupMenuListener(
  private val rowCount: Int,
  private val prototype: Icon,
) : PopupMenuListener {
  override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
    val combo = e.source as? JComboBox<*> ?: return
    val i = combo.insets
    val columnCount = IconComboBox.getColumnCount(combo.itemCount, rowCount)
    val popupWidth = prototype.iconWidth * columnCount + i.left + i.right
    val size = combo.size
    if (size.width < popupWidth) {
      // Temporarily widen the combo box so that BasicComboPopup#getPopupLocation()
      // sizes the popup from the widened bounds. The nested showPopup() fires this
      // listener again, but the width check above prevents infinite recursion.
      combo.setSize(popupWidth, size.height)
      combo.showPopup()
      // // Java 8
      // combo.size = size
      // Java 21: the outer BasicComboPopup#show() still calls getPopupLocation()
      // after this listener returns, so restoring the size synchronously would
      // shrink the already visible popup back to the combo box width.
      EventQueue.invokeLater { combo.size = size }
    }
  }

  override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
    // not needed
  }

  override fun popupMenuCanceled(e: PopupMenuEvent) {
    // not needed
  }
}

private fun createModel(): ComboBoxModel<Icon> {
  val model = DefaultComboBoxModel<Icon>()
  model.addElement(ColorIcon(Color.RED))
  model.addElement(ColorIcon(Color.GREEN))
  model.addElement(ColorIcon(Color.BLUE))
  model.addElement(ColorIcon(Color.ORANGE))
  model.addElement(ColorIcon(Color.CYAN))
  model.addElement(ColorIcon(Color.PINK))
  model.addElement(ColorIcon(Color.YELLOW))
  model.addElement(ColorIcon(Color.MAGENTA))
  model.addElement(ColorIcon(Color.GRAY))
  return model
}

private class ColorIcon(
  private val color: Color,
) : Icon {
  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = color
    g2.fillRect(1, 1, iconWidth - 2, iconHeight - 2)
    g2.dispose()
  }

  override fun getIconWidth() = 32

  override fun getIconHeight() = 32
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
