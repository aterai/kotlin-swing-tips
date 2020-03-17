package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableRowSorter

class MainPanel : JPanel(BorderLayout()) {
  private val data = arrayOf(
    arrayOf(1, 11, "A", ES, ES, ES, ES, ES),
    arrayOf(2, 22, ES, "B", ES, ES, ES, ES),
    arrayOf(3, 33, ES, ES, "C", ES, ES, ES),
    arrayOf(4, 1, ES, ES, ES, "D", ES, ES),
    arrayOf(5, 55, ES, ES, ES, ES, "E", ES),
    arrayOf(6, 66, ES, ES, ES, ES, ES, "F"))
  private val columnNames = arrayOf("fixed 1", "fixed 2", "A", "B", "C", "D", "E", "F")
  private val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = if (column < FIXED_RANGE) Number::class.java else Any::class.java
  }
  @Transient
  private val sorter = TableRowSorter<DefaultTableModel>(model)
  private val addButton = JButton("add")

  init {
    val fixedTable = JTable(model)
    val table = JTable(model)
    fixedTable.setSelectionModel(table.getSelectionModel())

    for (i in model.getColumnCount() - 1 downTo 0) {
      if (i < FIXED_RANGE) {
        table.removeColumn(table.getColumnModel().getColumn(i))
        fixedTable.getColumnModel().getColumn(i).setResizable(false)
      } else {
        fixedTable.removeColumn(fixedTable.getColumnModel().getColumn(i))
      }
    }

    fixedTable.setRowSorter(sorter)
    fixedTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF)
    fixedTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
    fixedTable.putClientProperty("terminateEditOnFocusLost", true)
    fixedTable.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.GRAY))
    fixedTable.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.GRAY))

    table.setRowSorter(sorter)
    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF)
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
    table.putClientProperty("terminateEditOnFocusLost", true)

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

    scroll.getRowHeader().addChangeListener { e ->
      val viewport = e.getSource() as? JViewport ?: return@addChangeListener
      scroll.getVerticalScrollBar().setValue(viewport.getViewPosition().y)
    }

    addButton.addActionListener {
      sorter.setSortKeys(null)
      for (i in 0 until 100) { model.addRow(arrayOf(i, i + 1, "A$i", "B$i")) }
    }

    add(scroll)
    add(addButton, BorderLayout.SOUTH)
    setPreferredSize(Dimension(320, 240))
  }

  companion object {
    const val FIXED_RANGE = 2
    private const val ES = ""
  }
}

@Suppress("LargeClass")
internal class RightFixedScrollPaneLayout : ScrollPaneLayout() {
  @Suppress("LongMethod", "ComplexMethod", "NestedBlockDepth")
  override fun layoutContainer(parent: Container) {
    val scrollPane = parent as? JScrollPane ?: return
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
    colHead?.takeIf { it.isVisible() }?.also {
      val colHeadHeight = minOf(availR.height, it.getPreferredSize().height)
      colHeadR.height = colHeadHeight
      availR.y += colHeadHeight
      availR.height -= colHeadHeight
    }

    val rowHeadR = Rectangle(0, 0, 0, 0)
    rowHead?.takeIf { it.isVisible() }?.also {
      val rowHeadWidth = minOf(availR.width, it.getPreferredSize().width)
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

    val vpbInsets = scrollPane.getViewportBorder()?.let {
      val ins = it.getBorderInsets(parent)
      availR.x += ins.left
      availR.y += ins.top
      availR.width -= ins.left + ins.right
      availR.height -= ins.top + ins.bottom
      ins
    } ?: Insets(0, 0, 0, 0)

    val view: Component? = viewport?.getView()
    val viewPrefSize = view?.getPreferredSize() ?: Dimension()
    var extentSize = viewport?.toViewCoordinates(availR.getSize()) ?: Dimension()

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
    var vsbNeeded = when (vsbPolicy) {
      ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS -> true
      ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER -> false
      else -> !viewTracksViewportHeight && viewPrefSize.height > extentSize.height
    }

    if (vsb != null && vsbNeeded) {
      adjustForVsb(true, rowHeadR, vsbR, vpbInsets, leftToRight)
      extentSize = viewport.toViewCoordinates(availR.getSize())
    }

    val hsbR = Rectangle(availR.x - vpbInsets.left, 0, 0, 0)
    var hsbNeeded = when (hsbPolicy) {
      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS -> true
      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER -> false
      else -> !viewTracksViewportWidth && viewPrefSize.width > extentSize.width
    }

    if (hsb != null && hsbNeeded) {
      adjustForHsb(true, availR, hsbR, vpbInsets)
      if (vsb != null && !vsbNeeded && vsbPolicy != ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER) {

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
        if (vsb != null && vsbPolicy == ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED) {
          val newVsbNeeded = !viewTracksViewportHeight && viewPrefSize.height > extentSize.height
          if (newVsbNeeded != vsbNeeded) {
            vsbNeeded = newVsbNeeded
            // adjustForVsb(vsbNeeded, availR, vsbR, vpbInsets, leftToRight);
            adjustForVsb(vsbNeeded, rowHeadR, vsbR, vpbInsets, leftToRight)
            extentSize = viewport.toViewCoordinates(availR.getSize())
          }
        }
        if (hsb != null && hsbPolicy == ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED) {
          val newHsbNeeded = !viewTracksViewportWidth && viewPrefSize.width > extentSize.width
          if (newHsbNeeded != hsbNeeded) {
            hsbNeeded = newHsbNeeded
            adjustForHsb(hsbNeeded, availR, hsbR, vpbInsets)
            if (vsb != null && !vsbNeeded && vsbPolicy != ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER) {

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

    rowHead?.setBounds(rowHeadR)

    colHead?.setBounds(colHeadR)

    if (vsb != null) {
      if (vsbNeeded) {
        // if (colHead != null && UIManager.getBoolean("ScrollPane.fillUpperCorner")) {
        //   if (leftToRight && upperRight != null || !leftToRight && upperLeft != null) {
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

    if (hsb != null) {
      if (hsbNeeded) {
        // if (rowHead != null && UIManager.getBoolean("ScrollPane.fillLowerCorner")) {
        //   if (leftToRight && lowerLeft != null || !leftToRight && lowerRight != null) {
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

    lowerLeft?.setBounds(
      if (leftToRight) rowHeadR.x else vsbR.x, hsbR.y,
      if (leftToRight) rowHeadR.width else vsbR.width, hsbR.height)

    lowerRight?.setBounds(
      if (leftToRight) vsbR.x else rowHeadR.x, hsbR.y,
      if (leftToRight) vsbR.width else rowHeadR.width, hsbR.height)

    upperLeft?.setBounds(
      if (leftToRight) rowHeadR.x else vsbR.x, colHeadR.y,
      if (leftToRight) rowHeadR.width else vsbR.width, colHeadR.height)

    upperRight?.setBounds(
      if (leftToRight) vsbR.x else rowHeadR.x, colHeadR.y,
      if (leftToRight) vsbR.width else rowHeadR.width, colHeadR.height)
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
      val vsbWidth = maxOf(0, minOf(vsb.getPreferredSize().width, available.width))
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
      val hsbHeight = maxOf(0, minOf(available.height, hsb.getPreferredSize().height))
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
      contentPane.add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
