package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.border.AbstractBorder
import javax.swing.border.Border
import javax.swing.border.TitledBorder

private const val HELP = """Start editing: Double-Click
Commit rename: field-focusLost, Enter-Key
Cancel editing: Esc-Key, title.isEmpty
"""

fun makeUI(): Component {
  val l1 = JScrollPane(JTree())
  l1.border = EditableTitledBorder("JTree 111111111111111", l1)

  val l2 = JScrollPane(JTextArea(HELP))
  l2.border = EditableTitledBorder(null, "JTextArea", TitledBorder.RIGHT, TitledBorder.BOTTOM, l2)

  return JPanel(GridLayout(0, 1, 5, 5)).also {
    it.add(l1)
    it.add(l2)
    it.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
    it.preferredSize = Dimension(320, 240)
  }
}

private class EditableTitledBorder(
  border: Border?,
  title: String?,
  justification: Int,
  pos: Int,
  font: Font?,
  color: Color?,
  var comp: Component
) : TitledBorder(border, title, justification, pos, font, color), MouseListener {
  private val glassPane: Container = EditorGlassPane()
  private val editorTextField = JTextField()
  private val dummy = JLabel()
  private val rect = Rectangle()
  private val startEditing: Action = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      (comp as? JComponent)?.rootPane?.glassPane = glassPane
      glassPane.removeAll()
      glassPane.add(editorTextField)
      glassPane.isVisible = true
      val p = SwingUtilities.convertPoint(comp, rect.location, glassPane)
      rect.location = p
      rect.grow(2, 2)
      editorTextField.bounds = rect
      editorTextField.text = getTitle()
      editorTextField.selectAll()
      editorTextField.requestFocusInWindow()
    }
  }
  private val cancelEditing = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      glassPane.isVisible = false
    }
  }
  private val renameTitle = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      if (editorTextField.text.trim().isNotEmpty()) {
        setTitle(editorTextField.text)
      }
      glassPane.isVisible = false
    }
  }

  constructor(title: String?, c: Component) : this(null, title, LEADING, DEFAULT_POSITION, null, null, c)

  constructor(
    border: Border?,
    title: String?,
    justification: Int,
    pos: Int,
    c: Component
  ) : this(border, title, justification, pos, null, null, c)

  override fun isBorderOpaque() = true

  private fun getLabel(c: Component): JLabel {
    dummy.text = getTitle()
    dummy.font = getFont(c)
    dummy.componentOrientation = c.componentOrientation
    dummy.isEnabled = c.isEnabled
    return dummy
  }

  private fun getJustification(c: Component): Int {
    val justification = getTitleJustification()
    if (justification == LEADING || justification == DEFAULT_JUSTIFICATION) {
      return if (c.componentOrientation.isLeftToRight) LEFT else RIGHT
    }
    return if (justification == TRAILING) {
      if (c.componentOrientation.isLeftToRight) RIGHT else LEFT
    } else justification
  }

  private fun getTitleBounds(c: Component, width: Int, height: Int): Rectangle {
    val title = getTitle()
    if (title?.isNotEmpty() == true) {
      val border = getBorder()
      val edge = if (border is TitledBorder) 0 else EDGE_SPACING
      val label = getLabel(c)
      val size = label.preferredSize
      val insets = makeBorderInsets(border, c, Insets(0, 0, 0, 0))
      var labelY = 0
      val labelH = size.height
      when (getTitlePosition()) {
        ABOVE_TOP -> {
          insets.left = 0
          insets.right = 0
        }
        TOP -> {
          insets.top = edge + insets.top / 2 - labelH / 2
          if (insets.top >= edge) {
            labelY += insets.top
          }
        }
        BELOW_TOP -> labelY += insets.top + edge
        ABOVE_BOTTOM -> labelY += height - labelH - insets.bottom - edge
        BOTTOM -> {
          labelY += height - labelH
          insets.bottom = edge + (insets.bottom - labelH) / 2
          if (insets.bottom >= edge) {
            labelY -= insets.bottom
          }
        }
        BELOW_BOTTOM -> {
          insets.left = 0
          insets.right = 0
          labelY += height - labelH
        }
      }
      insets.left += edge + TEXT_INSET_H
      insets.right += edge + TEXT_INSET_H
      var labelX = 0
      var labelW = width - insets.left - insets.right
      if (labelW > size.width) {
        labelW = size.width
      }
      when (getJustification(c)) {
        LEFT -> labelX += insets.left
        RIGHT -> labelX += width - insets.right - labelW
        CENTER -> labelX += (width - labelW) / 2
      }
      return Rectangle(labelX, labelY, labelW, labelH)
    }
    return Rectangle()
  }

  override fun mouseClicked(e: MouseEvent) {
    val isDoubleClick = e.clickCount >= 2
    if (isDoubleClick) {
      val src = e.component
      val dim = src.size
      rect.bounds = getTitleBounds(src, dim.width, dim.height)
      if (rect.contains(e.point)) {
        startEditing.actionPerformed(ActionEvent(src, ActionEvent.ACTION_PERFORMED, ""))
      }
    }
  }

  override fun mouseEntered(e: MouseEvent) {
    /* not needed */
  }

  override fun mouseExited(e: MouseEvent) {
    /* not needed */
  }

  override fun mousePressed(e: MouseEvent) {
    /* not needed */
  }

  override fun mouseReleased(e: MouseEvent) {
    /* not needed */
  }

  private inner class EditorGlassPane : JComponent() {
    override fun setVisible(flag: Boolean) {
      super.setVisible(flag)
      isFocusTraversalPolicyProvider = flag
      isFocusCycleRoot = flag
    }

    init {
      isOpaque = false
      focusTraversalPolicy = object : DefaultFocusTraversalPolicy() {
        public override fun accept(c: Component) = c == editorTextField
      }
      addMouseListener(object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
          if (!editorTextField.bounds.contains(e.point)) {
            renameTitle.actionPerformed(ActionEvent(e.component, ActionEvent.ACTION_PERFORMED, ""))
          }
        }
      })
    }
  }

  companion object {
    private fun makeBorderInsets(border: Border?, c: Component, insets: Insets): Insets {
      var ins = insets
      when (border) {
        null -> ins.set(0, 0, 0, 0)
        is AbstractBorder -> ins = border.getBorderInsets(c, ins)
        else -> {
          val i = border.getBorderInsets(c)
          ins.set(i.top, i.left, i.bottom, i.right)
        }
      }
      return ins
    }
  }

  init {
    comp.addMouseListener(this)
    val im = editorTextField.getInputMap(JComponent.WHEN_FOCUSED)
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "rename-title")
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel-editing")
    val am = editorTextField.actionMap
    am.put("rename-title", renameTitle)
    am.put("cancel-editing", cancelEditing)
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
