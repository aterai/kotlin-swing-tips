package example

import java.awt.*
import javax.swing.*
import javax.swing.plaf.basic.BasicComboBoxUI
import javax.swing.plaf.basic.BasicComboPopup
import javax.swing.plaf.basic.BasicScrollBarUI

private val BACKGROUND = Color.WHITE
private val FOREGROUND = Color.BLACK
private val SELECTION_FOREGROUND = Color.BLUE
private val THUMB = Color(0xCD_CD_CD)
private const val KEY = "ComboBox.border"

fun makeUI(): Component {
  val p = JPanel(GridLayout(0, 1, 16, 16))
  p.isOpaque = true
  val combo1 = JComboBox(makeModel())
  p.add(combo1)

  val combo2 = object : JComboBox<String>(makeModel()) {
    override fun updateUI() {
      UIManager.put(KEY, BorderFactory.createLineBorder(Color.GRAY))
      UIManager.put("ScrollBar.width", 10)
      UIManager.put("ScrollBar.thumbHeight", 20) // SynthLookAndFeel(GTK, Nimbus)
      UIManager.put("ScrollBar.minimumThumbSize", Dimension(30, 30))
      UIManager.put("ScrollBar.incrementButtonGap", 0)
      UIManager.put("ScrollBar.decrementButtonGap", 0)
      UIManager.put("ScrollBar.thumb", THUMB)
      UIManager.put("ScrollBar.track", BACKGROUND)
      UIManager.put("ComboBox.foreground", FOREGROUND)
      UIManager.put("ComboBox.background", BACKGROUND)
      UIManager.put("ComboBox.selectionForeground", SELECTION_FOREGROUND)
      UIManager.put("ComboBox.selectionBackground", BACKGROUND)
      UIManager.put("ComboBox.buttonDarkShadow", BACKGROUND)
      UIManager.put("ComboBox.buttonBackground", FOREGROUND)
      UIManager.put("ComboBox.buttonHighlight", FOREGROUND)
      UIManager.put("ComboBox.buttonShadow", FOREGROUND)
      super.updateUI()
      setUI(object : BasicComboBoxUI() {
        override fun createArrowButton() = JButton(ArrowIcon(BACKGROUND, FOREGROUND)).also {
          it.isContentAreaFilled = false
          it.isFocusPainted = false
          it.border = BorderFactory.createEmptyBorder()
        }

        override fun createPopup() = object : BasicComboPopup(comboBox) {
          override fun createScroller(): JScrollPane {
            val sp = object : JScrollPane(list) {
              override fun updateUI() {
                super.updateUI()
                getVerticalScrollBar().setUI(WithoutArrowButtonScrollBarUI())
                getHorizontalScrollBar().setUI(WithoutArrowButtonScrollBarUI())
              }
            }
            sp.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
            sp.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            sp.horizontalScrollBar = null
            return sp
          }
        }
      })
      (getAccessibleContext().getAccessibleChild(0) as? JComponent)?.also {
        it.border = BorderFactory.createMatteBorder(0, 1, 1, 1, Color.GRAY)
        it.foreground = FOREGROUND
        it.background = BACKGROUND
      }
    }
  }
  p.add(combo2)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.border = BorderFactory.createEmptyBorder(16, 16, 16, 16)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeModel(): DefaultComboBoxModel<String> {
  val model = DefaultComboBoxModel<String>()
  model.addElement("333333")
  model.addElement("aaa")
  model.addElement("1234555")
  model.addElement("555555555555")
  model.addElement("666666")
  model.addElement("bbb")
  model.addElement("444444444")
  model.addElement("1234")
  model.addElement("000000000000000")
  model.addElement("2222222222")
  model.addElement("ccc")
  model.addElement("111111111111111111")
  return model
}

private class ArrowIcon(
  private val color: Color,
  private val rollover: Color,
) : Icon {
  override fun paintIcon(
    c: Component?,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.paint = color
    var shift = 0
    if (c is AbstractButton) {
      val m = c.model
      if (m.isPressed) {
        shift = 1
      } else {
        if (m.isRollover) {
          g2.paint = rollover
        }
      }
    }
    g2.translate(x, y + shift)
    g2.drawLine(2, 3, 6, 3)
    g2.drawLine(3, 4, 5, 4)
    g2.drawLine(4, 5, 4, 5)
    g2.dispose()
  }

  override fun getIconWidth() = 9

  override fun getIconHeight() = 9
}

private class ZeroSizeButton : JButton() {
  override fun getPreferredSize() = Dimension()
}

private class WithoutArrowButtonScrollBarUI : BasicScrollBarUI() {
  override fun createDecreaseButton(orientation: Int) = ZeroSizeButton()

  override fun createIncreaseButton(orientation: Int) = ZeroSizeButton()

  override fun paintTrack(
    g: Graphics,
    c: JComponent,
    r: Rectangle,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.paint = trackColor
    g2.fill(r)
    g2.dispose()
  }

  override fun paintThumb(
    g: Graphics,
    c: JComponent,
    r: Rectangle,
  ) {
    val sb = c as? JScrollBar
    if (sb?.isEnabled != true) {
      return
    }
    val m = sb.model
    if (m.maximum - m.minimum - m.extent > 0) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g2.paint = when {
        isDragging -> thumbDarkShadowColor.brighter()
        isThumbRollover -> thumbLightShadowColor.brighter()
        else -> thumbColor
      }
      g2.fillRect(r.x + 1, r.y + 1, r.width - 2, r.height - 2)
      g2.dispose()
    }
  }
}

fun main() {
  EventQueue.invokeLater {
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
