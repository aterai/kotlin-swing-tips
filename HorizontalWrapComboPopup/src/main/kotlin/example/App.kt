package example

import java.awt.*
import javax.swing.*
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import javax.swing.plaf.basic.ComboPopup

fun makeUI(): Component {
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
  p.add(makeComboBox1(makeModel(), ColorIcon(Color.DARK_GRAY)), c)

  c.gridx = 0
  c.gridy = 1
  c.weightx = 0.0
  p.add(JLabel("PopupMenuListener:"), c)

  c.gridx = 1
  c.weightx = 1.0
  p.add(makeComboBox2(makeModel(), ColorIcon(Color.DARK_GRAY)), c)
  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeComboBox1(model: ComboBoxModel<Icon>, proto: Icon): JComboBox<Icon> {
  return object : JComboBox<Icon>(model) {
    override fun getPreferredSize(): Dimension {
      val i = insets
      val w = proto.iconWidth
      val h = proto.iconHeight
      return Dimension(w * 3 + i.left + i.right, h + i.top + i.bottom)
    }

    override fun updateUI() {
      super.updateUI()
      setMaximumRowCount(3)
      prototypeDisplayValue = proto
      val popup = getAccessibleContext().getAccessibleChild(0) as? ComboPopup ?: return
      val list = popup.list
      list.layoutOrientation = JList.HORIZONTAL_WRAP
      list.visibleRowCount = 3
      list.fixedCellWidth = proto.iconWidth
      list.fixedCellHeight = proto.iconHeight
    }
  }
}

private fun makeComboBox2(model: ComboBoxModel<Icon>, proto: Icon): JComboBox<Icon> {
  val combo = object : JComboBox<Icon>(model) {
    override fun getPreferredSize(): Dimension {
      val i = insets
      val w = proto.iconWidth
      val h = proto.iconHeight
      return Dimension(20 + w + i.left + i.right, h + i.top + i.bottom)
    }

    override fun updateUI() {
      setRenderer(null)
      super.updateUI()
      setMaximumRowCount(3)
      prototypeDisplayValue = proto
      val r = getRenderer()
      setRenderer { list, value, index, isSelected, cellHasFocus ->
        r.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus).also {
          if (it is JLabel) {
            it.icon = value
            it.border = BorderFactory.createEmptyBorder()
          }
        }
      }
      (getAccessibleContext().getAccessibleChild(0) as? ComboPopup)?.list?.also {
        it.layoutOrientation = JList.HORIZONTAL_WRAP
        it.visibleRowCount = 3
        it.fixedCellWidth = proto.iconWidth
        it.fixedCellHeight = proto.iconHeight
      }
    }
  }
  val pl = object : PopupMenuListener {
    private var adjusting = false
    override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
      val comboBox = e.source as? JComboBox<*> ?: return
      val i = comboBox.insets
      val popupWidth = proto.iconWidth * 3 + i.left + i.right
      val size = comboBox.size
      if (size.width >= popupWidth) {
        return
      }
      if (!adjusting) {
        adjusting = true
        comboBox.setSize(popupWidth, size.height)
        comboBox.showPopup()
      }
      comboBox.size = size
      adjusting = false
    }

    override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
      // not needed
    }

    override fun popupMenuCanceled(e: PopupMenuEvent) {
      // not needed
    }
  }
  combo.addPopupMenuListener(pl)
  return combo
}

private fun makeModel(): ComboBoxModel<Icon> {
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

private class ColorIcon(private val color: Color) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
