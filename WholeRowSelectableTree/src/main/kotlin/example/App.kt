package example

import java.awt.*
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.*
import javax.swing.plaf.basic.BasicTreeUI
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.TreePath

fun makeUI(): Component {
  UIManager.put("Tree.paintLines", true)
  UIManager.put("Tree.repaintWholeRow", true)
  UIManager.put("Tree.hash", Color.DARK_GRAY)

  val tree = object : JTree() {
    override fun updateUI() {
      super.updateUI()
      setUI(WholeRowSelectableTreeUI())
    }
  }

  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())

  return JPanel(GridLayout(1, 0)).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(JScrollPane(JTree()))
    it.add(JScrollPane(tree))
    it.preferredSize = Dimension(320, 240)
  }
}

private class WholeRowSelectableTreeUI : BasicTreeUI() {
  @Suppress("LongMethod", "ComplexMethod", "NestedBlockDepth", "CognitiveComplexMethod")
  override fun paint(g: Graphics, c: JComponent) {
    val paintBounds = g.clipBounds
    val insets = tree.insets
    val initialPath = getClosestPathForLocation(tree, 0, paintBounds.y)
    // var paintingEnum: Enumeration<*>? = treeState.getVisiblePathsFrom(initialPath)
    var row = treeState.getRowForPath(initialPath)
    val endY = paintBounds.y + paintBounds.height
    val treeModel = tree.model
    drawingCache.clear()
    val visiblePathsFrom = treeState.getVisiblePathsFrom(initialPath)
    if (initialPath != null && visiblePathsFrom != null) {
      // First pass, draw the rows
      var done = false
      var isExpanded: Boolean
      var hasBeenExpanded: Boolean
      var isLeaf: Boolean
      var bounds: Rectangle?
      for (path in visiblePathsFrom) {
        if (done) {
          break
        }
        bounds = getPathBounds(tree, path)
        if (path != null && bounds != null) {
          isLeaf = treeModel.isLeaf(path.lastPathComponent)
          if (isLeaf) {
            hasBeenExpanded = false
            isExpanded = hasBeenExpanded
          } else {
            isExpanded = treeState.getExpandedState(path)
            hasBeenExpanded = tree.hasBeenExpanded(path)
          }
          paintRow(
            g, paintBounds, insets, bounds, path, row, isExpanded, hasBeenExpanded, isLeaf,
          )
          if (bounds.y + bounds.height >= endY) {
            done = true
          }
        } else {
          done = true
        }
        row++
      }

      // Draw the connecting lines and controls.
      // Find each parent and have them draw a line to their last child
      // val rootVisible = tree.isRootVisible()
      var parentPath = initialPath
      parentPath = parentPath.parentPath
      while (parentPath != null) {
        paintVerticalPartOfLeg(g, paintBounds, insets, parentPath)
        drawingCache[parentPath] = java.lang.Boolean.TRUE
        parentPath = parentPath.parentPath
      }
      done = false
      // paintingEnum = treeState.getVisiblePathsFrom(initialPath)
      for (path in treeState.getVisiblePathsFrom(initialPath)) {
        if (done) {
          break
        }
        // path = paintingEnum.nextElement()
        bounds = getPathBounds(tree, path)
        if (path != null && bounds != null) {
          isLeaf = treeModel.isLeaf(path.lastPathComponent)
          if (isLeaf) {
            hasBeenExpanded = false
            isExpanded = hasBeenExpanded
          } else {
            isExpanded = treeState.getExpandedState(path)
            hasBeenExpanded = tree.hasBeenExpanded(path)
          }
          // See if the vertical line to the parent has been drawn.
          parentPath = path.parentPath
          if (parentPath != null) {
            if (drawingCache[parentPath] == null) {
              paintVerticalPartOfLeg(g, paintBounds, insets, parentPath)
              drawingCache[parentPath] = java.lang.Boolean.TRUE
            }
            paintHorizontalPartOfLeg(
              g,
              paintBounds,
              insets,
              bounds,
              path,
              row,
              isExpanded,
              hasBeenExpanded,
              isLeaf,
            )
          } else if (tree.isRootVisible && row == 0) {
            paintHorizontalPartOfLeg(
              g,
              paintBounds,
              insets,
              bounds,
              path,
              row,
              isExpanded,
              hasBeenExpanded,
              isLeaf,
            )
          }
          if (shouldPaintExpandControl(path, row, isExpanded, hasBeenExpanded, isLeaf)) {
            paintExpandControl(
              g,
              paintBounds,
              insets, bounds,
              path,
              row,
              isExpanded,
              hasBeenExpanded,
              isLeaf,
            )
          }
          if (bounds.y + bounds.height >= endY) {
            done = true
          }
        } else {
          done = true
        }
        row++
      }
    }
    paintDropLine(g)

    // Empty out the renderer pane, allowing renderers to be gc'ed.
    rendererPane.removeAll()
    drawingCache.clear()
  }

