package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableRowSorter

private const val FIXED_RANGE = 2
private const val ES = ""

private val data = arrayOf(
  arrayOf(1, 11, "A", ES, ES, ES, ES, ES),
  arrayOf(2, 22, ES, "B", ES, ES, ES, ES),
  arrayOf(3, 33, ES, ES, "C", ES, ES, ES),
  arrayOf(4, 1, ES, ES, ES, "D", ES, ES),
  arrayOf(5, 55, ES, ES, ES, ES, "E", ES),
  arrayOf(6, 66, ES, ES, ES, ES, ES, "F")
)
private val columnNames = arrayOf("fixed 1", "fixed 2", "A", "B", "C", "D", "E", "F")
private val model = object : DefaultTableModel(data, columnNames) {
  override fun getColumnClass(column: Int) = if (column < FIXED_RANGE) {
    Number::class.java
  } else {
    Any::class.java
  }
}

@Transient
private val sorter = TableRowSorter<DefaultTableModel>(model)
private val addButton = JButton("add")

fun makeUI(): Component {
  val fixedTable = JTable(model)
  val table = JTable(model)
  fixedTable.selectionModel = table.selectionModel

  for (i in model.columnCount - 1 downTo 0) {
    if (i < FIXED_RANGE) {
      table.removeColumn(table.columnModel.getColumn(i))
      fixedTable.columnModel.getColumn(i).resizable = false
    } else {
      fixedTable.removeColumn(fixedTable.columnModel.getColumn(i))
    }
  }

  fixedTable.rowSorter = sorter
  fixedTable.autoResizeMode = JTable.AUTO_RESIZE_OFF
  fixedTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
  fixedTable.putClientProperty("terminateEditOnFocusLost", true)
  fixedTable.border = BorderFactory.createMatteBorder(0, 1, 0, 0, Color.GRAY)
  fixedTable.tableHeader.border = BorderFactory.createMatteBorder(0, 1, 0, 0, Color.GRAY)

  table.rowSorter = sorter
  table.autoResizeMode = JTable.AUTO_RESIZE_OFF
  table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
  table.putClientProperty("terminateEditOnFocusLost", true)

  val scroll = JScrollPane(table)
  scroll.layout = RightFixedScrollPaneLayout()

  fixedTable.preferredScrollableViewportSize = fixedTable.preferredSize
  scroll.setRowHeaderView(fixedTable)

  scroll.setCorner(ScrollPaneConstants.UPPER_LEFT_CORNER, fixedTable.tableHeader)
  scroll.viewport.background = Color.WHITE
  scroll.rowHeader.background = Color.WHITE

  scroll.rowHeader.addChangeListener { e ->
    (e.source as? JViewport)?.also {
      scroll.verticalScrollBar.value = it.viewPosition.y
    }
  }

  addButton.addActionListener {
    sorter.sortKeys = null
    for (i in 0 until 100) {
      model.addRow(arrayOf(i, i + 1, "A$i", "B$i"))
    }
  }

  return JPanel(BorderLayout()).also {
    it.add(scroll)
    it.add(addButton, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

@Suppress("LargeClass")
private class RightFixedScrollPaneLayout : ScrollPaneLayout() {
  @Suppress("LongMethod", "ComplexMethod", "NestedBlockDepth")
  override fun layoutContainer(parent: Container) {
    val scrollPane = parent as? JScrollPane ?: return
    vsbPolicy = scrollPane.verticalScrollBarPolicy
    hsbPolicy = scrollPane.horizontalScrollBarPolicy

    val availR = scrollPane.bounds
    availR.x = 0
    availR.y = 0

    val ins = parent.insets
    availR.x = ins.left
    availR.y = ins.top
    availR.width -= ins.left + ins.right
    availR.height -= ins.top + ins.bottom

    // val leftToRight = SwingUtilities.isLeftToRight(scrollPane)
    val leftToRight = scrollPane.componentOrientation.isLeftToRight

    val colHeadR = Rectangle(0, availR.y, 0, 0)
    colHead?.takeIf { it.isVisible }?.also {
      val colHeadHeight = minOf(availR.height, it.preferredSize.height)
      colHeadR.height = colHeadHeight
      availR.y += colHeadHeight
      availR.height -= colHeadHeight
    }

    val rowHeadR = Rectangle(0, 0, 0, 0)
    rowHead?.takeIf { it.isVisible }?.also {
      val rowHeadWidth = minOf(availR.width, it.preferredSize.width)
      rowHeadR.width = rowHeadWidth
      availR.width -= rowHeadWidth
      // if (leftToRight) {
      //   rowHeadR.x = availR.x
      //   availR.x += rowHeadWidth
      // } else {
      //   rowHeadR.x = availR.x + availR.width
      // }
      rowHeadR.x = availR.x + availR.width
    }

    val vpbInsets = scrollPane.viewportBorder?.let {
      val i = it.getBorderInsets(parent)
      availR.x += i.left
      availR.y += i.top
      availR.width -= i.left + i.right
      availR.height -= i.top + i.bottom
      i
    } ?: Insets(0, 0, 0, 0)

    val view = viewport?.view
    val viewPrefSize = view?.preferredSize ?: Dimension()
    var extentSize = viewport?.toViewCoordinates(availR.size) ?: Dimension()

    var scrollableWidth = false
    var scrollableHeight = false
    val isEmpty = availR.width < 0 || availR.height < 0
    val sv = if (!isEmpty && view is Scrollable) {
      scrollableWidth = view.scrollableTracksViewportWidth
      scrollableHeight = view.scrollableTracksViewportHeight
      view
    } else {
      null
    }

    val vsbR = Rectangle(0, availR.y - vpbInsets.top, 0, 0)
    var vsbNeeded = when (vsbPolicy) {
      ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS -> true
      ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER -> false
      else -> !scrollableHeight && viewPrefSize.height > extentSize.height
    }

    if (vsb != null && vsbNeeded) {
      adjustForVsb(true, rowHeadR, vsbR, vpbInsets, leftToRight)
      extentSize = viewport.toViewCoordinates(availR.size)
    }

    val hsbR = Rectangle(availR.x - vpbInsets.left, 0, 0, 0)
    var hsbNeeded = when (hsbPolicy) {
      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS -> true
      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER -> false
      else -> !scrollableWidth && viewPrefSize.width > extentSize.width
    }

    if (hsb != null && hsbNeeded) {
      adjustForHsb(true, availR, hsbR, vpbInsets)
      if (vsb != null && !vsbNeeded && vsbPolicy != ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER) {

        extentSize = viewport.toViewCoordinates(availR.size)
        vsbNeeded = viewPrefSize.height > extentSize.height

        if (vsbNeeded) {
          // adjustForVsb(true, availR, vsbR, vpbInsets, leftToRight);
          adjustForVsb(true, rowHeadR, vsbR, vpbInsets, leftToRight)
        }
      }
    }

    if (viewport != null) {
      viewport.bounds = availR

      if (sv != null) {
        extentSize = viewport.toViewCoordinates(availR.size)

        val oldHsbNeeded = hsbNeeded
        val oldVsbNeeded = vsbNeeded
        scrollableWidth = sv.scrollableTracksViewportWidth
        scrollableHeight = sv.scrollableTracksViewportHeight
        if (vsb != null && vsbPolicy == ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED) {
          val newVsbNeeded = !scrollableHeight && viewPrefSize.height > extentSize.height
          if (newVsbNeeded != vsbNeeded) {
            vsbNeeded = newVsbNeeded
            // adjustForVsb(vsbNeeded, availR, vsbR, vpbInsets, leftToRight)
            adjustForVsb(vsbNeeded, rowHeadR, vsbR, vpbInsets, leftToRight)
            extentSize = viewport.toViewCoordinates(availR.size)
          }
        }
        if (hsb != null && hsbPolicy == ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED) {
          val newHsbNeeded = !scrollableWidth && viewPrefSize.width > extentSize.width
          if (newHsbNeeded != hsbNeeded) {
            hsbNeeded = newHsbNeeded
            adjustForHsb(hsbNeeded, availR, hsbR, vpbInsets)
            if (vsb != null && !vsbNeeded && vsbPolicy != ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER) {

              extentSize = viewport.toViewCoordinates(availR.size)
              vsbNeeded = viewPrefSize.height > extentSize.height

              if (vsbNeeded) {
                // adjustForVsb(true, availR, vsbR, vpbInsets, leftToRight)
                adjustForVsb(true, rowHeadR, vsbR, vpbInsets, leftToRight)
              }
            }
          }
        }
        if (oldHsbNeeded != hsbNeeded || oldVsbNeeded != vsbNeeded) {
          viewport.bounds = availR
        }
      }
    }

    vsbR.height = availR.height + vpbInsets.top + vpbInsets.bottom
    hsbR.width = availR.width + vpbInsets.left + vpbInsets.right
    rowHeadR.height = availR.height + vpbInsets.top + vpbInsets.bottom
    rowHeadR.y = availR.y - vpbInsets.top
    colHeadR.width = availR.width + vpbInsets.left + vpbInsets.right
    colHeadR.x = availR.x - vpbInsets.left

    rowHead?.bounds = rowHeadR

    colHead?.bounds = colHeadR

    if (vsb != null) {
      if (vsbNeeded) {
        // if (colHead != null && UIManager.getBoolean("ScrollPane.fillUpperCorner")) {
        //   if (leftToRight && upperRight != null || !leftToRight && upperLeft != null) {
        //     vsbR.y = colHeadR.y
        //     vsbR.height += colHeadR.height
        //   }
        // }
        vsb.isVisible = true
        vsb.bounds = vsbR
      } else {
        vsb.isVisible = false
      }
    }

    if (hsb != null) {
      if (hsbNeeded) {
        // if (rowHead != null && UIManager.getBoolean("ScrollPane.fillLowerCorner")) {
        //   if (leftToRight && lowerLeft != null || !leftToRight && lowerRight != null) {
        //     if (leftToRight) {
        //       hsbR.x = rowHeadR.x
        //     }
        //     hsbR.width += rowHeadR.width
        //   }
        // }
        hsb.isVisible = true
        hsb.bounds = hsbR
      } else {
        hsb.isVisible = false
      }
    }

    lowerLeft?.setBounds(
      if (leftToRight) rowHeadR.x else vsbR.x,
      hsbR.y,
      if (leftToRight) rowHeadR.width else vsbR.width,
      hsbR.height
    )

    lowerRight?.setBounds(
      if (leftToRight) vsbR.x else rowHeadR.x,
      hsbR.y,
      if (leftToRight) vsbR.width else rowHeadR.width,
      hsbR.height
    )

    upperLeft?.setBounds(
      if (leftToRight) rowHeadR.x else vsbR.x,
      colHeadR.y,
      if (leftToRight) rowHeadR.width else vsbR.width,
      colHeadR.height
    )

    upperRight?.setBounds(
      if (leftToRight) vsbR.x else rowHeadR.x,
      colHeadR.y,
      if (leftToRight) vsbR.width else rowHeadR.width,
      colHeadR.height
    )
  }

  private fun adjustForVsb(
    wantsVsb: Boolean,
    available: Rectangle,
    vsbR: Rectangle,
    vpbInsets: Insets,
    leftToRight: Boolean
  ) {
    val oldWidth = vsbR.width
    if (wantsVsb) {
      val vsbWidth = available.width.coerceIn(0, vsb.preferredSize.width)
      available.width -= vsbWidth
      vsbR.width = vsbWidth

      if (leftToRight) {
        vsbR.x = available.x + available.width + vpbInsets.right
      } else {
        vsbR.x = available.x - vpbInsets.left
        available.x += vsbWidth
      }
    } else {
      available.width += oldWidth
    }
  }

  private fun adjustForHsb(
    wantsHsb: Boolean,
    available: Rectangle,
    hsbR: Rectangle,
    vpbInsets: Insets
  ) {
    val oldHeight = hsbR.height
    if (wantsHsb) {
      val hsbHeight = available.height.coerceIn(0, hsb.preferredSize.height)
      available.height -= hsbHeight
      hsbR.y = available.y + available.height + vpbInsets.bottom
      hsbR.height = hsbHeight
    } else {
      available.height += oldHeight
    }
  }
}

fun main() {
  EventQueue.invokeLater {
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
