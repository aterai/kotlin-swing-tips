package example

import java.awt.*
import java.awt.geom.Area
import java.awt.geom.Path2D
import java.awt.geom.PathIterator
import javax.swing.*
import javax.swing.plaf.nimbus.AbstractRegionPainter
import javax.swing.tree.DefaultTreeCellRenderer

fun makeUI(): Component {
  val tree = JTree()
  tree.rowHeight = 20
  val mb = JMenuBar()
  mb.add(example.LookAndFeelUtils.createLookAndFeelMenu())
  return JPanel(GridLayout(1, 2, 2, 2)).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(makeScrollPane(tree))
    it.add(makeScrollPane(RoundedSelectionTree()))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeScrollPane(view: Component): JScrollPane {
  val scroll = JScrollPane(view)
  scroll.background = Color.WHITE
  scroll.viewport.background = Color.WHITE
  scroll.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
  return scroll
}

private class RoundedSelectionTree : JTree() {
  override fun paintComponent(g: Graphics) {
    val sr = selectionRows
    if (sr != null) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON,
      )
      g2.paint = SELECTED_COLOR
      val innerArea = SwingUtilities.calculateInnerArea(this, null)
      val area = Area()
      sr
        .map { getRowBounds(it) }
        .map { Rectangle(innerArea.x, it.y, innerArea.width, it.height) }
        .forEach { area.add(Area(it)) }
      val arc = 10
      for (a in singularization(area)) {
        val r = a.bounds
        g2.fillRoundRect(r.x, r.y, r.width - 1, r.height - 1, arc, arc)
      }
      g2.dispose()
    }
    super.paintComponent(g)
  }

  override fun updateUI() {
    super.updateUI()
    UIManager.put("Tree.repaintWholeRow", true)
    setCellRenderer(TransparentTreeCellRenderer())
    isOpaque = false
    setRowHeight(20)
    val d = UIDefaults()
    val key = "Tree:TreeCell[Enabled+Selected].backgroundPainter"
    d[key] = TransparentTreeCellPainter()
    putClientProperty("Nimbus.Overrides", d)
    putClientProperty("Nimbus.Overrides.InheritDefaults", false)
    addTreeSelectionListener { repaint() }
  }

  private fun singularization(rect: Area): List<Area> {
    val list = mutableListOf<Area>()
    val path = Path2D.Double()
    val pi = rect.getPathIterator(null)
    val coords = DoubleArray(6)
    while (!pi.isDone) {
      val pathSegmentType = pi.currentSegment(coords)
      when (pathSegmentType) {
        PathIterator.SEG_MOVETO -> {
          path.reset()
          path.moveTo(coords[0], coords[1])
        }

        PathIterator.SEG_LINETO -> {
          path.lineTo(coords[0], coords[1])
        }

        PathIterator.SEG_CLOSE -> {
          path.closePath()
          list.add(Area(path))
        }
      }
      pi.next()
    }
    return list
  }

  companion object {
    private val SELECTED_COLOR = Color(0xC8_00_78_D7.toInt(), true)
  }
}

private class TransparentTreeCellRenderer : DefaultTreeCellRenderer() {
  override fun getTreeCellRendererComponent(
    tree: JTree,
    value: Any,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int,
    hasFocus: Boolean,
  ): Component {
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

  override fun getBackgroundNonSelectionColor() = ALPHA_OF_ZERO

  override fun getBackgroundSelectionColor() = ALPHA_OF_ZERO

  companion object {
    private val ALPHA_OF_ZERO = Color(0x0, true)
  }
}

private class TransparentTreeCellPainter : AbstractRegionPainter() {
  override fun doPaint(
    g: Graphics2D,
    c: JComponent,
    width: Int,
    height: Int,
    extendedCacheKeys: Array<Any>?,
  ) {
    // Do nothing
  }

  override fun getPaintContext() = null
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

  fun initLookAndFeelAction(
    info: UIManager.LookAndFeelInfo,
    b: AbstractButton,
  ) {
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

