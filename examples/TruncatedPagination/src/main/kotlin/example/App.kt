package example

import java.awt.*
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter
import kotlin.math.abs

private const val EDGE_PAGE_COUNT = 2
private const val PAGE_WINDOW_DELTA = 2
private const val ELLIPSIS = -1
private const val SINGLE_PAGE_COUNT = 1
private val paginationBar = Box.createHorizontalBox()
private val model = createTableModel()
private val sorter = TableRowSorter<TableModel>(model)

fun createUI(): Component {
  val table = JTable(model)
  table.fillsViewportHeight = true
  table.intercellSpacing = Dimension()
  table.setShowGrid(false)
  table.putClientProperty("terminateEditOnFocusLost", true)
  table.rowSorter = sorter

  for (i in 1..2026) {
    val rowData = arrayOf<Any>(
      i,
      "Test: $i",
      if (i % 2 == 0) "" else "comment...",
    )
    model.addRow(rowData)
  }
  showPage(100, 1)
  paginationBar.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2))

  return JPanel(BorderLayout()).also {
    it.add(paginationBar, BorderLayout.NORTH)
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun showPage(
  itemsPerPage: Int,
  currentPageIndex: Int,
) {
  sorter.rowFilter = object : RowFilter<TableModel, Int>() {
    override fun include(entry: Entry<out TableModel, out Int>): Boolean {
      val start = (currentPageIndex - 1) * itemsPerPage
      val end = start + itemsPerPage
      return entry.identifier in start..<end
    }
  }

  val rowCount = model.rowCount
  val extraPage = if (rowCount % itemsPerPage == 0) 0 else 1
  val maxPageIndex = rowCount / itemsPerPage + extraPage

  paginationBar.removeAll()
  if (maxPageIndex <= SINGLE_PAGE_COUNT) {
    return
  }

  val hasPreviousPage = currentPageIndex > 1
  val prevBtn = createPrevNextButton(
    itemsPerPage,
    currentPageIndex - 1,
    "<",
    hasPreviousPage,
  )
  paginationBar.add(prevBtn)

  paginationBar.add(Box.createHorizontalGlue())
  val pageButtonGroup = ButtonGroup()
  for (pageIndex in createPageIndexList(currentPageIndex, maxPageIndex)) {
    if (pageIndex == ELLIPSIS) {
      paginationBar.add(createEllipsisLabel())
    } else {
      val pageButton = createPageButton(
        itemsPerPage,
        currentPageIndex,
        pageIndex,
      )
      paginationBar.add(pageButton)
      pageButtonGroup.add(pageButton)
    }
  }
  paginationBar.add(Box.createHorizontalGlue())

  val hasNextPage = currentPageIndex < maxPageIndex
  val nextBtn = createPrevNextButton(
    itemsPerPage,
    currentPageIndex + 1,
    ">",
    hasNextPage,
  )
  paginationBar.add(nextBtn)
  paginationBar.revalidate()
  paginationBar.repaint()
}

private fun createPageButton(
  itemsPerPage: Int,
  currentPageIndex: Int,
  targetPageIndex: Int,
): AbstractButton {
  val button = object : JToggleButton(targetPageIndex.toString()) {
    override fun updateUI() {
      super.updateUI()
      setMargin(Insets(2, 4, 2, 4))
    }
  }
  if (targetPageIndex == currentPageIndex) {
    button.setSelected(true)
  }
  button.addActionListener { showPage(itemsPerPage, targetPageIndex) }
  return button
}

private fun createPrevNextButton(
  itemsPerPage: Int,
  targetPageIndex: Int,
  label: String,
  enabled: Boolean,
): AbstractButton {
  val button: AbstractButton = object : JButton(label) {
    override fun updateUI() {
      super.updateUI()
      setMargin(Insets(2, 4, 2, 4))
      setFocusable(false)
    }
  }
  button.setEnabled(enabled)
  button.addActionListener { showPage(itemsPerPage, targetPageIndex) }
  return button
}

fun createTableModel(): DefaultTableModel {
  val columnNames = arrayOf("Year", "String", "Comment")
  return object : DefaultTableModel(columnNames, 0) {
    override fun getColumnClass(
      column: Int,
    ): Class<*> = if (column == 0) Int::class.java else Any::class.java
  }
}

private fun createPageIndexList(
  currentPageIndex: Int,
  maxPageIndex: Int,
): MutableList<Int> {
  val pageIndexList = mutableListOf<Int>()
  var previousIndex = 0
  for (i in 1..maxPageIndex) {
    val isVisible =
      i <= EDGE_PAGE_COUNT || i > maxPageIndex - EDGE_PAGE_COUNT ||
        isWithinPageWindow(i, currentPageIndex)
    if (!isVisible) {
      continue
    }
    if (previousIndex != 0 && i != previousIndex + 1) {
      pageIndexList.add(ELLIPSIS)
    }
    pageIndexList.add(i)
    previousIndex = i
  }
  return pageIndexList
}

private fun isWithinPageWindow(
  pageIndex: Int,
  currentPageIndex: Int,
) = abs(pageIndex - currentPageIndex) <= PAGE_WINDOW_DELTA

private fun createEllipsisLabel(): JComponent {
  val label = JLabel("...")
  label.setForeground(Color.GRAY)
  label.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4))
  return label
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
