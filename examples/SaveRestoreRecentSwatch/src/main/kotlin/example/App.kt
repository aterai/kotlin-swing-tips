package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.accessibility.AccessibleContext
import javax.swing.*
import javax.swing.colorchooser.AbstractColorChooserPanel

fun createUI(): Component {
  val label = object : JLabel() {
    override fun getPreferredSize() = Dimension(32, 32)
  }
  label.setOpaque(true)
  label.setBackground(Color.WHITE)

  val recentPanel = RecentSwatchPanel()
  recentPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK))
  recentPanel.colors?.also {
    it[0] = Color.RED
    it[1] = Color.GREEN
    it[2] = Color.BLUE
  }

  val button = JButton("open JColorChooser")
  button.addActionListener {
    val cc = JColorChooser()
    cc.setChooserPanels(createChooserPanels(cc, recentPanel))
    val ok = ColorTracker(cc)
    val parent = button.rootPane
    val title = "JColorChooser"
    val dialog = JColorChooser.createDialog(parent, title, true, cc, ok, null)
    dialog.addComponentListener(object : ComponentAdapter() {
      override fun componentHidden(e: ComponentEvent) {
        (e.component as? Window)?.dispose()
      }
    })
    dialog.isVisible = true // blocks until user brings dialog down...
    recentPanel.repaint()
    val color = ok.color
    if (color != null) {
      label.setBackground(color)
    }
  }

  return JPanel().also {
    it.add(recentPanel)
    it.add(label)
    it.add(button)
    it.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createChooserPanels(
  cc: JColorChooser,
  recentPanel: RecentSwatchPanel,
): Array<AbstractColorChooserPanel> {
  // https://stackoverflow.com/questions/10793916/jcolorchooser-save-restore-recent-colors-in-swatches-panel
  val swatch = MySwatchChooserPanel()
  swatch.addPropertyChangeListener("ancestor") { event ->
    swatch.recentSwatchPanel?.colors?.also { colors ->
      recentPanel.colors?.also { rpc ->
        if (event.newValue == null) {
          // removed from the parent: save the recent colors
          System.arraycopy(colors, 0, rpc, 0, colors.size)
        } else {
          // added to the parent: restore the recent colors
          System.arraycopy(rpc, 0, colors, 0, colors.size)
        }
      }
    }
  }
  // getChooserPanels() returns a copy, so the default Swatches panel
  // (the first element) can be replaced directly
  val panels = cc.chooserPanels
  panels[0] = swatch
  return panels
}

private class ColorTracker(
  private val chooser: JColorChooser,
) : ActionListener {
  var color: Color? = null
    private set

  override fun actionPerformed(e: ActionEvent) {
    color = chooser.color
  }
}

// copied from javax/swing/colorchooser/DefaultSwatchChooserPanel.java
private class MySwatchChooserPanel : AbstractColorChooserPanel() {
  private var swatchPanel: SwatchPanel? = null
  var recentSwatchPanel: RecentSwatchPanel? = null
  private var mainSwatchListener: MouseListener? = null
  private var recentSwatchListener: MouseListener? = null
  private var mainSwatchKeyListener: KeyListener? = null
  private var recentSwatchKeyListener: KeyListener? = null

  init {
    inheritsPopupMenu = true
  }

  override fun getDisplayName(): String =
    UIManager.getString("ColorChooser.swatchesNameText", getLocale())

  override fun getSmallDisplayIcon() = null

  override fun getLargeDisplayIcon() = null

  override fun buildChooser() {
    val mainHolder = JPanel(BorderLayout())
    val outside = BorderFactory.createLineBorder(Color.BLACK)
    val inside = BorderFactory.createLineBorder(Color.WHITE)
    val border = BorderFactory.createCompoundBorder(outside, inside)
    mainHolder.setBorder(border)
    swatchPanel = MainSwatchPanel().also {
      it.putClientProperty(AccessibleContext.ACCESSIBLE_NAME_PROPERTY, displayName)
      it.setInheritsPopupMenu(true)
      mainSwatchKeyListener = MainSwatchKeyListener()
      mainSwatchListener = MainSwatchListener()
      it.addMouseListener(mainSwatchListener)
      it.addKeyListener(mainSwatchKeyListener)
      mainHolder.add(it)
    }

    val recentHolder = JPanel(BorderLayout())
    recentHolder.setBorder(border)
    recentHolder.setInheritsPopupMenu(true)
    val l = getLocale()
    val recentStr = UIManager.getString("ColorChooser.swatchesRecentText", l)
    recentSwatchPanel = RecentSwatchPanel().also {
      it.putClientProperty(AccessibleContext.ACCESSIBLE_NAME_PROPERTY, recentStr)
      recentSwatchListener = RecentSwatchListener()
      recentSwatchKeyListener = RecentSwatchKeyListener()
      it.addMouseListener(recentSwatchListener)
      it.addKeyListener(recentSwatchKeyListener)
      it.setInheritsPopupMenu(true)
      recentHolder.add(it)
    }

    val gbc = GridBagConstraints()
    gbc.anchor = GridBagConstraints.LAST_LINE_START
    gbc.gridwidth = 1
    gbc.gridheight = 2
    val oldInsets = gbc.insets
    gbc.insets = Insets(0, 0, 0, 10)
    val superHolder = JPanel(GridBagLayout())
    superHolder.add(mainHolder, gbc)
    gbc.insets = oldInsets

    val recentLabel = JLabel(recentStr)
    recentLabel.setLabelFor(recentSwatchPanel)
    gbc.gridwidth = GridBagConstraints.REMAINDER
    gbc.gridheight = 1
    gbc.weighty = 1.0
    superHolder.add(recentLabel, gbc)
    gbc.weighty = 0.0
    gbc.gridheight = GridBagConstraints.REMAINDER
    gbc.insets = Insets(0, 0, 0, 2)
    superHolder.add(recentHolder, gbc)
    superHolder.setInheritsPopupMenu(true)
    add(superHolder)
  }

  override fun uninstallChooserPanel(enclosingChooser: JColorChooser) {
    super.uninstallChooserPanel(enclosingChooser)
    swatchPanel?.removeMouseListener(mainSwatchListener)
    swatchPanel?.removeKeyListener(mainSwatchKeyListener)
    recentSwatchPanel?.removeMouseListener(recentSwatchListener)
    recentSwatchPanel?.removeKeyListener(recentSwatchKeyListener)
    swatchPanel = null
    recentSwatchPanel = null
    mainSwatchListener = null
    mainSwatchKeyListener = null
    recentSwatchListener = null
    recentSwatchKeyListener = null
    removeAll()
  }

  override fun updateChooser() {
    // empty
  }

  private fun updateModelColor(color: Color?) {
    val model = colorSelectionModel
    // An empty cell of the recent swatch has no color (null),
    // so ignore it to avoid setting null to the selection model
    if (model != null && color != null) {
      model.selectedColor = color
    }
  }

  private inner class RecentSwatchKeyListener : KeyAdapter() {
    override fun keyPressed(e: KeyEvent) {
      if (KeyEvent.VK_SPACE == e.keyCode) {
        val color = recentSwatchPanel?.selectedColor
        updateModelColor(color)
      }
    }
  }

  private inner class MainSwatchKeyListener : KeyAdapter() {
    override fun keyPressed(e: KeyEvent) {
      if (KeyEvent.VK_SPACE == e.keyCode) {
        val color = swatchPanel?.selectedColor
        updateModelColor(color)
        recentSwatchPanel?.setMostRecentColor(color)
      }
    }
  }

  private inner class RecentSwatchListener : MouseAdapter() {
    override fun mousePressed(e: MouseEvent) {
      if (isEnabled) {
        val color = recentSwatchPanel?.getColorForLocation(e.x, e.y)
        recentSwatchPanel?.setSelectedColorFromLocation(e.x, e.y)
        updateModelColor(color)
        recentSwatchPanel?.requestFocusInWindow()
      }
    }
  }

  private inner class MainSwatchListener : MouseAdapter() {
    override fun mousePressed(e: MouseEvent) {
      if (isEnabled) {
        val color = swatchPanel?.getColorForLocation(e.x, e.y)
        updateModelColor(color)
        swatchPanel?.setSelectedColorFromLocation(e.x, e.y)
        recentSwatchPanel?.setMostRecentColor(color)
        swatchPanel?.requestFocusInWindow()
      }
    }
  }
}

open class SwatchPanel : JPanel() {
  var colors: Array<Color?>? = null
  val swatchSize = Dimension()
  val numSwatches = Dimension()
  var gap = Dimension()
  private var selectedRow = 0
  private var selectedColumn = 0

  init {
    initValues()
    initColors()
    setToolTipText("") // register for events
    setOpaque(true)
    setBackground(Color.WHITE)
    setFocusable(true)
    setInheritsPopupMenu(true)
    addFocusListener(object : FocusAdapter() {
      override fun focusGained(e: FocusEvent) {
        repaint()
      }

      override fun focusLost(e: FocusEvent) {
        repaint()
      }
    })
    addKeyListener(object : KeyAdapter() {
      @Suppress("CognitiveComplexMethod")
      override fun keyPressed(e: KeyEvent) {
        val leftToRight = componentOrientation.isLeftToRight
        when (e.keyCode) {
          KeyEvent.VK_UP -> {
            if (selectedRow > 0) {
              selectedRow--
              repaint()
            }
          }

          KeyEvent.VK_DOWN -> {
            if (selectedRow < numSwatches.height - 1) {
              selectedRow++
              repaint()
            }
          }

          KeyEvent.VK_LEFT -> {
            if (selectedColumn > 0 && leftToRight) {
              selectedColumn--
              repaint()
            } else if (selectedColumn < numSwatches.width - 1 && !leftToRight) {
              selectedColumn++
              repaint()
            }
          }

          KeyEvent.VK_RIGHT -> {
            if (selectedColumn < numSwatches.width - 1 && leftToRight) {
              selectedColumn++
              repaint()
            } else if (selectedColumn > 0 && !leftToRight) {
              selectedColumn--
              repaint()
            }
          }

          KeyEvent.VK_HOME -> {
            selectedColumn = 0
            selectedRow = 0
            repaint()
          }

          KeyEvent.VK_END -> {
            selectedColumn = numSwatches.width - 1
            selectedRow = numSwatches.height - 1
            repaint()
          }

          else -> {
            // repaint()
          }
        }
      }
    })
  }

  open val selectedColor: Color?
    get() = getColorForCell(selectedColumn, selectedRow)

  open fun initValues() {
    // empty
  }

  override fun paintComponent(g: Graphics) {
    g.color = getBackground()
    g.fillRect(0, 0, width, height)
    val key = "ColorChooser.swatchesDefaultRecentColor"
    val defaultRecent = UIManager.getColor(key)
    val ltr = componentOrientation.isLeftToRight
    val sw = swatchSize.width
    val sh = swatchSize.height
    for (row in 0..<numSwatches.height) {
      val y = row * (sh + gap.height)
      for (column in 0..<numSwatches.width) {
        val c = getColorForCell(column, row) ?: defaultRecent
        g.color = c
        val x = if (ltr) {
          column * (sw + gap.width)
        } else {
          (numSwatches.width - column - 1) * (sw + gap.width)
        }
        g.fillRect(x, y, sw, sh)
        g.color = Color.BLACK
        g.drawLine(x + sw - 1, y, x + sw - 1, y + sh - 1)
        g.drawLine(x, y + sh - 1, x + sw - 1, y + sh - 1)
        if (selectedRow == row && selectedColumn == column && this.isFocusOwner) {
          val c2 = getFocusColor(c)
          g.color = c2
          g.drawLine(x, y, x + sw - 1, y)
          g.drawLine(x, y, x, y + sh - 1)
          g.drawLine(x + sw - 1, y, x + sw - 1, y + sh - 1)
          g.drawLine(x, y + sh - 1, x + sw - 1, y + sh - 1)
          g.drawLine(x, y, x + sw - 1, y + sh - 1)
          g.drawLine(x, y + sh - 1, x + sw - 1, y)
        }
      }
    }
  }

  override fun getPreferredSize(): Dimension {
    val x = numSwatches.width * (swatchSize.width + gap.width) - 1
    val y = numSwatches.height * (swatchSize.height + gap.height) - 1
    return Dimension(x, y)
  }

  open fun initColors() {
    // empty
  }

  override fun getToolTipText(e: MouseEvent) = getColorForLocation(e.x, e.y)?.let {
    it.red.toString() + ", " + it.green + ", " + it.blue
  }

  fun setSelectedColorFromLocation(
    x: Int,
    y: Int,
  ) {
    val column = getColumnForLocation(x)
    val row = getRowForLocation(y)
    if (isValidCell(column, row)) {
      selectedColumn = column
      selectedRow = row
      repaint()
    }
  }

  fun getColorForLocation(
    x: Int,
    y: Int,
  ) = getColorForCell(getColumnForLocation(x), getRowForLocation(y))

  private fun getColumnForLocation(x: Int): Int {
    val column = x / (swatchSize.width + gap.width)
    return if (componentOrientation.isLeftToRight) {
      column
    } else {
      numSwatches.width - column - 1
    }
  }

  private fun getRowForLocation(y: Int) = y / (swatchSize.height + gap.height)

  private fun isValidCell(
    column: Int,
    row: Int,
  ) = column in 0..<numSwatches.width && row in 0..<numSwatches.height

  // The panel can be larger than its preferred size, so the location
  // may be outside of the swatch grid
  private fun getColorForCell(
    column: Int,
    row: Int,
  ) = colors?.takeIf { isValidCell(column, row) }?.let {
    it[row * numSwatches.width + column]
  }

  companion object {
    private fun getFocusColor(c: Color): Color {
      val r = if (c.red < 125) 255 else 0
      val g = if (c.green < 125) 255 else 0
      val b = if (c.blue < 125) 255 else 0
      return Color(r, g, b)
    }
  }
}

private class RecentSwatchPanel : SwatchPanel() {
  override fun initValues() {
    val key = "ColorChooser.swatchesRecentSwatchSize"
    swatchSize.size = UIManager.getDimension(key)
    numSwatches.setSize(5, 7)
    gap.setSize(1, 1)
  }

  override fun initColors() {
    // Keep the recent colors when updateUI() is called again (e.g. Look and Feel change).
    // Empty cells are left null and painted with "ColorChooser.swatchesDefaultRecentColor".
    if (colors == null) {
      colors = arrayOfNulls(numSwatches.width * numSwatches.height)
    }
  }

  fun setMostRecentColor(c: Color?) {
    val a = colors
    if (c != null && a != null) {
      System.arraycopy(a, 0, a, 1, a.size - 1)
      a[0] = c
      repaint()
    }
  }
}

private class MainSwatchPanel : SwatchPanel() {
  override fun initValues() {
    val key = "ColorChooser.swatchesSwatchSize"
    swatchSize.size = UIManager.getDimension(key, getLocale())
    numSwatches.setSize(31, 9)
    gap.setSize(1, 1)
  }

  override fun initColors() {
    val numColors = RAW_COLOR_VALUES.size / 3
    colors = Array<Color?>(numColors) { i ->
      val v = i * 3
      Color(RAW_COLOR_VALUES[v], RAW_COLOR_VALUES[v + 1], RAW_COLOR_VALUES[v + 2])
    }
  }

  companion object {
    @Suppress("LongMethod", "ktlint:standard:argument-list-wrapping")
    val RAW_COLOR_VALUES = intArrayOf(
      // first row.
      255, 255, 255,
      204, 255, 255,
      204, 204, 255,
      204, 204, 255,
      204, 204, 255,
      204, 204, 255,
      204, 204, 255,
      204, 204, 255,
      204, 204, 255,
      204, 204, 255,
      204, 204, 255,
      255, 204, 255,
      255, 204, 204,
      255, 204, 204,
      255, 204, 204,
      255, 204, 204,
      255, 204, 204,
      255, 204, 204,
      255, 204, 204,
      255, 204, 204,
      255, 204, 204,
      255, 255, 204,
      204, 255, 204,
      204, 255, 204,
      204, 255, 204,
      204, 255, 204,
      204, 255, 204,
      204, 255, 204,
      204, 255, 204,
      204, 255, 204,
      204, 255, 204,
      // second row.
      204, 204, 204,
      153, 255, 255,
      153, 204, 255,
      153, 153, 255,
      153, 153, 255,
      153, 153, 255,
      153, 153, 255,
      153, 153, 255,
      153, 153, 255,
      153, 153, 255,
      204, 153, 255,
      255, 153, 255,
      255, 153, 204,
      255, 153, 153,
      255, 153, 153,
      255, 153, 153,
      255, 153, 153,
      255, 153, 153,
      255, 153, 153,
      255, 153, 153,
      255, 204, 153,
      255, 255, 153,
      204, 255, 153,
      153, 255, 153,
      153, 255, 153,
      153, 255, 153,
      153, 255, 153,
      153, 255, 153,
      153, 255, 153,
      153, 255, 153,
      153, 255, 204,
      // third row
      204, 204, 204,
      102, 255, 255,
      102, 204, 255,
      102, 153, 255,
      102, 102, 255,
      102, 102, 255,
      102, 102, 255,
      102, 102, 255,
      102, 102, 255,
      153, 102, 255,
      204, 102, 255,
      255, 102, 255,
      255, 102, 204,
      255, 102, 153,
      255, 102, 102,
      255, 102, 102,
      255, 102, 102,
      255, 102, 102,
      255, 102, 102,
      255, 153, 102,
      255, 204, 102,
      255, 255, 102,
      204, 255, 102,
      153, 255, 102,
      102, 255, 102,
      102, 255, 102,
      102, 255, 102,
      102, 255, 102,
      102, 255, 102,
      102, 255, 153,
      102, 255, 204,
      // fourth row
      153, 153, 153,
      51, 255, 255,
      51, 204, 255,
      51, 153, 255,
      51, 102, 255,
      51, 51, 255,
      51, 51, 255,
      51, 51, 255,
      102, 51, 255,
      153, 51, 255,
      204, 51, 255,
      255, 51, 255,
      255, 51, 204,
      255, 51, 153,
      255, 51, 102,
      255, 51, 51,
      255, 51, 51,
      255, 51, 51,
      255, 102, 51,
      255, 153, 51,
      255, 204, 51,
      255, 255, 51,
      204, 255, 51,
      153, 255, 51,
      102, 255, 51,
      51, 255, 51,
      51, 255, 51,
      51, 255, 51,
      51, 255, 102,
      51, 255, 153,
      51, 255, 204,
      // Fifth row
      153, 153, 153,
      0, 255, 255,
      0, 204, 255,
      0, 153, 255,
      0, 102, 255,
      0, 51, 255,
      0, 0, 255,
      51, 0, 255,
      102, 0, 255,
      153, 0, 255,
      204, 0, 255,
      255, 0, 255,
      255, 0, 204,
      255, 0, 153,
      255, 0, 102,
      255, 0, 51,
      255, 0, 0,
      255, 51, 0,
      255, 102, 0,
      255, 153, 0,
      255, 204, 0,
      255, 255, 0,
      204, 255, 0,
      153, 255, 0,
      102, 255, 0,
      51, 255, 0,
      0, 255, 0,
      0, 255, 51,
      0, 255, 102,
      0, 255, 153,
      0, 255, 204,
      // sixth row
      102, 102, 102,
      0, 204, 204,
      0, 204, 204,
      0, 153, 204,
      0, 102, 204,
      0, 51, 204,
      0, 0, 204,
      51, 0, 204,
      102, 0, 204,
      153, 0, 204,
      204, 0, 204,
      204, 0, 204,
      204, 0, 204,
      204, 0, 153,
      204, 0, 102,
      204, 0, 51,
      204, 0, 0,
      204, 51, 0,
      204, 102, 0,
      204, 153, 0,
      204, 204, 0,
      204, 204, 0,
      204, 204, 0,
      153, 204, 0,
      102, 204, 0,
      51, 204, 0,
      0, 204, 0,
      0, 204, 51,
      0, 204, 102,
      0, 204, 153,
      0, 204, 204,
      // seventh row
      102, 102, 102,
      0, 153, 153,
      0, 153, 153,
      0, 153, 153,
      0, 102, 153,
      0, 51, 153,
      0, 0, 153,
      51, 0, 153,
      102, 0, 153,
      153, 0, 153,
      153, 0, 153,
      153, 0, 153,
      153, 0, 153,
      153, 0, 153,
      153, 0, 102,
      153, 0, 51,
      153, 0, 0,
      153, 51, 0,
      153, 102, 0,
      153, 153, 0,
      153, 153, 0,
      153, 153, 0,
      153, 153, 0,
      153, 153, 0,
      102, 153, 0,
      51, 153, 0,
      0, 153, 0,
      0, 153, 51,
      0, 153, 102,
      0, 153, 153,
      0, 153, 153,
      // eighth row
      51, 51, 51,
      0, 102, 102,
      0, 102, 102,
      0, 102, 102,
      0, 102, 102,
      0, 51, 102,
      0, 0, 102,
      51, 0, 102,
      102, 0, 102,
      102, 0, 102,
      102, 0, 102,
      102, 0, 102,
      102, 0, 102,
      102, 0, 102,
      102, 0, 102,
      102, 0, 51,
      102, 0, 0,
      102, 51, 0,
      102, 102, 0,
      102, 102, 0,
      102, 102, 0,
      102, 102, 0,
      102, 102, 0,
      102, 102, 0,
      102, 102, 0,
      51, 102, 0,
      0, 102, 0,
      0, 102, 51,
      0, 102, 102,
      0, 102, 102,
      0, 102, 102,
      // ninth row
      0, 0, 0,
      0, 51, 51,
      0, 51, 51,
      0, 51, 51,
      0, 51, 51,
      0, 51, 51,
      0, 0, 51,
      51, 0, 51,
      51, 0, 51,
      51, 0, 51,
      51, 0, 51,
      51, 0, 51,
      51, 0, 51,
      51, 0, 51,
      51, 0, 51,
      51, 0, 51,
      51, 0, 0,
      51, 51, 0,
      51, 51, 0,
      51, 51, 0,
      51, 51, 0,
      51, 51, 0,
      51, 51, 0,
      51, 51, 0,
      51, 51, 0,
      0, 51, 0,
      0, 51, 51,
      0, 51, 51,
      0, 51, 51,
      0, 51, 51,
      51, 51, 51,
    )
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
