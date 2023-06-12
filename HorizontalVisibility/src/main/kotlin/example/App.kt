package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.plaf.basic.BasicScrollBarUI

private val TEXT = listOf(
  """javascript:(function(){""",
  """var%20l=location,m=l.href.match("^https?://(.+)(api[^+]+|technotes[^+]+)");""",
  """if(m)l.href='https://docs.oracle.com/javase/8/docs/'""",
  """+decodeURIComponent(m[2]).replace(/\+.*$/,'').replace(/\[\]/g,':A')""",
  """.replace(/,%20|\(|\)/g,'-');}());"""
).joinToString(separator = "")

private val textField1 = JTextField(TEXT)
private val scroller1 = JScrollBar(Adjustable.HORIZONTAL)

private val textField2 = JTextField(TEXT)
private val scroller2 = object : JScrollBar(Adjustable.HORIZONTAL) {
  override fun updateUI() {
    super.updateUI()
    setUI(ArrowButtonlessScrollBarUI())
  }

  override fun getPreferredSize(): Dimension {
    val d = super.getPreferredSize()
    d.height = 10
    return d
  }
}

fun makeUI(): Component {
  scroller1.model = textField1.horizontalVisibility
  val handler = EmptyThumbHandler(textField1, scroller1)

  scroller2.model = textField2.horizontalVisibility

  val check = JCheckBox("add EmptyThumbHandler")
  check.addActionListener { e ->
    if ((e.source as? JCheckBox)?.isSelected == true) {
      textField1.addComponentListener(handler)
      textField1.document.addDocumentListener(handler)
    } else {
      textField1.removeComponentListener(handler)
      textField1.document.removeDocumentListener(handler)
    }
  }

  val caretButton = JButton("setCaretPosition: 0")
  caretButton.addActionListener {
    textField1.requestFocusInWindow()
    textField1.caretPosition = 0
    scroller1.revalidate()
    textField2.requestFocusInWindow()
    textField2.caretPosition = 0
    scroller2.revalidate()
  }

  val offsetButton = JButton("setScrollOffset: 0")
  offsetButton.addActionListener {
    textField1.scrollOffset = 0
    scroller1.revalidate()
    textField2.scrollOffset = 0
    scroller2.revalidate()
  }

  val scroll = JScrollPane(JTextField(TEXT))
  scroll.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
  scroll.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS

  val p = Box.createVerticalBox()
  p.add(JLabel("JScrollPane + VERTICAL_SCROLLBAR_NEVER"))
  p.add(scroll)
  p.add(Box.createVerticalStrut(5))
  p.add(JLabel("BoundedRangeModel: textField.getHorizontalVisibility()"))
  p.add(textField1)
  p.add(Box.createVerticalStrut(2))
  p.add(scroller1)
  p.add(Box.createVerticalStrut(2))
  p.add(check)
  p.add(Box.createVerticalStrut(5))
  p.add(JLabel("BoundedRangeModel+textField.ArrowButtonlessScrollBarUI"))
  p.add(textField2)
  p.add(Box.createVerticalStrut(2))
  p.add(scroller2)
  p.add(Box.createVerticalStrut(5))

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(makeBox(listOf(caretButton, offsetButton)), BorderLayout.SOUTH)
    it.border = BorderFactory.createEmptyBorder(20, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeBox(list: List<JButton>) = Box.createHorizontalBox().also {
  it.add(Box.createHorizontalGlue())
  list.forEach { button ->
    it.add(button)
    it.add(Box.createHorizontalStrut(5))
  }
}

private class EmptyThumbHandler(
  private val textField: JTextField,
  private val scroller: JScrollBar
) : ComponentAdapter(), DocumentListener {
  private val emptyThumbModel = DefaultBoundedRangeModel(0, 1, 0, 1)
  private fun changeThumbModel() {
    EventQueue.invokeLater {
      val m = textField.horizontalVisibility
      val iv = m.maximum - m.minimum - m.extent - 1 // -1: bug?
      if (iv <= 0) {
        scroller.model = emptyThumbModel
      } else {
        scroller.model = textField.horizontalVisibility
      }
    }
  }

  override fun componentResized(e: ComponentEvent) {
    changeThumbModel()
  }

  override fun insertUpdate(e: DocumentEvent) {
    changeThumbModel()
  }

  override fun removeUpdate(e: DocumentEvent) {
    changeThumbModel()
  }

  override fun changedUpdate(e: DocumentEvent) {
    changeThumbModel()
  }
}

private class ZeroSizeButton : JButton() {
  override fun getPreferredSize() = Dimension()
}

private class ArrowButtonlessScrollBarUI : BasicScrollBarUI() {
  override fun createDecreaseButton(orientation: Int) = ZeroSizeButton()

  override fun createIncreaseButton(orientation: Int) = ZeroSizeButton()

  override fun paintTrack(g: Graphics, c: JComponent, r: Rectangle) {
    // val g2 = g.create() as? Graphics2D ?: return
    // g2.setPaint(Color(100, 100, 100, 100))
    // g2.fillRect(r.x, r.y, r.width - 1, r.height - 1)
    // g2.dispose()
  }

  override fun paintThumb(g: Graphics, c: JComponent, r: Rectangle) {
    val sb = c as? JScrollBar
    if (sb?.isEnabled != true) {
      return
    }
    val m = sb.model
    val iv = m.maximum - m.minimum - m.extent - 1 // -1: bug?
    if (iv > 0) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      val color = when {
        isDragging -> DRAGGING_COLOR
        isThumbRollover -> ROLLOVER_COLOR
        else -> DEFAULT_COLOR
      }
      g2.paint = color
      g2.fillRect(r.x, r.y, r.width - 1, r.height - 1)
      g2.dispose()
    }
  }

  companion object {
    private val DEFAULT_COLOR = Color(220, 100, 100, 100)
    private val DRAGGING_COLOR = Color(200, 100, 100, 100)
    private val ROLLOVER_COLOR = Color(255, 120, 100, 100)
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
