package example

import java.awt.*
import java.awt.geom.Area
import javax.swing.*
import javax.swing.border.TitledBorder

fun makeUI(): Component {
  val p1 = object : JPanel() {
    override fun paintComponent(g: Graphics) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.paint = background
      g2.fill(SwingUtilities.calculateInnerArea(this, null))
      g2.dispose()
      super.paintComponent(g)
    }
  }
  val p2 = object : JPanel() {
    override fun paintComponent(g: Graphics) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.paint = background
      val area = Area(Rectangle(size))
      area.subtract(Area(SwingUtilities.calculateInnerArea(this, null)))
      g2.fill(area)
      g2.dispose()
      super.paintComponent(g)
    }
  }

  val p3 = JPanel(BorderLayout())
  p3.border = object : TitledBorder("Override paintBorder") {
    override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.paint = Color.WHITE
      val area = Area(Rectangle(x, y, width, 16))
      area.subtract(Area(SwingUtilities.calculateInnerArea(p3, null)))
      g2.fill(area)
      g2.dispose()
      super.paintBorder(c, g, x, y, width, height)
    }

    override fun isBorderOpaque() = false
  }

  val p = JPanel()
  val p4 = makeLabelTitledBorderPanel("OverlayLayout + JLabel", p)

  return JPanel(GridLayout(0, 1, 5, 5)).also {
    it.add(init(JPanel(), "Default TitledBorder"))
    it.add(init(p1, "Transparent TitledBorder"))
    it.add(init(p2, "Paint TitledBorder background"))
    it.add(p3)
    it.add(p4)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun init(c: JComponent, title: String): JComponent {
  c.background = Color.WHITE
  c.isOpaque = false
  c.border = BorderFactory.createTitledBorder(title)
  return c
}

fun makeLabelTitledBorderPanel(title: String, p: JPanel): JPanel {
  val label = JLabel(title, SwingConstants.LEADING)
  label.border = BorderFactory.createEmptyBorder(2, 4, 2, 4)
  label.isOpaque = true
  label.background = Color.WHITE
  label.alignmentX = Component.LEFT_ALIGNMENT
  label.alignmentY = Component.TOP_ALIGNMENT

  val box = Box.createHorizontalBox()
  box.add(Box.createHorizontalStrut(8))
  box.add(label)
  box.alignmentX = Component.LEFT_ALIGNMENT
  box.alignmentY = Component.TOP_ALIGNMENT

  val height = label.preferredSize.height / 2
  val color = Color(0x0, true)
  val b1 = BorderFactory.createMatteBorder(height, 2, 2, 2, color)
  val b2 = BorderFactory.createTitledBorder("")
  p.border = BorderFactory.createCompoundBorder(b1, b2)
  p.alignmentX = Component.LEFT_ALIGNMENT
  p.alignmentY = Component.TOP_ALIGNMENT

  val panel = JPanel()
  panel.layout = OverlayLayout(panel)
  panel.add(box)
  panel.add(p)
  return panel
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
