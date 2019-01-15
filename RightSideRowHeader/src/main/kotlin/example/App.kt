package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.util.Objects
import java.util.stream.IntStream
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableRowSorter

class MainPanel : JPanel(BorderLayout()) {
  private val data = arrayOf(
      arrayOf<Any>(1, 11, "A", ES, ES, ES, ES, ES),
      arrayOf<Any>(2, 22, ES, "B", ES, ES, ES, ES),
      arrayOf<Any>(3, 33, ES, ES, "C", ES, ES, ES),
      arrayOf<Any>(4, 1, ES, ES, ES, "D", ES, ES),
      arrayOf<Any>(5, 55, ES, ES, ES, ES, "E", ES),
      arrayOf<Any>(6, 66, ES, ES, ES, ES, ES, "F"))
  private val columnNames = arrayOf("fixed 1", "fixed 2", "A", "B", "C", "D", "E", "F")
  private val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int): Class<*> = if (column < FIXEDCOLUMN_RANGE) Int::class.java else Any::class.java
  }
  @Transient
  private val sorter = TableRowSorter<DefaultTableModel>(model)
  private val addButton = JButton("add")

  init {
    val fixedTable = JTable(model)
    val table = JTable(model)
    fixedTable.setSelectionModel(table.getSelectionModel())

    for (i in model.getColumnCount() - 1 downTo 0) {
      if (i < FIXEDCOLUMN_RANGE) {
        table.removeColumn(table.getColumnModel().getColumn(i))
        fixedTable.getColumnModel().getColumn(i).setResizable(false)
      } else {
        fixedTable.removeColumn(fixedTable.getColumnModel().getColumn(i))
      }
  }

    fixedTable.setRowSorter(sorter)
    fixedTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF)
    fixedTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
    fixedTable.putClientProperty("terminateEditOnFocusLost", java.lang.Boolean.TRUE)
    fixedTable.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.GRAY))
    fixedTable.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.GRAY))

    table.setRowSorter(sorter)
    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF)
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
    table.putClientProperty("terminateEditOnFocusLost", java.lang.Boolean.TRUE)

    val scroll = JScrollPane(table)
    scroll.setLayout(RightFixedScrollPaneLayout())

    fixedTable.setPreferredScrollableViewportSize(fixedTable.getPreferredSize())
    scroll.setRowHeaderView(fixedTable)

    scroll.setCorner(ScrollPaneConstants.UPPER_LEFT_CORNER, fixedTable.getTableHeader())
    // TEST:
    // table.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    // // fixedTable.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    // scroll.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    // scroll.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, fixedTable.getTableHeader());

    scroll.getViewport().setBackground(Color.WHITE)
    scroll.getRowHeader().setBackground(Color.WHITE)

    scroll.getRowHeader().addChangeListener({ e ->
      val viewport = e.getSource() as JViewport
      scroll.getVerticalScrollBar().setValue(viewport.getViewPosition().y)
    })

    addButton.addActionListener({
      sorter.setSortKeys(null)
      IntStream.range(0, 100).forEach({ i -> model.addRow(arrayOf<Any>(i, i + 1, "A$i", "B$i")) })
    })

    add(scroll)
    add(addButton, BorderLayout.SOUTH)
    setPreferredSize(Dimension(320, 240))
  }

  companion object {
    val FIXEDCOLUMN_RANGE = 2
    private val ES = ""
  }
}

