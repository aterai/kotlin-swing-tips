package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.BasicHTML
import javax.swing.plaf.basic.BasicRadioButtonUI
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter
import javax.swing.text.View

private const val LR_PAGE_SIZE = 5

private val box = Box.createHorizontalBox()
private val group = ButtonGroup()
private val columnNames = arrayOf("Year", "String", "Comment")
private val model = object : DefaultTableModel(null, columnNames) {
  override fun getColumnClass(column: Int) = if (column == 0) {
    Number::class.java
  } else {
    Any::class.java
  }
}

@Transient
private val sorter = TableRowSorter<DefaultTableModel>(model)
private val table = JTable(model)
private var linkViewRadioButtonUI: LinkViewRadioButtonUI? = null

fun makeUI(): Component {
  table.fillsViewportHeight = true
  table.intercellSpacing = Dimension()
  table.setShowGrid(false)
  table.putClientProperty("terminateEditOnFocusLost", true)
  table.rowSorter = sorter

  (1..2019).map { i -> arrayOf(i, "Test: $i", if (i % 2 == 0) "" else "comment...") }
    .forEach { model.addRow(it) }

  initLinkBox(100, 1)
  box.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun initLinkBox(itemsPerPage: Int, currentPageIndex: Int) {
  sorter.rowFilter = object : RowFilter<TableModel, Int>() {
    override fun include(entry: Entry<out TableModel, out Int>): Boolean {
      val ti = currentPageIndex - 1
      val ei = entry.identifier.toInt()
      return ti * itemsPerPage <= ei && ei < ti * itemsPerPage + itemsPerPage
    }
  }

  var startPageIndex = currentPageIndex - LR_PAGE_SIZE
  if (startPageIndex <= 0) {
    startPageIndex = 1
  }

  val rowCount = model.rowCount
  val v = if (rowCount % itemsPerPage == 0) 0 else 1
  val maxPageIndex = rowCount / itemsPerPage + v
  var endPageIndex = currentPageIndex + LR_PAGE_SIZE - 1
  if (endPageIndex > maxPageIndex) {
    endPageIndex = maxPageIndex
  }
  if (startPageIndex < endPageIndex) {
    initLinkBoxLayout(itemsPerPage, currentPageIndex, maxPageIndex, startPageIndex, endPageIndex)
    box.revalidate()
    box.repaint()
  }
}

private fun initLinkBoxLayout(
  itemsPerPage: Int,
  currentPageIndex: Int,
  maxPageIndex: Int,
  startPageIndex: Int,
  endPageIndex: Int
) {
  box.removeAll()
  group.elements.toList().forEach { group.remove(it) }

  val flg1 = currentPageIndex > 1
  addButton(makePrevNextButton(itemsPerPage, 1, "|<", flg1))
  addButton(makePrevNextButton(itemsPerPage, currentPageIndex - 1, "<", flg1))

  box.add(Box.createHorizontalGlue())
  for (i in startPageIndex..endPageIndex) {
    addButton(makeRadioButton(itemsPerPage, currentPageIndex, i))
  }
  box.add(Box.createHorizontalGlue())

  val flg2 = currentPageIndex < maxPageIndex
  addButton(makePrevNextButton(itemsPerPage, currentPageIndex + 1, ">", flg2))
  addButton(makePrevNextButton(itemsPerPage, maxPageIndex, ">|", flg2))
}

private fun addButton(button: AbstractButton) {
  box.add(button)
  group.add(button)
}

private fun makeRadioButton(itemsPerPage: Int, current: Int, target: Int): JRadioButton {
  val radio = object : JRadioButton(target.toString()) {
    override fun fireStateChanged() {
      val bm = getModel()
      if (bm.isEnabled) {
        if (bm.isPressed && bm.isArmed) {
          foreground = Color.GREEN
        } else if (bm.isSelected) {
          foreground = Color.RED
        }
      } else {
        foreground = Color.GRAY
      }
      super.fireStateChanged()
    }
  }
  radio.foreground = Color.BLUE
  linkViewRadioButtonUI = LinkViewRadioButtonUI()
  radio.ui = linkViewRadioButtonUI
  if (target == current) {
    radio.isSelected = true
  }
  radio.addActionListener { initLinkBox(itemsPerPage, target) }
  return radio
}

private fun makePrevNextButton(
  itemsPerPage: Int,
  target: Int,
  title: String,
  flag: Boolean
) = JRadioButton(title).also {
  it.foreground = Color.BLUE
  it.ui = linkViewRadioButtonUI
  it.isEnabled = flag
  it.addActionListener { initLinkBox(itemsPerPage, target) }
}

private class LinkViewRadioButtonUI : BasicRadioButtonUI() {
  private val viewRect = Rectangle()
  private val iconRect = Rectangle()
  private val textRect = Rectangle()

  override fun getDefaultIcon() = null

  @Synchronized
  override fun paint(g: Graphics, c: JComponent) {
    g.font = c.font

    SwingUtilities.calculateInnerArea(c, viewRect)
    iconRect.setBounds(0, 0, 0, 0)
    textRect.setBounds(0, 0, 0, 0)

    if (c.isOpaque) {
      g.color = c.background
      g.fillRect(0, 0, c.width, c.height)
    }

    val b = c as? AbstractButton ?: return
    val text = SwingUtilities.layoutCompoundLabel(
      b, c.getFontMetrics(c.font), b.text, null, b.verticalAlignment,
      b.horizontalAlignment, b.verticalTextPosition, b.horizontalTextPosition,
      viewRect, iconRect, textRect, 0
    )

    val m = b.model
    g.color = b.foreground
    val isRollover = b.isRolloverEnabled && m.isRollover
    val isNotAimed = !m.isSelected && !m.isPressed && !m.isArmed
    if (isNotAimed && isRollover) {
      g.drawLine(
        viewRect.x,
        viewRect.y + viewRect.height,
        viewRect.x + viewRect.width,
        viewRect.y + viewRect.height
      )
    }
    (b.getClientProperty(BasicHTML.propertyKey) as? View)?.paint(g, textRect)
      ?: paintText(g, b, textRect, text)
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
