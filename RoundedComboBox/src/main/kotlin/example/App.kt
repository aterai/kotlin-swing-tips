package example

import com.sun.java.swing.plaf.windows.WindowsComboBoxUI
import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.Area
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.border.AbstractBorder
import javax.swing.plaf.basic.BasicComboBoxUI
import javax.swing.plaf.metal.MetalComboBoxUI

class MainPanel : JPanel(BorderLayout()) {
  init {
    val combo0 = JComboBox(makeModel())
    val combo1 = JComboBox(makeModel())
    val combo2 = JComboBox(makeModel())

    combo0.setBorder(RoundedCornerBorder())
    combo1.setBorder(KamabokoBorder())
    combo2.setBorder(KamabokoBorder())
    if (combo2.getUI() is WindowsComboBoxUI) {
      combo2.setUI(object : WindowsComboBoxUI() {
        override fun createArrowButton(): JButton {
          val b = JButton(ArrowIcon(Color.BLACK, Color.BLUE)) // .createArrowButton();
          b.setContentAreaFilled(false)
          b.setFocusPainted(false)
          b.setBorder(BorderFactory.createEmptyBorder())
          return b
        }
      })
    }

    val box0 = Box.createVerticalBox()
    box0.add(makeTitledPanel("RoundRectangle2D:", combo0, null))
    box0.add(Box.createVerticalStrut(5))
    box0.add(makeTitledPanel("Path2D:", combo1, null))
    box0.add(Box.createVerticalStrut(5))
    box0.add(makeTitledPanel("WindowsComboBoxUI#createArrowButton():", combo2, null))
    box0.add(Box.createVerticalStrut(5))
    box0.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))

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

    // UIManager.put("ComboBox.border", BorderFactory.createLineBorder(Color.WHITE));
    // UIManager.put("ComboBox.editorBorder", BorderFactory.createLineBorder(Color.GREEN));
    UIManager.put("ComboBox.border", KamabokoBorder())

    val combo00 = JComboBox(makeModel())
    val combo01 = JComboBox(makeModel())

    UIManager.put("ComboBox.border", KamabokoBorder())
    val combo02 = JComboBox(makeModel())

    combo00.setUI(MetalComboBoxUI())
    combo01.setUI(BasicComboBoxUI())
    combo02.setUI(object : BasicComboBoxUI() {
      override fun createArrowButton(): JButton {
        val b = JButton(ArrowIcon(BACKGROUND, FOREGROUND))
        b.setContentAreaFilled(false)
        b.setFocusPainted(false)
        b.setBorder(BorderFactory.createEmptyBorder())
        return b
      }
    })

    combo02.addMouseListener(ComboRolloverHandler())

    (combo00.getAccessibleContext().getAccessibleChild(0) as? JComponent)
      ?.setBorder(BorderFactory.createMatteBorder(0, 1, 1, 1, FOREGROUND))
    (combo01.getAccessibleContext().getAccessibleChild(0) as? JComponent)
      ?.setBorder(BorderFactory.createMatteBorder(0, 1, 1, 1, FOREGROUND))
    (combo02.getAccessibleContext().getAccessibleChild(0) as? JComponent)
      ?.setBorder(BorderFactory.createMatteBorder(0, 1, 1, 1, FOREGROUND))

    val box1 = Box.createVerticalBox()
    box1.add(makeTitledPanel("MetalComboBoxUI:", combo00, BACKGROUND))
    box1.add(Box.createVerticalStrut(10))
    box1.add(makeTitledPanel("BasicComboBoxUI:", combo01, BACKGROUND))
    box1.add(Box.createVerticalStrut(10))
    box1.add(makeTitledPanel("BasicComboBoxUI#createArrowButton():", combo02, BACKGROUND))
    box1.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))

    val tabbedPane = JTabbedPane()
    tabbedPane.addTab("Basic, Metal", makeTitledPanel(null, box1, BACKGROUND))
    tabbedPane.addTab("Windows", makeTitledPanel(null, box0, null))

    val check = JCheckBox("editable")
    check.addActionListener { e ->
      val f = (e.getSource() as? JCheckBox)?.isSelected() ?: false
      listOf(combo00, combo01, combo02, combo0, combo1, combo2).forEach { it.setEditable(f) }
      repaint()
    }

    add(tabbedPane)
    add(check, BorderLayout.SOUTH)
    // setOpaque(true);
    // setBackground(BACKGROUND);
    setPreferredSize(Dimension(320, 240))
  }

  companion object {
    val BACKGROUND: Color = Color.BLACK // RED;
    val FOREGROUND: Color = Color.WHITE // YELLOW;
    val SELECTION_FOREGROUND: Color = Color.CYAN

    private fun makeTitledPanel(title: String?, cmp: Container, bgc: Color?): Component {
      val p = JPanel(BorderLayout())
      if (cmp.getLayout() is BoxLayout) {
        p.add(cmp, BorderLayout.NORTH)
      } else {
        p.add(cmp)
      }
      if (title != null) {
        val b = BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), title)
        if (bgc != null) {
          b.setTitleColor(Color(bgc.getRGB().inv()))
        }
        p.setBorder(b)
      }
      if (bgc != null) {
        p.setOpaque(true)
        p.setBackground(bgc)
      }
      return p
    }

    private fun makeModel() = DefaultComboBoxModel<String>().also {
      it.addElement("1234")
      it.addElement("5555555555555555555555")
      it.addElement("6789000000000")
    }
  }
}

