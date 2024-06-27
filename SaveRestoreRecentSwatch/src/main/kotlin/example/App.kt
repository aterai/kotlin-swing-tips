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
import javax.swing.border.Border
import javax.swing.colorchooser.AbstractColorChooserPanel

fun makeUI(): Component {
  val label = object : JLabel() {
    override fun getPreferredSize() = Dimension(32, 32)
  }
  label.setOpaque(true)
  label.setBackground(Color.WHITE)

  val switchPanel = RecentSwatchPanel()
  switchPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK))
  switchPanel.colors?.also {
    it[0] = Color.RED
    it[1] = Color.GREEN
    it[2] = Color.BLUE
  }

  val button = JButton("open JColorChooser")
  button.addActionListener {
    val cc = JColorChooser()
    val panels = cc.chooserPanels
    val choosers = panels.toMutableList()
    choosers.removeAt(0)
    val swatch = MySwatchChooserPanel()
    swatch.addPropertyChangeListener("ancestor") { event ->
      swatch.recentSwatchPanel?.colors?.also { colors ->
        switchPanel.colors?.also { spc ->
          if (event.newValue == null) {
            System.arraycopy(colors, 0, spc, 0, colors.size)
          } else {
            System.arraycopy(spc, 0, colors, 0, colors.size)
          }
        }
      }
    }
    choosers.add(0, swatch)
    cc.setChooserPanels(choosers.toTypedArray<AbstractColorChooserPanel>())
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
    switchPanel.repaint()
    val color = ok.color
    if (color != null) {
      label.setBackground(color)
    }
  }

  return JPanel().also {
    it.add(switchPanel)
    it.add(label)
    it.add(button)
    it.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10))
    it.preferredSize = Dimension(320, 240)
  }
}

private class ColorTracker(private val chooser: JColorChooser) : ActionListener {
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

  override fun getDisplayName() =
    UIManager.getString("ColorChooser.swatchesNameText", getLocale())

  override fun getSmallDisplayIcon() = null

  override fun getLargeDisplayIcon() = null