  override fun paintRow(
    g: Graphics,
    clipBounds: Rectangle,
    insets: Insets,
    bounds: Rectangle,
    path: TreePath,
    row: Int,
    isExpanded: Boolean,
    hasBeenExpanded: Boolean,
    isLeaf: Boolean,
  ) {
    val isSelected = tree.isRowSelected(row)

    // Don't paint the renderer if editing this row.
    if (editingComponent != null && editingRow == row) {
      if (isSelected) {
        val oldColor = g.color
        g.color = Color.PINK
        g.fillRect(0, bounds.y, tree.width, bounds.height)
        g.color = oldColor
      }
      return
    }
    val component = currentCellRenderer.getTreeCellRendererComponent(
      tree,
      path.lastPathComponent,
      isSelected,
      isExpanded,
      isLeaf,
      row,
      false,
    )
    if (isSelected) {
      val oldColor = g.color
      g.color = getBackgroundSelectionColor(component)
      g.fillRect(0, bounds.y, tree.width, bounds.height)
      g.color = oldColor
    }
    rendererPane.paintComponent(
      g,
      component,
      tree,
      bounds.x,
      bounds.y,
      bounds.width,
      bounds.height,
      true,
    )
    val leadIndex = if (tree.hasFocus()) leadSelectionRow else -1
    val hasFocus = leadIndex == row
    if (hasFocus) {
      g.color = UIManager.getColor("Tree.selectionBorderColor")
      g.drawRect(0, bounds.y, tree.width - 1, bounds.height - 1)
    }
  }

  override fun createMouseListener(): MouseListener {
    return object : MouseHandler() {
      override fun mousePressed(e: MouseEvent) {
        super.mousePressed(convertMouseEvent(e))
      }

      override fun mouseReleased(e: MouseEvent) {
        super.mouseReleased(convertMouseEvent(e))
      }

      override fun mouseDragged(e: MouseEvent) {
        super.mouseDragged(convertMouseEvent(e))
      }

      private fun convertMouseEvent(e: MouseEvent): MouseEvent {
        val path = getClosestPathForLocation(tree, e.x, e.y)
        val bounds = getPathBounds(tree, path)
        val newX = bounds.centerX.toInt()
        val b1 = !tree.isEnabled || !SwingUtilities.isLeftMouseButton(e) || e.isConsumed
        val b2 = path == null || isLocationInExpandControl(path, e.x, e.y)
        bounds.x = 0
        bounds.width = tree.width
        return if (!b1 && !b2 && bounds.contains(e.point)) {
          MouseEvent(
            e.component,
            e.id,
            e.getWhen(),
            e.modifiers or e.modifiersEx,
            newX,
            e.y,
            e.clickCount,
            e.isPopupTrigger,
            e.button,
          )
        } else {
          e
        }
      }
    }
  }

  private fun getBackgroundSelectionColor(c: Component) =
    if (c is DefaultTreeCellRenderer) c.backgroundSelectionColor else Color.LIGHT_GRAY
}

private object LookAndFeelUtils {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.name

  fun createLookAndFeelMenu(): JMenu {
    val menu = JMenu("LookAndFeel")
    val buttonGroup = ButtonGroup()
    for (info in UIManager.getInstalledLookAndFeels()) {
      val b = JRadioButtonMenuItem(info.name, info.className == lookAndFeel)
      initLookAndFeelAction(info, b)
      menu.add(b)
      buttonGroup.add(b)
    }
    return menu
  }

  fun initLookAndFeelAction(info: UIManager.LookAndFeelInfo, b: AbstractButton) {
    val cmd = info.className
    b.text = info.name
    b.actionCommand = cmd
    b.hideActionText = true
    b.addActionListener { setLookAndFeel(cmd) }
  }

  @Throws(
    ClassNotFoundException::class,
    InstantiationException::class,
    IllegalAccessException::class,
    UnsupportedLookAndFeelException::class,
  )
  private fun setLookAndFeel(newLookAndFeel: String) {
    val oldLookAndFeel = lookAndFeel
    if (oldLookAndFeel != newLookAndFeel) {
      UIManager.setLookAndFeel(newLookAndFeel)
      lookAndFeel = newLookAndFeel
      updateLookAndFeel()
    }
  }

  private fun updateLookAndFeel() {
    for (window in Window.getWindows()) {
      SwingUtilities.updateComponentTreeUI(window)
    }
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
