package example

import java.awt.*
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.image.BufferedImage
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.*
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.TreePath

fun createUI() = JPanel(GridLayout(1, 2, 2, 2)).also {
  it.add(createTranslucentScrollPane(TranslucentTree()))
  it.add(createTranslucentScrollPane(TransparentTree()))
  it.isOpaque = false
  it.preferredSize = Dimension(320, 240)
}

private fun createTranslucentScrollPane(view: Component) = JScrollPane(view).also {
  it.isOpaque = false
  it.viewport.isOpaque = false
}

private class TranslucentTree : JTree() {
  override fun updateUI() {
    super.updateUI()
    setCellRenderer(TranslucentTreeCellRenderer())
    isOpaque = false
    border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  }
}

private class TransparentTree : JTree() {
  private var handler: RepaintHandler? = null

  private fun repaintWholeRow(path: TreePath?) {
    path?.let { getPathBounds(it) }?.also {
      repaint(0, it.y, width, it.height)
    }
  }

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.paint = SELECTED_COLOR
    selectionRows
      ?.asList()
      ?.mapNotNull { getRowBounds(it) }
      ?.forEach { g2.fillRect(0, it.y, width, it.height) }
    super.paintComponent(g)
    if (hasFocus()) {
      leadSelectionPath?.let { getPathBounds(it) }?.also { r ->
        g2.paint = SELECTED_COLOR.darker()
        g2.drawRect(0, r.y, width - 1, r.height - 1)
      }
    }
    g2.dispose()
  }

  override fun updateUI() {
    removeTreeSelectionListener(handler)
    removePropertyChangeListener(LEAD_SELECTION_PATH_PROPERTY, handler)
    removeFocusListener(handler)
    super.updateUI()
    // The selection background and the lead row border are painted across
    // the whole width of the tree, but the TreeUI repaints only the node
    // bounds, so repaint the whole row of the changed paths instead of using
    // the global UIManager.put("Tree.repaintWholeRow", true) setting.
    val h = RepaintHandler()
    addTreeSelectionListener(h)
    addPropertyChangeListener(LEAD_SELECTION_PATH_PROPERTY, h)
    addFocusListener(h)
    handler = h
    setCellRenderer(TransparentTreeCellRenderer())
    isOpaque = false
    border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  }

  private inner class RepaintHandler :
    TreeSelectionListener,
    PropertyChangeListener,
    FocusListener {
    override fun valueChanged(e: TreeSelectionEvent) {
      e.paths?.forEach { repaintWholeRow(it) }
    }

    override fun propertyChange(e: PropertyChangeEvent) {
      repaintWholeRow(e.oldValue as? TreePath)
      repaintWholeRow(e.newValue as? TreePath)
    }

    override fun focusGained(e: FocusEvent) {
      repaintWholeRow(leadSelectionPath)
    }

    override fun focusLost(e: FocusEvent) {
      repaintWholeRow(leadSelectionPath)
    }
  }

  companion object {
    private val SELECTED_COLOR = Color(0x64_64_64_FF, true)
  }
}

// https://ateraimemo.com/Swing/RootPaneBackground.html
private class TransparentRootPane : JRootPane() {
  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val g2 = g.create() as? Graphics2D ?: return
    g2.paint = TEXTURE
    g2.fillRect(0, 0, width, height)
    g2.dispose()
  }

  override fun updateUI() {
    super.updateUI()
    isOpaque = false
  }

  companion object {
    private val TEXTURE = createCheckerTexture()

    private fun createCheckerTexture(): TexturePaint {
      val cs = 6
      val sz = cs * cs
      val img = BufferedImage(sz, sz, BufferedImage.TYPE_INT_ARGB)
      val g2 = img.createGraphics()
      g2.paint = Color(0xDC_DC_DC)
      g2.fillRect(0, 0, sz, sz)
      g2.paint = Color(0xC8_C8_C8_C8.toInt(), true)
      var i = 0
      while (i * cs < sz) {
        var j = 0
        while (j * cs < sz) {
          if ((i + j) % 2 == 0) {
            g2.fillRect(i * cs, j * cs, cs, cs)
          }
          j++
        }
        i++
      }
      g2.dispose()
      return TexturePaint(img, Rectangle(sz, sz))
    }
  }
}

// https://ateraimemo.com/Swing/TreeBackgroundSelectionColor.html
private open class TransparentTreeCellRenderer : DefaultTreeCellRenderer() {
  override fun getTreeCellRendererComponent(
    tree: JTree,
    value: Any?,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int,
    hasFocus: Boolean,
  ): Component {
    // Pass false as hasFocus to hide the focus border of the node
    val c = super.getTreeCellRendererComponent(
      tree,
      value,
      selected,
      expanded,
      leaf,
      row,
      false,
    )
    (c as? JComponent)?.isOpaque = false
    return c
  }

  // Do not delegate to getBackgroundSelectionColor(), as a subclass may
  // override it to paint the selected nodes with a translucent color.
  override fun getBackgroundNonSelectionColor() = ALPHA_OF_ZERO

  override fun getBackgroundSelectionColor() = ALPHA_OF_ZERO

  companion object {
    private val ALPHA_OF_ZERO = Color(0x0, true)
  }
}

private class TranslucentTreeCellRenderer : TransparentTreeCellRenderer() {
  override fun getBackgroundSelectionColor() = SELECTION_COLOR

  companion object {
    private val SELECTION_COLOR = Color(0x64_64_64_FF, true)
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
    val frame = object : JFrame() {
      override fun createRootPane() = TransparentRootPane()
    }
    val contentPane = frame.contentPane
    (contentPane as? JComponent)?.isOpaque = false
    contentPane.add(createUI())
    frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    frame.pack()
    frame.setLocationRelativeTo(null)
    frame.isVisible = true
  }
}