internal class RightFixedScrollPaneLayout : ScrollPaneLayout() {
  override fun layoutContainer(parent: Container) {
    if (parent !is JScrollPane) {
      return
    }
    val scrollPane = parent // as JScrollPane
    vsbPolicy = scrollPane.getVerticalScrollBarPolicy()
    hsbPolicy = scrollPane.getHorizontalScrollBarPolicy()

    val availR = scrollPane.getBounds()
    availR.x = 0
    availR.y = 0

    val insets = parent.getInsets()
    availR.x = insets.left
    availR.y = insets.top
    availR.width -= insets.left + insets.right
    availR.height -= insets.top + insets.bottom

    val leftToRight = true // SwingUtilities.isLeftToRight(scrollPane);

    val colHeadR = Rectangle(0, availR.y, 0, 0)

    if (Objects.nonNull(colHead) && colHead.isVisible()) {
      val colHeadHeight = Math.min(availR.height, colHead.getPreferredSize().height)
      colHeadR.height = colHeadHeight
      availR.y += colHeadHeight
      availR.height -= colHeadHeight
    }

    val rowHeadR = Rectangle(0, 0, 0, 0)
    if (Objects.nonNull(rowHead) && rowHead.isVisible()) {
      val rowHeadWidth = Math.min(availR.width, rowHead.getPreferredSize().width)
      rowHeadR.width = rowHeadWidth
      availR.width -= rowHeadWidth
      // if (leftToRight) {
      //   rowHeadR.x = availR.x;
      //   availR.x += rowHeadWidth;
      // } else {
      //   rowHeadR.x = availR.x + availR.width;
      // }
      rowHeadR.x = availR.x + availR.width
    }

    val viewportBorder = scrollPane.getViewportBorder()
    val vpbInsets: Insets
    if (Objects.nonNull(viewportBorder)) {
      vpbInsets = viewportBorder.getBorderInsets(parent)
      availR.x += vpbInsets.left
      availR.y += vpbInsets.top
      availR.width -= vpbInsets.left + vpbInsets.right
      availR.height -= vpbInsets.top + vpbInsets.bottom
    } else {
      vpbInsets = Insets(0, 0, 0, 0)
    }

    val view = if (Objects.nonNull(viewport)) viewport.getView() else null
    val viewPrefSize = if (Objects.nonNull(view)) view!!.getPreferredSize() else Dimension()
    var extentSize = if (Objects.nonNull(viewport)) viewport.toViewCoordinates(availR.getSize()) else Dimension()

    var viewTracksViewportWidth = false
    var viewTracksViewportHeight = false
    val isEmpty = availR.width < 0 || availR.height < 0
    val sv: Scrollable?
    if (!isEmpty && view is Scrollable) {
      sv = view
      viewTracksViewportWidth = sv.getScrollableTracksViewportWidth()
      viewTracksViewportHeight = sv.getScrollableTracksViewportHeight()
    } else {
      sv = null
    }

    val vsbR = Rectangle(0, availR.y - vpbInsets.top, 0, 0)

    var vsbNeeded: Boolean
    if (vsbPolicy == ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS) {
      vsbNeeded = true
    } else if (vsbPolicy == ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER) {
      vsbNeeded = false
    } else { // vsbPolicy == VERTICAL_SCROLLBAR_AS_NEEDED
      vsbNeeded = !viewTracksViewportHeight && viewPrefSize.height > extentSize.height
    }

    if (Objects.nonNull(vsb) && vsbNeeded) {
      adjustForVsb(true, rowHeadR, vsbR, vpbInsets, leftToRight)
      extentSize = viewport.toViewCoordinates(availR.getSize())
    }

    val hsbR = Rectangle(availR.x - vpbInsets.left, 0, 0, 0)
    var hsbNeeded: Boolean
    if (hsbPolicy == ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS) {
      hsbNeeded = true
    } else if (hsbPolicy == ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER) {
      hsbNeeded = false
    } else { // hsbPolicy == HORIZONTAL_SCROLLBAR_AS_NEEDED
      hsbNeeded = !viewTracksViewportWidth && viewPrefSize.width > extentSize.width
    }

    if (Objects.nonNull(hsb) && hsbNeeded) {
      adjustForHsb(true, availR, hsbR, vpbInsets)
      if (Objects.nonNull(vsb) && !vsbNeeded && vsbPolicy != ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER) {

        extentSize = viewport.toViewCoordinates(availR.getSize())
        vsbNeeded = viewPrefSize.height > extentSize.height

        if (vsbNeeded) {
          // adjustForVsb(true, availR, vsbR, vpbInsets, leftToRight);
          adjustForVsb(true, rowHeadR, vsbR, vpbInsets, leftToRight)
        }
      }
    }

    if (viewport != null) {
      viewport.setBounds(availR)

      if (sv != null) {
        extentSize = viewport.toViewCoordinates(availR.getSize())

        val oldHsbNeeded = hsbNeeded
        val oldVsbNeeded = vsbNeeded
        viewTracksViewportWidth = sv.getScrollableTracksViewportWidth()
        viewTracksViewportHeight = sv.getScrollableTracksViewportHeight()
        if (Objects.nonNull(vsb) && vsbPolicy == ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED) {
          val newVsbNeeded = !viewTracksViewportHeight && viewPrefSize.height > extentSize.height
          if (newVsbNeeded != vsbNeeded) {
            vsbNeeded = newVsbNeeded
            // adjustForVsb(vsbNeeded, availR, vsbR, vpbInsets, leftToRight);
            adjustForVsb(vsbNeeded, rowHeadR, vsbR, vpbInsets, leftToRight)
            extentSize = viewport.toViewCoordinates(availR.getSize())
          }
        }
        if (Objects.nonNull(hsb) && hsbPolicy == ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED) {
          val newHsbNeeded = !viewTracksViewportWidth && viewPrefSize.width > extentSize.width
          if (newHsbNeeded != hsbNeeded) {
            hsbNeeded = newHsbNeeded
            adjustForHsb(hsbNeeded, availR, hsbR, vpbInsets)
            if (Objects.nonNull(vsb) && !vsbNeeded && vsbPolicy != ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER) {

              extentSize = viewport.toViewCoordinates(availR.getSize())
              vsbNeeded = viewPrefSize.height > extentSize.height

              if (vsbNeeded) {
                // adjustForVsb(true, availR, vsbR, vpbInsets, leftToRight);
                adjustForVsb(true, rowHeadR, vsbR, vpbInsets, leftToRight)
              }
            }
          }
        }
        if (oldHsbNeeded != hsbNeeded || oldVsbNeeded != vsbNeeded) {
          viewport.setBounds(availR)
        }
      }
    }

    vsbR.height = availR.height + vpbInsets.top + vpbInsets.bottom
    hsbR.width = availR.width + vpbInsets.left + vpbInsets.right
    rowHeadR.height = availR.height + vpbInsets.top + vpbInsets.bottom
    rowHeadR.y = availR.y - vpbInsets.top
    colHeadR.width = availR.width + vpbInsets.left + vpbInsets.right
    colHeadR.x = availR.x - vpbInsets.left

    if (Objects.nonNull(rowHead)) {
      rowHead.setBounds(rowHeadR)
    }

    if (Objects.nonNull(colHead)) {
      colHead.setBounds(colHeadR)
    }

    if (Objects.nonNull(vsb)) {
      if (vsbNeeded) {
        // if (Objects.nonNull(colHead) && UIManager.getBoolean("ScrollPane.fillUpperCorner")) {
        //   if (leftToRight && Objects.isNull(upperRight) || !leftToRight && Objects.isNull(upperLeft)) {
        //     vsbR.y = colHeadR.y;
        //     vsbR.height += colHeadR.height;
        //   }
        // }
        vsb.setVisible(true)
        vsb.setBounds(vsbR)
      } else {
        vsb.setVisible(false)
      }
    }

    if (Objects.nonNull(hsb)) {
      if (hsbNeeded) {
        // if (Objects.nonNull(rowHead) && UIManager.getBoolean("ScrollPane.fillLowerCorner")) {
        //   if (leftToRight && Objects.isNull(lowerLeft) || !leftToRight && Objects.isNull(lowerRight)) {
        //     if (leftToRight) {
        //       hsbR.x = rowHeadR.x;
        //     }
        //     hsbR.width += rowHeadR.width;
        //   }
        // }
        hsb.setVisible(true)
        hsb.setBounds(hsbR)
      } else {
        hsb.setVisible(false)
      }
    }

    if (Objects.nonNull(lowerLeft)) {
      lowerLeft.setBounds(if (leftToRight) rowHeadR.x else vsbR.x, hsbR.y, if (leftToRight) rowHeadR.width else vsbR.width, hsbR.height)
    }

    if (Objects.nonNull(lowerRight)) {
      lowerRight.setBounds(if (leftToRight) vsbR.x else rowHeadR.x, hsbR.y, if (leftToRight) vsbR.width else rowHeadR.width, hsbR.height)
    }

    if (Objects.nonNull(upperLeft)) {
      upperLeft.setBounds(if (leftToRight) rowHeadR.x else vsbR.x, colHeadR.y, if (leftToRight) rowHeadR.width else vsbR.width, colHeadR.height)
    }

    if (Objects.nonNull(upperRight)) {
      upperRight.setBounds(if (leftToRight) vsbR.x else rowHeadR.x, colHeadR.y, if (leftToRight) vsbR.width else rowHeadR.width, colHeadR.height)
    }
  }

  private fun adjustForVsb(wantsVsb: Boolean, available: Rectangle, vsbR: Rectangle, vpbInsets: Insets, leftToRight: Boolean) {
    val oldWidth = vsbR.width
    if (wantsVsb) {
      val vsbWidth = Math.max(0, Math.min(vsb.getPreferredSize().width, available.width))
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

  private fun adjustForHsb(wantsHsb: Boolean, available: Rectangle, hsbR: Rectangle, vpbInsets: Insets) {
    val oldHeight = hsbR.height
    if (wantsHsb) {
      val hsbHeight = Math.max(0, Math.min(available.height, hsb.getPreferredSize().height))
      available.height -= hsbHeight
      hsbR.y = available.y + available.height + vpbInsets.bottom
      hsbR.height = hsbHeight
    } else {
      available.height += oldHeight
    }
  }
}

fun main() {
  EventQueue.invokeLater({
    JFrame().apply {
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  })
}
