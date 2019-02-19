package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.BasicHTML
import javax.swing.plaf.basic.BasicRadioButtonUI
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter
import javax.swing.text.View

class MainPanel : JPanel(BorderLayout()) {
  private val box = Box.createHorizontalBox()
  private val columnNames = arrayOf("Year", "String", "Comment")
  private val model = object : DefaultTableModel(null, columnNames) {
    override fun getColumnClass(column: Int) = if (column == 0) java.lang.Integer::class.java else Any::class.java
  }
  @Transient
  private val sorter = TableRowSorter<DefaultTableModel>(model)
  private val table = JTable(model)
  private var linkViewRadioButtonUI: LinkViewRadioButtonUI? = null

  init {
    table.setFillsViewportHeight(true)
    table.setIntercellSpacing(Dimension())
    table.setShowGrid(false)
    table.putClientProperty("terminateEditOnFocusLost", java.lang.Boolean.TRUE)
    table.setRowSorter(sorter)

    (1..2016).map { i -> arrayOf<Any>(i, "Test: $i", if (i % 2 == 0) "" else "comment...") }
        .forEach { model.addRow(it) }

    initLinkBox(100, 1)
    box.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2))
    add(box, BorderLayout.NORTH)
    add(JScrollPane(table))
    setPreferredSize(Dimension(320, 240))
  }

  private fun initLinkBox(itemsPerPage: Int, currentPageIndex: Int) {
    // assert currentPageIndex > 0;
    sorter.setRowFilter(object : RowFilter<TableModel, Int>() {
      override fun include(entry: RowFilter.Entry<out TableModel, out Int>): Boolean {
        val ti = currentPageIndex - 1
        val ei = entry.getIdentifier().toInt()
        return ti * itemsPerPage <= ei && ei < ti * itemsPerPage + itemsPerPage
      }
    })

    var startPageIndex = currentPageIndex - LR_PAGE_SIZE
    if (startPageIndex <= 0) {
      startPageIndex = 1
    }

    val rowCount = model.getRowCount()
    val v = if (rowCount % itemsPerPage == 0) 0 else 1
    val maxPageIndex = rowCount / itemsPerPage + v
    var endPageIndex = currentPageIndex + LR_PAGE_SIZE - 1
    if (endPageIndex > maxPageIndex) {
      endPageIndex = maxPageIndex
    }
    if (startPageIndex < endPageIndex) {
      initLinkBoxLayout(itemsPerPage, currentPageIndex, maxPageIndex, startPageIndex, endPageIndex)
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

    val bg = ButtonGroup()
    val f = makePrevNextRadioButton(itemsPerPage, 1, "|<", currentPageIndex > 1)
    box.add(f)
    bg.add(f)

    val p = makePrevNextRadioButton(itemsPerPage, currentPageIndex - 1, "<", currentPageIndex > 1)
    box.add(p)
    bg.add(p)

    box.add(Box.createHorizontalGlue())
    for (i in startPageIndex..endPageIndex) {
      val c = makeRadioButton(itemsPerPage, currentPageIndex, i)
      box.add(c)
      bg.add(c)
    }
    box.add(Box.createHorizontalGlue())

    val n = makePrevNextRadioButton(itemsPerPage, currentPageIndex + 1, ">", currentPageIndex < maxPageIndex)
    box.add(n)
    bg.add(n)

    val l = makePrevNextRadioButton(itemsPerPage, maxPageIndex, ">|", currentPageIndex < maxPageIndex)
    box.add(l)
    bg.add(l)

    box.revalidate()
    box.repaint()
  }

  private fun makeRadioButton(itemsPerPage: Int, current: Int, target: Int): JRadioButton {
    val radio = object : JRadioButton(target.toString()) {
      protected override fun fireStateChanged() {
        val bm = getModel()
        if (bm.isEnabled()) {
          if (bm.isPressed() && bm.isArmed()) {
            setForeground(Color.GREEN)
          } else if (bm.isSelected()) {
            setForeground(Color.RED)
          }
        } else {
          setForeground(Color.GRAY)
        }
        super.fireStateChanged()
      }
    }
    radio.setForeground(Color.BLUE)
    linkViewRadioButtonUI = LinkViewRadioButtonUI()
    radio.setUI(linkViewRadioButtonUI)
    if (target == current) {
      radio.setSelected(true)
    }
    radio.addActionListener { initLinkBox(itemsPerPage, target) }
    return radio
  }

  private fun makePrevNextRadioButton(itemsPerPage: Int, target: Int, title: String, flag: Boolean) =
    JRadioButton(title).apply {
      setForeground(Color.BLUE)
      setUI(linkViewRadioButtonUI)
      setEnabled(flag)
      addActionListener { initLinkBox(itemsPerPage, target) }
    }

  companion object {
    private const val LR_PAGE_SIZE = 5
  }
}

internal class LinkViewRadioButtonUI : BasicRadioButtonUI() {
  private val size = Dimension()
  private val viewRect = Rectangle()
  private val iconRect = Rectangle()
  private val textRect = Rectangle()

  override fun getDefaultIcon(): Icon? {
    return null
  }

  @Synchronized
  override fun paint(g: Graphics, c: JComponent) {
    val f = c.getFont()
    g.setFont(f)
    val fm = c.getFontMetrics(f)

    val i = c.getInsets()
    c.getSize(size)
    viewRect.x = i.left
    viewRect.y = i.top
    viewRect.width = size.width - i.right - viewRect.x
    viewRect.height = size.height - i.bottom - viewRect.y
    iconRect.setBounds(0, 0, 0, 0)
    textRect.setBounds(0, 0, 0, 0)

    if (c.isOpaque()) {
      g.setColor(c.getBackground())
      g.fillRect(0, 0, size.width, size.height)
    }

    if (c is AbstractButton) {
      val text = SwingUtilities.layoutCompoundLabel(
          c, fm, c.getText(), null, c.getVerticalAlignment(),
          c.getHorizontalAlignment(), c.getVerticalTextPosition(), c.getHorizontalTextPosition(),
          viewRect, iconRect, textRect, 0)

      val m = c.getModel()
      g.setColor(c.getForeground())
      val isRollover = c.isRolloverEnabled() && m.isRollover()
      val isNotAimed = !m.isSelected() && !m.isPressed() && !m.isArmed()
      if (isNotAimed && isRollover) {
        g.drawLine(viewRect.x, viewRect.y + viewRect.height, viewRect.x + viewRect.width, viewRect.y + viewRect.height)
      }
      val v = c.getClientProperty(BasicHTML.propertyKey)
      if (v is View) {
        v.paint(g, textRect)
      } else {
        paintText(g, c, textRect, text)
      }
    }
  }
}

fun main() {
  EventQueue.invokeLater {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (ex: ClassNotFoundException) {
      ex.printStackTrace()
    } catch (ex: InstantiationException) {
      ex.printStackTrace()
    } catch (ex: IllegalAccessException) {
      ex.printStackTrace()
    } catch (ex: UnsupportedLookAndFeelException) {
      ex.printStackTrace()
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
