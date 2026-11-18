package example

import java.awt.*
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.awt.geom.Area
import java.awt.geom.Path2D
import java.awt.geom.RoundRectangle2D
import javax.swing.*

fun createUI(): Component {
  val model = createModel()
  val list1 = object : JList<String>(model) {
    override fun createToolTip(): JToolTip {
      val tip = BalloonToolTip()
      tip.component = this
      return tip
    }

    override fun updateUI() {
      super.updateUI()
      setCellRenderer(ToolTipListCellRenderer())
    }
  }
  val list2 = object : JList<String>(model) {
    override fun updateUI() {
      super.updateUI()
      setCellRenderer(ToolTipListCellRenderer())
    }
  }
  return JPanel(GridLayout(1, 2)).also {
    it.add(createTitledPanel("BalloonToolTip", list1))
    it.add(createTitledPanel("Default JToolTip", list2))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createModel(): DefaultListModel<String> {
  val model = DefaultListModel<String>()
  model.addElement("ABC DEF GHI JKL MNO PQR STU VWX YZ")
  model.addElement("111")
  model.addElement("111222")
  model.addElement("111222333")
  model.addElement("1234567890 abc def ghi jkl mno pqr stu vwx yz")
  model.addElement("bbb1")
  model.addElement("bbb12")
  model.addElement("1234567890-+*/=ABC DEF GHI JKL MNO PQR STU VWX YZ")
  model.addElement("bbb123")
  return model
}

private fun createTitledPanel(
  title: String,
  c: Component,
): Component {
  val scroll = JScrollPane(c)
  scroll.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(scroll)
  return p
}

private class ToolTipListCellRenderer<E> : ListCellRenderer<E> {
  private val renderer = DefaultListCellRenderer()

  override fun getListCellRendererComponent(
    list: JList<out E>,
    value: E?,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean,
  ): Component {
    val c = renderer.getListCellRendererComponent(
      list,
      value,
      index,
      isSelected,
      cellHasFocus,
    )
    if (c is JComponent) {
      val vp = SwingUtilities.getAncestorOfClass(JViewport::class.java, list)
      val rect = (vp as? JComponent)
        ?.let { SwingUtilities.calculateInnerArea(it, null) }
        ?: Rectangle()
      // Show the cell text as a tooltip only if it is clipped by the viewport
      val fm = c.getFontMetrics(c.font)
      c.toolTipText = value
        ?.toString()
        ?.takeIf { fm.stringWidth(it) > rect.width }
        ?: list.toolTipText
    }
    return c
  }
}

private class BalloonToolTip : JToolTip() {
  private var listener: HierarchyListener? = null

  override fun updateUI() {
    removeHierarchyListener(listener)
    super.updateUI()
    listener = HierarchyListener { e ->
      val c = e.component
      val f = e.changeFlags.toInt() and HierarchyEvent.SHOWING_CHANGED != 0
      if (f && c.isShowing) {
        SwingUtilities
          .getWindowAncestor(c)
          ?.takeIf { it.graphicsConfiguration?.isTranslucencyCapable == true }
          ?.takeIf { it.type == Window.Type.POPUP }
          ?.background = Color(0x0, true)
      }
    }
    addHierarchyListener(listener)
    isOpaque = false
    border = BorderFactory.createEmptyBorder(8, 5, 0, 5)
  }

  override fun getPreferredSize(): Dimension? = super.getPreferredSize()?.also {
    // Keep the default height, but grow it for larger fonts
    it.height = maxOf(it.height, 28)
  }

  override fun paintComponent(g: Graphics) {
    val s = createBalloonShape(4.0, 6.0)
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.color = background
    g2.fill(s)
    g2.color = foreground
    g2.draw(s)
    g2.dispose()
    super.paintComponent(g)
  }

  private fun createBalloonShape(triHeight: Double, arc: Double): Shape {
    // The tail (triangle) starts at the left inset and points upward
    val x = insets.left.toDouble()
    val triangle = Path2D.Double()
    triangle.moveTo(x + triHeight, triHeight)
    triangle.lineTo(x + triHeight * 2.0, 0.0)
    triangle.lineTo(x + triHeight * 3.0, triHeight)
    triangle.closePath()
    val w = width - 1.0
    val h = height - triHeight - 1.0
    val area = Area(RoundRectangle2D.Double(0.0, triHeight, w, h, arc, arc))
    area.add(Area(triangle))
    return area
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
