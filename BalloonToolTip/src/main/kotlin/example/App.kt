package example

import java.awt.*
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.awt.geom.Area
import java.awt.geom.RoundRectangle2D
import javax.swing.*

fun makeUI(): Component {
  val model = makeModel()
  val list1 = makeList1(model)
  val list2 = makeList2(model)
  return JPanel(GridLayout(1, 2)).also {
    it.add(makeTitledPanel("BalloonToolTip", list1))
    it.add(makeTitledPanel("Default JToolTip", list2))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeModel(): DefaultListModel<String> {
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

private fun makeList1(model: DefaultListModel<String>) = object : JList<String>(model) {
  override fun createToolTip(): JToolTip {
    val tip = BalloonToolTip()
    tip.component = this
    return tip
  }

  override fun updateUI() {
    cellRenderer = null
    super.updateUI()
    val r = DefaultListCellRenderer()
    setCellRenderer { list, value, index, isSelected, cellHasFocus ->
      r.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus).also {
        val vp = SwingUtilities.getAncestorOfClass(JViewport::class.java, list)
        if (vp is JViewport) {
          val rect = SwingUtilities.calculateInnerArea(vp, vp.bounds)
          val fm = it.getFontMetrics(it.font)
          val str = value?.toString() ?: ""
          val b = fm.stringWidth(str) > rect.width
          (it as? JComponent)?.toolTipText = if (b) str else null
        }
      }
    }
  }
}

private fun makeList2(model: DefaultListModel<String>) = object : JList<String>(model) {
  override fun updateUI() {
    cellRenderer = null
    super.updateUI()
    val r = DefaultListCellRenderer()
    setCellRenderer { list, value, index, isSelected, cellHasFocus ->
      r.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus).also {
        val vp = SwingUtilities.getAncestorOfClass(JViewport::class.java, list)
        if (vp is JViewport) {
          val rect = SwingUtilities.calculateInnerArea(vp, vp.bounds)
          val fm = it.getFontMetrics(it.font)
          val str = value?.toString() ?: ""
          val b = fm.stringWidth(str) > rect.width
          (it as? JComponent)?.toolTipText = if (b) str else list.toolTipText
        }
      }
    }
  }
}

private fun makeTitledPanel(
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

private class BalloonToolTip : JToolTip() {
  private var listener: HierarchyListener? = null

  override fun updateUI() {
    removeHierarchyListener(listener)
    super.updateUI()
    listener = HierarchyListener { e ->
      val c = e.component
      if (e.changeFlags.toInt() and HierarchyEvent.SHOWING_CHANGED != 0 && c.isShowing) {
        SwingUtilities.getWindowAncestor(c)
          ?.takeIf { it.type == Window.Type.POPUP }
          ?.background = Color(0x0, true)
      }
    }
    addHierarchyListener(listener)
    isOpaque = false
    border = BorderFactory.createEmptyBorder(8, 5, 0, 5)
    // setForeground(Color.WHITE)
    // setBackground(Color(0xEF_64_64_64.toInt(), true))
  }

  override fun getPreferredSize() = super.getPreferredSize()?.also { it.height = 28 }

  override fun paintComponent(g: Graphics) {
    val s = makeBalloonShape()
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.color = background
    g2.fill(s)
    g2.color = foreground
    g2.draw(s)
    g2.dispose()
    super.paintComponent(g)
  }

  private fun makeBalloonShape(): Shape {
    val i = insets
    val w = width - 1
    val h = height - 1
    val v = i.top / 2
    val triangle = Polygon()
    triangle.addPoint(i.left + v + v, 0)
    triangle.addPoint(i.left + v, v)
    triangle.addPoint(i.left + v + v + v, v)
    val area = Area(
      RoundRectangle2D.Float(
        0f,
        v.toFloat(),
        w.toFloat(),
        (h - i.bottom - v).toFloat(),
        i.top.toFloat(),
        i.top.toFloat(),
      ),
    )
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
