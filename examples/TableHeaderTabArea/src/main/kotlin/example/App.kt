package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.plaf.basic.BasicButtonUI
import javax.swing.plaf.basic.BasicHTML
import javax.swing.table.JTableHeader
import javax.swing.table.TableColumn
import javax.swing.table.TableColumnModel
import javax.swing.text.View

fun makeUI(): Component {
  val tabs = TableHeaderTabbedPane()
  tabs.border = BorderFactory.createTitledBorder("CardLayout+JTableHeader")
  tabs.addTab("111", JScrollPane(JTree()))
  tabs.addTab("222", JLabel("55555"))
  tabs.addTab("333", JLabel("66666"))
  tabs.addTab("444", JButton("77777"))
  return JPanel(BorderLayout()).also {
    it.add(tabs)
    it.preferredSize = Dimension(320, 240)
  }
}

private class TableHeaderTabbedPane : JPanel(BorderLayout()) {
  private val cardLayout = CardLayout()
  private val contentsPanel = JPanel(cardLayout)
  private val header: JTableHeader
  private val model: TableColumnModel
  private var selectedColumn: Any? = null
  private var rolloverColumn = -1

  init {
    val left = 1
    val right = 3
    val tabPanel = JPanel(GridLayout(1, 0, 0, 0))
    tabPanel.border = BorderFactory.createEmptyBorder(1, left, 0, right)
    contentsPanel.border = BorderFactory.createEmptyBorder(4, left, 2, right)

    val table = JTable(0, 0)
    header = table.tableHeader
    model = header.columnModel

    val handler = TableHeaderMouseInputHandler()
    header.addMouseListener(handler)
    header.addMouseMotionListener(handler)
    val l = TabButton()
    header.setDefaultRenderer { _, value, _, _, _, column ->
      l.also {
        it.text = value?.toString() ?: ""
        it.isSelected = value == selectedColumn || column == rolloverColumn
      }
    }

    val vp = object : JViewport() {
      override fun getPreferredSize() = Dimension()
    }
    vp.view = table

    val sp = JScrollPane()
    sp.viewport = vp

    add(sp, BorderLayout.NORTH)
    add(contentsPanel)
  }

  fun addTab(
    title: String,
    comp: Component,
  ) {
    contentsPanel.add(comp, title)
    val tc = TableColumn(model.columnCount, 75, header.defaultRenderer, null)
    tc.headerValue = title
    model.addColumn(tc)
    if (selectedColumn == null) {
      cardLayout.show(contentsPanel, title)
      selectedColumn = title
    }
  }

  private inner class TableHeaderMouseInputHandler : MouseAdapter() {
    override fun mousePressed(e: MouseEvent) {
      val h = e.component
      if (h is JTableHeader && h.columnAtPoint(e.point) >= 0) {
        val m = h.columnModel
        val title = m.getColumn(h.columnAtPoint(e.point)).headerValue
        cardLayout.show(contentsPanel, title.toString())
        selectedColumn = title
      }
    }

    override fun mouseEntered(e: MouseEvent) {
      updateRolloverColumn(e)
    }

    override fun mouseMoved(e: MouseEvent) {
      updateRolloverColumn(e)
    }

    override fun mouseDragged(e: MouseEvent) {
      rolloverColumn = -1
      updateRolloverColumn(e)
    }

    override fun mouseExited(e: MouseEvent) {
      rolloverColumn = -1
    }

    private fun updateRolloverColumn(e: MouseEvent) {
      val h = e.component as? JTableHeader ?: return
      if (h.draggedColumn == null && h.contains(e.point)) {
        val col = h.columnAtPoint(e.point)
        if (col != rolloverColumn) {
          rolloverColumn = col
        }
      }
    }
  }
}

private class TabButton : JRadioButton(null, null) {
  var textColor: Color? = null
  var pressedTc: Color? = null
  var rolloverTc: Color? = null
  var rolloverSelTc: Color? = null
  var selectedTc: Color? = null