class ComboRolloverHandler : MouseAdapter() {
  private fun getButtonModel(e: MouseEvent) =
    ((e.getComponent() as? Container)?.getComponent(0) as? JButton)?.getModel()

  override fun mouseEntered(e: MouseEvent) {
    getButtonModel(e)?.setRollover(true)
  }

  override fun mouseExited(e: MouseEvent) {
    getButtonModel(e)?.setRollover(false)
  }

  override fun mousePressed(e: MouseEvent) {
    getButtonModel(e)?.setPressed(true)
  }

  override fun mouseReleased(e: MouseEvent) {
    getButtonModel(e)?.setPressed(false)
  }
}

class ArrowIcon(private val color: Color, private val rollover: Color) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as Graphics2D
    g2.setPaint(color)
    var shift = 0
    (c as? AbstractButton)?.also {
      val m = it.getModel()
      if (m.isPressed()) {
        shift = 1
      } else {
        if (m.isRollover()) {
          g2.setPaint(rollover)
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

open class RoundedCornerBorder : AbstractBorder() {
  override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    val r = 12.0
    val dx = x.toDouble()
    val dy = y.toDouble()
    val dw = width.toDouble() - 1.0
    val dh = height.toDouble() - 1.0

    val round = Area(RoundRectangle2D.Double(dx, dy, dw, dh, r, r))

    c.getParent()?.also {
      g2.setPaint(it.getBackground())
      val corner = Area(Rectangle2D.Double(dx, dy, width.toDouble(), height.toDouble()))
      corner.subtract(round)
      g2.fill(corner)
    }
    g2.setPaint(c.getForeground())
    g2.draw(round)
    g2.dispose()
  }

  override fun getBorderInsets(c: Component) = Insets(4, 8, 4, 8)

  override fun getBorderInsets(c: Component?, insets: Insets): Insets {
    insets.set(4, 8, 4, 8)
    return insets
  }
}

class KamabokoBorder : RoundedCornerBorder() {
  override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    val r = 12.0
    val dx = x.toDouble()
    val dy = y.toDouble()
    val dw = width.toDouble() - 1.0
    val dh = height.toDouble() - 1.0

    val p = Path2D.Double()
    p.moveTo(dx, dy + dh)
    p.lineTo(dx, dy + r)
    p.quadTo(dx, dy, dx + r, dy)
    p.lineTo(dx + dw - r, dy)
    p.quadTo(dx + dw, dy, dx + dw, dy + r)
    p.lineTo(dx + dw, dy + dh)
    p.closePath()
    val round = Area(p)
    val parent = c.getParent()
    if (parent != null) {
      g2.setPaint(parent.getBackground())
      val corner = Area(Rectangle2D.Double(dx, dy, width.toDouble(), height.toDouble()))
      corner.subtract(round)
      g2.fill(corner)
    }
    g2.setPaint(c.getForeground())
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
