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

private val HELP = """
  Start editing: Double-Click
  Commit rename: field-focusLost, Enter-Key
  Cancel editing: Esc-Key, title.isEmpty
""".trimIndent()

fun makeUI(): Component {
  val l1 = object : JScrollPane(JTree()) {
    override fun updateUI() {
      val title = (border as? TitledBorder)?.title ?: "JTree 111111111111111"
      border = null
      super.updateUI()
      border = EditableTitledBorder(title, this)
    }
  }

  val l2 = object : JScrollPane(JTextArea(HELP)) {
    override fun updateUI() {
      val title = (border as? TitledBorder)?.title ?: "JTextArea"
      border = null
      super.updateUI()
      border = EditableTitledBorder(null, title, TitledBorder.RIGHT, TitledBorder.BOTTOM, this)
    }
  }

  return JPanel(GridLayout(0, 1, 5, 5)).also {
    it.add(l1)
    it.add(l2)
    it.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
    it.preferredSize = Dimension(320, 240)
  }
}

private class EditableTitledBorder(
  border: Border?,
  title: String,
  justification: Int,
  pos: Int,
  font: Font?,
  var comp: Component
) : TitledBorder(border, title, justification, pos, font) {
  private val editor = JTextField()
  private val renderer = JLabel()
  private val rect = Rectangle()
  private val startEditing = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      (comp as? JComponent)?.rootPane?.glassPane = glassPane
      glassPane.removeAll()
      glassPane.add(editor)
      glassPane.isVisible = true
      val p = SwingUtilities.convertPoint(comp, rect.location, glassPane)
      rect.location = p
      rect.grow(2, 2)
      editor.bounds = rect
      editor.text = getTitle()
      editor.selectAll()
      editor.requestFocusInWindow()
    }
  }
  private val cancelEditing = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      glassPane.isVisible = false
    }
  }
  private val renameTitle = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      val str = editor.text.trim()
      if (str.isNotEmpty()) {
        setTitle(str)
      }
      glassPane.isVisible = false
    }
  }
  private val glassPane: Container = object : JComponent() {
    private var listener: MouseListener? = null
    override fun updateUI() {
      super.updateUI()
      focusTraversalPolicy = object : DefaultFocusTraversalPolicy() {
        public override fun accept(c: Component) = c == editor
      }
      listener = object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
          if (!editor.bounds.contains(e.point)) {
            renameTitle.actionPerformed(ActionEvent(e.component, ActionEvent.ACTION_PERFORMED, ""))
          }
        }
      }
      addMouseListener(listener)
      isOpaque = false
    }

    override fun setVisible(flag: Boolean) {
      super.setVisible(flag)
      isFocusTraversalPolicyProvider = flag
      isFocusCycleRoot = flag
    }
  }

  init {
    comp.addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        val isDoubleClick = e.clickCount >= 2
        if (isDoubleClick) {
          val src = e.component
          rect.bounds = getTitleBounds(src)
          if (rect.contains(e.point)) {
            startEditing.actionPerformed(ActionEvent(src, ActionEvent.ACTION_PERFORMED, ""))
          }
        }
      }
    })
    val im = editor.getInputMap(JComponent.WHEN_FOCUSED)
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "rename-title")
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel-editing")
    val am = editor.actionMap
    am.put("rename-title", renameTitle)
    am.put("cancel-editing", cancelEditing)
  }

  constructor(title: String, c: Component) : this(null, title, LEADING, DEFAULT_POSITION, null, c)

  constructor(
    border: Border?,
    title: String,
    justification: Int,
    pos: Int,
    c: Component
  ) : this(border, title, justification, pos, null, c)

  override fun isBorderOpaque() = true

  // private fun getLabel2(c: Component): JLabel {
  //   renderer.text = getTitle()
  //   renderer.font = getFont(c)
  //   renderer.componentOrientation = c.componentOrientation
  //   renderer.isEnabled = c.isEnabled
  //   return renderer
  // }

  private fun getJustification2(c: Component): Int {
    val justification = getTitleJustification()
    if (justification == LEADING || justification == DEFAULT_JUSTIFICATION) {
      return if (c.componentOrientation.isLeftToRight) LEFT else RIGHT
    }
    return if (justification == TRAILING) {
      if (c.componentOrientation.isLeftToRight) RIGHT else LEFT
    } else {
      justification
    }
  }

  private fun getTitleBounds(c: Component): Rectangle {
    // if (getTitle()?.isNotEmpty() == true) {
    val border = getBorder()
    val edge = if (border is TitledBorder) 0 else EDGE_SPACING
    val i = getBorderInsets(border, c)
    val label = renderer.also {
      it.text = getTitle()
      it.font = getFont(c)
      it.componentOrientation = c.componentOrientation
      it.isEnabled = c.isEnabled
    }
    val size = label.preferredSize
    val r = Rectangle(c.width - i.left - i.right, size.height)
    calcLabelPosition(c, edge, i, r)
    calcLabelJustification(c, size, i, r)
    return r
  }

  private fun calcLabelPosition(c: Component, edge: Int, insets: Insets, lblR: Rectangle) {
    when (getTitlePosition()) {
      ABOVE_TOP -> {
        insets.left = 0
        insets.right = 0
      }
      TOP -> {
        insets.top = edge + insets.top / 2 - lblR.height / 2
        if (insets.top >= edge) {
          lblR.y += insets.top
        }
      }
      BELOW_TOP -> lblR.y += insets.top + edge
      ABOVE_BOTTOM -> lblR.y += c.height - lblR.height - insets.bottom - edge
      BOTTOM -> {
        lblR.y += c.height - lblR.height
        insets.bottom = edge + (insets.bottom - lblR.height) / 2
        if (insets.bottom >= edge) {
          lblR.y -= insets.bottom
        }
      }
      BELOW_BOTTOM -> {
        insets.left = 0
        insets.right = 0
        lblR.y += c.height - lblR.height
      }
    }
    insets.left += edge + TEXT_INSET_H
    insets.right += edge + TEXT_INSET_H
  }

  private fun calcLabelJustification(
    c: Component,
    size: Dimension,
    insets: Insets,
    lblR: Rectangle
  ) {
    if (lblR.width > size.width) {
      lblR.width = size.width
    }
    when (getJustification2(c)) {
      LEFT -> lblR.x += insets.left
      RIGHT -> lblR.x += c.width - insets.right - lblR.width
      CENTER -> lblR.x += (c.width - lblR.width) / 2
    }
  }

  private fun getBorderInsets(border: Border?, c: Component): Insets {
    var ins = Insets(0, 0, 0, 0)
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
