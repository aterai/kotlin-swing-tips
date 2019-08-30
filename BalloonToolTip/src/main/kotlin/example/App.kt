package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.awt.geom.Area
import java.awt.geom.RoundRectangle2D
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(GridLayout(1, 2)) {
  init {
    val model = DefaultListModel<String>()
    model.addElement("ABCDEFGHIJKLMNOPQRSTUVWXYZ")
    model.addElement("aaaa")
    model.addElement("aaaabbb")
    model.addElement("aaaabbbcc")
    model.addElement("1234567890abcdefghijklmnopqrstuvwxyz")
    model.addElement("bbb1")
    model.addElement("bbb12")
    model.addElement("1234567890-+*/=ABCDEFGHIJKLMNOPQRSTUVWXYZ")
    model.addElement("bbb123")

    val list1 = object : JList<String>(model) {
      override fun createToolTip(): JToolTip {
        val tip = BalloonToolTip()
        tip.setComponent(this)
        return tip
      }

      override fun updateUI() {
        super.updateUI()
        setCellRenderer(TooltipListCellRenderer<Any>())
      }
    }

    val list2 = object : JList<String>(model) {
      override fun updateUI() {
        super.updateUI()
        setCellRenderer(TooltipListCellRenderer<Any>())
      }
    }

    add(makeTitledPanel("BalloonToolTip", list1))
    add(makeTitledPanel("Default JToolTip", list2))
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeTitledPanel(title: String, c: Component): Component {
    val scroll = JScrollPane(c)
    scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
    val p = JPanel(BorderLayout())
    p.setBorder(BorderFactory.createTitledBorder(title))
    p.add(scroll)
    return p
  }
}

class TooltipListCellRenderer<E> : ListCellRenderer<E> {
  private val renderer = DefaultListCellRenderer()

  override fun getListCellRendererComponent(
    list: JList<out E>,
    value: E?,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean
  ): Component {
    val c = renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
    val l = c as? JLabel ?: return c
    val i = l.getInsets()
    val v = SwingUtilities.getAncestorOfClass(JViewport::class.java, list)
    val rect = v.getBounds()
    rect.width -= i.left + i.right
    val fm = l.getFontMetrics(l.getFont())
    val str = value?.toString() ?: ""
    l.setToolTipText(if (fm.stringWidth(str) > rect.width) str else null)
    return l
  }
}

class BalloonToolTip : JToolTip() {
  private var listener: HierarchyListener? = null

  override fun updateUI() {
    removeHierarchyListener(listener)
    super.updateUI()
    listener = HierarchyListener { e ->
      val c = e.getComponent()
      if (e.getChangeFlags().toInt() and HierarchyEvent.SHOWING_CHANGED != 0 && c.isShowing()) {
        // (SwingUtilities.getRoot(c) as? JWindow)?.also {
        //   println("Popup\$HeavyWeightWindow")
        //   it.setBackground(Color(0x0, true))
        // }
        (SwingUtilities.getRoot(c) as? JWindow)?.setBackground(Color(0x0, true))
      }
    }
    addHierarchyListener(listener)
    setOpaque(false)
    setBorder(BorderFactory.createEmptyBorder(8, 5, 0, 5))
    // setForeground(Color.WHITE)
    // setBackground(Color(0xEF_64_64_64.toInt(), true))
  }

  override fun getPreferredSize() = super.getPreferredSize()?.also { it.height = 28 }

  override fun paintComponent(g: Graphics) {
    val s = makeBalloonShape()
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.setColor(getBackground())
    g2.fill(s)
    g2.setColor(getForeground())
    g2.draw(s)
    g2.dispose()
    super.paintComponent(g)
  }

  private fun makeBalloonShape(): Shape {
    val i = getInsets()
    val w = getWidth() - 1
    val h = getHeight() - 1
    val v = i.top / 2
    val triangle = Polygon()
    triangle.addPoint(i.left + v + v, 0)
    triangle.addPoint(i.left + v, v)
    triangle.addPoint(i.left + v + v + v, v)
    val area = Area(RoundRectangle2D.Float(
      0f, v.toFloat(),
      w.toFloat(), (h - i.bottom - v).toFloat(),
      i.top.toFloat(), i.top.toFloat()))
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