  override fun buildChooser() {
    val mainHolder = JPanel(BorderLayout())
    val outside = BorderFactory.createLineBorder(Color.BLACK)
    val inside = BorderFactory.createLineBorder(Color.WHITE)
    val border: Border = BorderFactory.createCompoundBorder(outside, inside)
    mainHolder.setBorder(border)
    swatchPanel = MainSwatchPanel().also {
      it.putClientProperty(AccessibleContext.ACCESSIBLE_NAME_PROPERTY, displayName)
      it.setInheritsPopupMenu(true)
      mainSwatchKeyListener = MainSwatchKeyListener()
      mainSwatchListener = MainSwatchListener()
      it.addMouseListener(mainSwatchListener)
      it.addKeyListener(mainSwatchKeyListener)
      mainHolder.add(it, BorderLayout.CENTER)
    }

    val recentHolder = JPanel(BorderLayout())
    recentHolder.setBorder(border)
    recentHolder.setInheritsPopupMenu(true)
    val recentStr = UIManager.getString("ColorChooser.swatchesRecentText", getLocale())
    recentSwatchPanel = RecentSwatchPanel().also {
      it.putClientProperty(AccessibleContext.ACCESSIBLE_NAME_PROPERTY, recentStr)
      recentSwatchListener = RecentSwatchListener()
      recentSwatchKeyListener = RecentSwatchKeyListener()
      it.addMouseListener(recentSwatchListener)
      it.addKeyListener(recentSwatchKeyListener)
      it.setInheritsPopupMenu(true)
      recentHolder.add(it, BorderLayout.CENTER)
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

    val l = JLabel(recentStr)
    l.setLabelFor(recentSwatchPanel)
    gbc.gridwidth = GridBagConstraints.REMAINDER
    gbc.gridheight = 1
    gbc.weighty = 1.0
    superHolder.add(l, gbc)
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

  fun setSelectedColor2(color: Color?) {
    val model = colorSelectionModel
    if (model != null) {
      model.selectedColor = color
    }
  }

  private inner class RecentSwatchKeyListener : KeyAdapter() {
    override fun keyPressed(e: KeyEvent) {
      if (KeyEvent.VK_SPACE == e.keyCode) {
        val color = recentSwatchPanel?.selectedColor
        setSelectedColor2(color)
      }
    }
  }

  private inner class MainSwatchKeyListener : KeyAdapter() {
    override fun keyPressed(e: KeyEvent) {
      if (KeyEvent.VK_SPACE == e.keyCode) {
        val color = swatchPanel?.selectedColor
        setSelectedColor2(color)
        recentSwatchPanel?.setMostRecentColor(color)
      }
    }
  }

  private inner class RecentSwatchListener : MouseAdapter() {
    override fun mousePressed(e: MouseEvent) {
      if (isEnabled) {
        val color = recentSwatchPanel?.getColorForLocation(e.x, e.y)
        recentSwatchPanel?.setSelectedColorFromLocation(e.x, e.y)
        setSelectedColor2(color)
        recentSwatchPanel?.requestFocusInWindow()
      }
    }
  }

  private inner class MainSwatchListener : MouseAdapter() {
    override fun mousePressed(e: MouseEvent) {
      if (isEnabled) {
        val color = swatchPanel?.getColorForLocation(e.x, e.y)
        setSelectedColor2(color)
        swatchPanel?.setSelectedColorFromLocation(e.x, e.y)
        recentSwatchPanel?.setMostRecentColor(color)
        swatchPanel?.requestFocusInWindow()
      }
    }
  }
}

open class SwatchPanel : JPanel() {
  var colors: Array<Color>? = null
  val swatchSize = Dimension()
  val numSwatches = Dimension()
  var gap = Dimension()
  private var selRow = 0
  private var selCol = 0

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
            if (selRow > 0) {
              selRow--
              repaint()
            }
          }

          KeyEvent.VK_DOWN -> {
            if (selRow < numSwatches.height - 1) {
              selRow++
              repaint()
            }
          }

          KeyEvent.VK_LEFT -> {
            if (selCol > 0 && leftToRight) {
              selCol--
              repaint()
            } else if (selCol < numSwatches.width - 1 && !leftToRight) {
              selCol++
              repaint()
            }
          }

          KeyEvent.VK_RIGHT -> {
            if (selCol < numSwatches.width - 1 && leftToRight) {
              selCol++
              repaint()
            } else if (selCol > 0 && !leftToRight) {
              selCol--
              repaint()
            }
          }

          KeyEvent.VK_HOME -> {
            selCol = 0
            selRow = 0
            repaint()
          }

          KeyEvent.VK_END -> {
            selCol = numSwatches.width - 1
            selRow = numSwatches.height - 1
            repaint()
          }

          else -> {
            // repaint()
          }
        }
      }
    })
  }

  open val selectedColor: Color
    get() = getColorForCell(selCol, selRow)

  open fun initValues() {
    // empty
  }

  public override fun paintComponent(g: Graphics) {
    g.color = getBackground()
    g.fillRect(0, 0, width, height)
    for (row in 0..<numSwatches.height) {
      val y = row * (swatchSize.height + gap.height)
      for (column in 0..<numSwatches.width) {
        val c = getColorForCell(column, row)
        g.color = c
        val x = if (componentOrientation.isLeftToRight) {
          column * (swatchSize.width + gap.width)
        } else {
          (numSwatches.width - column - 1) * (swatchSize.width + gap.width)
        }
        g.fillRect(x, y, swatchSize.width, swatchSize.height)
        g.color = Color.BLACK
        g.drawLine(
          x + swatchSize.width - 1,
          y,
          x + swatchSize.width - 1,
          y + swatchSize.height - 1,
        )
        g.drawLine(
          x,
          y + swatchSize.height - 1,
          x + swatchSize.width - 1,
          y + swatchSize.height - 1,
        )
        if (selRow == row && selCol == column && this.isFocusOwner) {
          val c2 = getFocusColor(c)
          g.color = c2
          g.drawLine(x, y, x + swatchSize.width - 1, y)
          g.drawLine(x, y, x, y + swatchSize.height - 1)
          g.drawLine(
            x + swatchSize.width - 1,
            y,
            x + swatchSize.width - 1,
            y + swatchSize.height - 1,
          )
          g.drawLine(
            x,
            y + swatchSize.height - 1,
            x + swatchSize.width - 1,
            y + swatchSize.height - 1,
          )
          g.drawLine(x, y, x + swatchSize.width - 1, y + swatchSize.height - 1)
          g.drawLine(x, y + swatchSize.height - 1, x + swatchSize.width - 1, y)
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

  override fun getToolTipText(e: MouseEvent): String {
    val color = getColorForLocation(e.x, e.y)
    return color.red.toString() + ", " + color.green + ", " + color.blue
  }

  fun setSelectedColorFromLocation(
    x: Int,
    y: Int,
  ) {
    selCol = if (componentOrientation.isLeftToRight) {
      x / (swatchSize.width + gap.width)
    } else {
      numSwatches.width - x / (swatchSize.width + gap.width) - 1
    }
    selRow = y / (swatchSize.height + gap.height)
    repaint()
  }

  fun getColorForLocation(
    x: Int,
    y: Int,
  ): Color {
    val column = if (componentOrientation.isLeftToRight) {
      x / (swatchSize.width + gap.width)
    } else {
      numSwatches.width - x / (swatchSize.width + gap.width) - 1
    }
    val row = y / (swatchSize.height + gap.height)
    return getColorForCell(column, row)
  }

  private fun getColorForCell(column: Int, row: Int) = colors?.let {
    it[row * numSwatches.width + column]
  } ?: Color.RED

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
    swatchSize.size = UIManager.getDimension("ColorChooser.swatchesRecentSwatchSize")
    numSwatches.setSize(5, 7)
    gap.setSize(1, 1)
  }

  override fun initColors() {
    val defaultRecent = UIManager.getColor("ColorChooser.swatchesDefaultRecentColor")
    val numColors = numSwatches.width * numSwatches.height
    colors = Array<Color>(numColors) { defaultRecent }
  }

  fun setMostRecentColor(c: Color?) {
    colors?.also {
      System.arraycopy(it, 0, it, 1, it.size - 1)
      it[0] = c ?: Color.RED
    }
    repaint()
  }
}

private class MainSwatchPanel : SwatchPanel() {
  override fun initValues() {
    swatchSize.size = UIManager.getDimension("ColorChooser.swatchesSwatchSize", getLocale())
    numSwatches.setSize(31, 9)
    gap.setSize(1, 1)
  }

  override fun initColors() {
    val rawValues = initRawValues()
    val numColors = rawValues.size / 3
    colors = Array<Color>(numColors) { _ -> Color.RED }.also {
      for (i in 0..<numColors) {
        it[i] = Color(rawValues[i * 3], rawValues[i * 3 + 1], rawValues[i * 3 + 2])
      }
    }
  }

  @Suppress("LongMethod", "ktlint:standard:argument-list-wrapping")
  private fun initRawValues(): IntArray {
    return intArrayOf(
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