  override fun updateUI() {
    if (UIManager.get(uiClassID) != null) {
      setUI(UIManager.getUI(this) as? TabViewButtonUI)
    } else {
      setUI(BasicTabViewButtonUI())
    }
  }

  override fun getUIClassID() = UI_CLASS_ID

  override fun getUI() = ui as? TabViewButtonUI

  override fun fireStateChanged() {
    val m = getModel()
    foreground = when {
      !m.isEnabled -> Color.GRAY
      m.isPressed && m.isArmed -> pressedTc
      m.isSelected -> selectedTc
      isRolloverEnabled && m.isRollover -> rolloverTc
      else -> textColor
    }
    super.fireStateChanged()
  }

  companion object {
    private const val UI_CLASS_ID = "TabViewButtonUI"
  }
}

private open class TabViewButtonUI : BasicButtonUI() {
  // ButtonUI
}

private class BasicTabViewButtonUI : TabViewButtonUI() {
  private val viewRect = Rectangle()
  private val iconRect = Rectangle()
  private val textRect = Rectangle()

  override fun installDefaults(b: AbstractButton) {
    super.installDefaults(b)
    b.preferredSize = Dimension(0, 24)
    b.isRolloverEnabled = true
    b.isOpaque = true
    val out = BorderFactory.createMatteBorder(2, 0, 0, 0, b.background)
    val inb = BorderFactory.createMatteBorder(1, 1, 0, 1, Color.RED)
    b.border = BorderFactory.createCompoundBorder(out, inb)
    if (b is TabButton) {
      b.textColor = Color(0x64_64_64)
      b.pressedTc = Color.GRAY
      b.rolloverTc = Color.BLACK
      b.rolloverSelTc = Color.GRAY
      b.selectedTc = Color.BLACK
    }
  }

  override fun paint(
    g: Graphics,
    c: JComponent,
  ) {
    val g2 = g.create()
    if (c !is AbstractButton || g2 !is Graphics2D) {
      return
    }
    SwingUtilities.calculateInnerArea(c, viewRect)
    iconRect.setBounds(0, 0, 0, 0)
    textRect.setBounds(0, 0, 0, 0)
    val text = SwingUtilities.layoutCompoundLabel(
      c,
      c.getFontMetrics(c.font),
      c.text,
      null,
      c.verticalAlignment,
      c.horizontalAlignment,
      c.verticalTextPosition,
      c.horizontalTextPosition,
      viewRect,
      iconRect,
      textRect,
      0,
    )
    g2.color = c.background
    g2.fillRect(0, 0, c.width, c.height)
    val m = c.model
    g2.color = if (m.isSelected || m.isArmed) Color.WHITE else Color(0xDC_DC_DC)
    val vx = viewRect.x
    val vy = viewRect.y
    val vw = viewRect.width
    g2.fill(viewRect)
    val color = Color(0xFF_78_28)
    if (m.isSelected) {
      g2.color = color
      g2.drawLine(vx + 1, vy - 2, vx + vw - 1, vy - 2)
      g2.color = color.brighter()
      g2.drawLine(vx, vy - 1, vx + vw, vy - 1)
      g2.color = color
      g2.drawLine(vx, vy, vx + vw, vy)
    } else if (m.isRollover) {
      g2.color = color
      g2.drawLine(vx + 1, vy, vx + vw - 1, vy)
      g2.color = color.brighter()
      g2.drawLine(vx, vy + 1, vx + vw, vy + 1)
      g2.color = color
      g2.drawLine(vx, vy + 2, vx + vw, vy + 2)
    }
    val p = c.getClientProperty(BasicHTML.propertyKey)
    (p as? View)?.paint(g2, textRect) ?: also {
      if (m.isSelected) {
        textRect.y -= 2
        textRect.x -= 1
      }
      textRect.x += 4
      paintText(g2, c, textRect, text)
    }
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
