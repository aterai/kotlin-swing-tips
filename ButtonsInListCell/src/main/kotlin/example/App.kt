package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.MouseInputAdapter
import javax.swing.event.MouseInputListener

fun makeUI() = JPanel(BorderLayout()).also {
  val model = makeModel()
  val list = object : JList<String>(model) {
    private var handler: MouseInputListener? = null

    override fun updateUI() {
      removeMouseListener(handler)
      removeMouseMotionListener(handler)
      super.updateUI()
      fixedCellHeight = -1
      handler = CellButtonsMouseListener(this)
      addMouseListener(handler)
      addMouseMotionListener(handler)
      cellRenderer = ButtonsRenderer(model)
    }
  }
  it.add(JScrollPane(list))
  it.preferredSize = Dimension(320, 240)
}

fun makeModel() = DefaultListModel<String>().also {
  it.addElement("11\n1")
  it.addElement("222222222222222\n222222222222222")
  it.addElement("3333333333333333333\n33333333333333333333\n33333333333333333")
  it.addElement("444")
}

private class CellButtonsMouseListener<E>(private val list: JList<E>) : MouseInputAdapter() {
  private var prevIndex = -1
  private var prevButton: JButton? = null
  override fun mouseMoved(e: MouseEvent) {
    val pt = e.point
    val index = list.locationToIndex(pt)
    if (!list.getCellBounds(index, index).contains(pt)) {
      if (prevIndex >= 0) {
        rectRepaint(list, list.getCellBounds(prevIndex, prevIndex))
      }
      prevButton = null
      return
    }
    if (index >= 0) {
      val renderer = list.cellRenderer as? ButtonsRenderer<*> ?: return
      val button = getButton(list, pt, index)
      renderer.button = button
      button?.also {
        repaintCell(renderer, button, index)
      } ?: repaintPrevButton(renderer, index)
      prevButton = button
    }
    prevIndex = index
  }

  private fun repaintCell(renderer: ButtonsRenderer<*>, button: JButton, index: Int) {
    button.model.isRollover = true
    renderer.rolloverIndex = index
    if (button != prevButton) {
      rectRepaint(list, list.getCellBounds(prevIndex, index))
    }
  }

  private fun repaintPrevButton(renderer: ButtonsRenderer<*>, index: Int) {
    renderer.rolloverIndex = -1
    val r = if (prevIndex == index) {
      prevButton?.let { list.getCellBounds(prevIndex, prevIndex) }
    } else {
      list.getCellBounds(index, index)
    }
    rectRepaint(list, r)
    prevIndex = -1
  }

  override fun mousePressed(e: MouseEvent) {
    val pt = e.point
    val index = list.locationToIndex(pt)
    if (index >= 0) {
      val button = getButton(list, pt, index)
      val renderer = list.cellRenderer
      if (button != null && renderer is ButtonsRenderer<*>) {
        renderer.pressedIndex = index
        renderer.button = button
        rectRepaint(list, list.getCellBounds(index, index))
      }
    }
  }

  override fun mouseReleased(e: MouseEvent) {
    val pt = e.point
    val index = list.locationToIndex(pt)
    if (index >= 0) {
      val button = getButton(list, pt, index)
      val renderer = list.cellRenderer
      if (button != null && renderer is ButtonsRenderer<*>) {
        renderer.pressedIndex = -1
        renderer.button = null
        button.doClick()
        rectRepaint(list, list.getCellBounds(index, index))
      }
    }
  }

  private fun rectRepaint(c: JComponent, rect: Rectangle?) {
    rect?.also { c.repaint(it) }
  }

  private fun <E> getButton(list: JList<E>, pt: Point, index: Int): JButton? {
    val prototype = list.prototypeCellValue
    val c = list.cellRenderer.getListCellRendererComponent(list, prototype, index, false, false)
    val r = list.getCellBounds(index, index)
    c.bounds = r
    pt.translate(-r.x, -r.y)
    return SwingUtilities.getDeepestComponentAt(c, pt.x, pt.y) as? JButton
  }
}

private class ButtonsRenderer<E>(model: DefaultListModel<E>) : ListCellRenderer<E> {
  private val textArea = JTextArea()
  private val deleteButton = JButton("delete")
  private val copyButton = JButton("copy")
  private val buttons = listOf(deleteButton, copyButton)
  private val renderer = object : JPanel(BorderLayout()) {
    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      d.width = 0 // VerticalScrollBar as needed
      return d
    }
  }
  private var targetIndex = 0
  var pressedIndex = -1
  var rolloverIndex = -1
  var button: JButton? = null

  init {
    renderer.border = BorderFactory.createEmptyBorder(5, 5, 5, 0)
    renderer.isOpaque = true
    textArea.lineWrap = true
    textArea.isOpaque = false
    renderer.add(textArea)
    deleteButton.addActionListener {
      // val isMoreThanOneItem = model.size > 1
      model.takeIf { it.size > 1 }?.remove(targetIndex)
    }
    copyButton.addActionListener {
      model.add(targetIndex, model[targetIndex])
    }
    val box = Box.createHorizontalBox()
    buttons.forEach {
      it.isFocusable = false
      it.isRolloverEnabled = false
      box.add(it)
      box.add(Box.createHorizontalStrut(5))
    }
    renderer.add(box, BorderLayout.EAST)
  }

  override fun getListCellRendererComponent(
    list: JList<out E>,
    value: E?,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean
  ): Component {
    textArea.text = value?.toString() ?: ""
    targetIndex = index
    if (isSelected) {
      renderer.background = list.selectionBackground
      textArea.foreground = list.selectionForeground
    } else {
      renderer.background = if (index % 2 == 0) EVEN_COLOR else list.background
      textArea.foreground = list.foreground
    }
    buttons.forEach { resetButtonStatus(it) }
    button?.also {
      if (index == pressedIndex) {
        it.model.isSelected = true
        it.model.isArmed = true
        it.model.isPressed = true
      } else if (index == rolloverIndex) {
        it.model.isRollover = true
      }
    }
    return renderer
  }

  private fun resetButtonStatus(button: AbstractButton) {
    val model = button.model
    model.isRollover = false
    model.isArmed = false
    model.isPressed = false
    model.isSelected = false
  }

  companion object {
    private val EVEN_COLOR = Color(0xE6_FF_E6)
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
