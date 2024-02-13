package example

import com.sun.java.swing.plaf.windows.WindowsComboBoxUI
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.Area
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import javax.swing.*
import javax.swing.border.AbstractBorder
import javax.swing.plaf.basic.BasicComboBoxUI
import javax.swing.plaf.metal.MetalComboBoxUI
import kotlin.math.sqrt

private val BACKGROUND = Color.BLACK // RED
private val FOREGROUND = Color.WHITE // YELLOW
private val SELECTION_FOREGROUND = Color.CYAN

fun makeUI(): Component {
  val combo0 = JComboBox(makeModel())
  val combo1 = JComboBox(makeModel())
  val combo2 = JComboBox(makeModel())
  val box0 = makeBox0(combo0, combo1, combo2)

  // UIManager.put("TitledBorder.titleColor", FOREGROUND)
  // UIManager.put("TitledBorder.border", BorderFactory.createEmptyBorder())

  UIManager.put("ComboBox.foreground", FOREGROUND)
  UIManager.put("ComboBox.background", BACKGROUND)
  UIManager.put("ComboBox.selectionForeground", SELECTION_FOREGROUND)
  UIManager.put("ComboBox.selectionBackground", BACKGROUND)

  UIManager.put("ComboBox.buttonDarkShadow", BACKGROUND)
  UIManager.put("ComboBox.buttonBackground", FOREGROUND)
  UIManager.put("ComboBox.buttonHighlight", FOREGROUND)
  UIManager.put("ComboBox.buttonShadow", FOREGROUND)

  // UIManager.put("ComboBox.border", BorderFactory.createLineBorder(Color.WHITE))
  // UIManager.put("ComboBox.editorBorder", BorderFactory.createLineBorder(Color.GREEN))
  UIManager.put("ComboBox.border", KamabokoBorder())

  val combo00 = JComboBox(makeModel())
  val combo01 = JComboBox(makeModel())

  UIManager.put("ComboBox.border", KamabokoBorder())
  val combo02 = JComboBox(makeModel())
  val box1 = makeBox1(combo00, combo01, combo02)

  val tabbedPane = JTabbedPane()
  tabbedPane.addTab("Basic, Metal", makeTitledPanel(null, box1, BACKGROUND))
  tabbedPane.addTab("Windows", makeTitledPanel(null, box0, null))

  val check = JCheckBox("editable")
  check.addActionListener { e ->
    val f = (e.source as? JCheckBox)?.isSelected ?: false
    listOf(combo00, combo01, combo02, combo0, combo1, combo2).forEach { it.setEditable(f) }
    tabbedPane.rootPane.repaint()
  }

  return JPanel(BorderLayout()).also {
    it.add(tabbedPane)
    it.add(check, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeBox0(
  combo0: JComboBox<String>,
  combo1: JComboBox<String>,
  combo2: JComboBox<String>,
) = Box.createVerticalBox().also {
  combo0.border = RoundedCornerBorder()
  combo1.border = KamabokoBorder()
  combo2.border = KamabokoBorder()
  if (combo2.ui is WindowsComboBoxUI) {
    combo2.setUI(object : WindowsComboBoxUI() {
      override fun createArrowButton(): JButton {
        val b = JButton(ArrowIcon(Color.BLACK, Color.BLUE)) // .createArrowButton()
        b.isContentAreaFilled = false
        b.isFocusPainted = false
        b.border = BorderFactory.createEmptyBorder()
        return b
      }
    })
  }
  it.add(makeTitledPanel("RoundRectangle2D:", combo0, null))
  it.add(Box.createVerticalStrut(5))
  it.add(makeTitledPanel("Path2D:", combo1, null))
  it.add(Box.createVerticalStrut(5))
  it.add(makeTitledPanel("WindowsComboBoxUI#createArrowButton():", combo2, null))
  it.add(Box.createVerticalStrut(5))
  it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
}

private fun makeBox1(
  combo00: JComboBox<String>,
  combo01: JComboBox<String>,
  combo02: JComboBox<String>,
) = Box.createVerticalBox().also {
  combo00.setUI(MetalComboBoxUI())
  combo01.setUI(BasicComboBoxUI())
  combo02.setUI(object : BasicComboBoxUI() {
    override fun createArrowButton(): JButton {
      val b = JButton(ArrowIcon(BACKGROUND, FOREGROUND))
      b.isContentAreaFilled = false
      b.isFocusPainted = false
      b.border = BorderFactory.createEmptyBorder()
      return b
    }
  })

  combo02.addMouseListener(ComboRolloverHandler())

  (combo00.accessibleContext.getAccessibleChild(0) as? JComponent)?.border =
    BorderFactory.createMatteBorder(0, 1, 1, 1, FOREGROUND)
  (combo01.accessibleContext.getAccessibleChild(0) as? JComponent)?.border =
    BorderFactory.createMatteBorder(0, 1, 1, 1, FOREGROUND)
  (combo02.accessibleContext.getAccessibleChild(0) as? JComponent)?.border =
    BorderFactory.createMatteBorder(0, 1, 1, 1, FOREGROUND)

  it.add(makeTitledPanel("MetalComboBoxUI:", combo00, BACKGROUND))
  it.add(Box.createVerticalStrut(10))
  it.add(makeTitledPanel("BasicComboBoxUI:", combo01, BACKGROUND))
  it.add(Box.createVerticalStrut(10))
  it.add(makeTitledPanel("BasicComboBoxUI#createArrowButton():", combo02, BACKGROUND))
  it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
}

private fun makeTitledPanel(
  title: String?,
  cmp: Container,
  bgc: Color?,
): Component {
  val p = JPanel(BorderLayout())
  if (cmp.layout is BoxLayout) {
    p.add(cmp, BorderLayout.NORTH)
  } else {
    p.add(cmp)
  }
  if (title != null) {
    val b = BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), title)
    if (bgc != null) {
      b.titleColor = Color(bgc.rgb.inv())
    }
    p.border = b
  }
  if (bgc != null) {
    p.isOpaque = true
    p.background = bgc
  }
  return p
}

private fun makeModel() = DefaultComboBoxModel<String>().also {
  it.addElement("1234")
  it.addElement("5555555555555555555555")
  it.addElement("6789000000000")
}

private class ComboRolloverHandler : MouseAdapter() {
  private fun getButtonModel(e: MouseEvent) =
    ((e.component as? Container)?.getComponent(0) as? JButton)?.model

  override fun mouseEntered(e: MouseEvent) {
    getButtonModel(e)?.isRollover = true
  }

  override fun mouseExited(e: MouseEvent) {
    getButtonModel(e)?.isRollover = false
  }

  override fun mousePressed(e: MouseEvent) {
    getButtonModel(e)?.isPressed = true
  }

  override fun mouseReleased(e: MouseEvent) {
    getButtonModel(e)?.isPressed = false
  }
}

private class ArrowIcon(private val color: Color, private val rollover: Color) : Icon {
  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.paint = color
    var shift = 0
    (c as? AbstractButton)?.also {
      val m = it.model
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

private open class RoundedCornerBorder : AbstractBorder() {
  override fun paintBorder(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    val r = 6 * 2.0
    val dx = x.toDouble()
    val dy = y.toDouble()
    val dw = width.toDouble() - 1.0
    val dh = height.toDouble() - 1.0

    val round = Area(RoundRectangle2D.Double(dx, dy, dw, dh, r, r))

    c.parent?.also {
      g2.paint = it.background
      val corner = Area(Rectangle2D.Double(dx, dy, width.toDouble(), height.toDouble()))
      corner.subtract(round)
      g2.fill(corner)
    }
    g2.paint = c.foreground
    g2.draw(round)
    g2.dispose()
  }

  override fun getBorderInsets(c: Component) = Insets(4, 8, 4, 8)

  override fun getBorderInsets(
    c: Component?,
    insets: Insets,
  ): Insets {
    insets.set(4, 8, 4, 8)
    return insets
  }
}

private class KamabokoBorder : RoundedCornerBorder() {
  override fun paintBorder(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    val r = 6.0
    val rr = r * 4.0 * (sqrt(2.0) - 1.0) / 3.0
    val dx = x.toDouble()
    val dy = y.toDouble()
    val dw = width.toDouble() - 1.0
    val dh = height.toDouble() - 1.0

    val p = Path2D.Double()
    p.moveTo(dx, dy + dh)
    p.lineTo(dx, dy + r)
    p.curveTo(dx, dy + r - rr, dx + r - rr, dy, dx + r, dy)
    p.lineTo(dx + dw - r, dy)
    p.curveTo(dx + dw - r + rr, dy, dx + dw, dy + r - rr, dx + dw, dy + r)
    p.lineTo(dx + dw, dy + dh)
    p.closePath()
    val round = Area(p)
    val parent = c.parent
    if (parent != null) {
      g2.paint = parent.background
      val corner = Area(Rectangle2D.Double(dx, dy, width.toDouble(), height.toDouble()))
      corner.subtract(round)
      g2.fill(corner)
    }
    g2.paint = c.foreground
    g2.draw(round)
    g2.dispose()
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
