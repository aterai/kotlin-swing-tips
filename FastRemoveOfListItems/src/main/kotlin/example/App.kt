package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.geom.Rectangle2D
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI() = JTabbedPane().also {
  it.addTab("default remove", makeCmp0())
  it.addTab("clear + addElement", makeCmp1())
  it.addTab("addAll + remove", makeCmp2())
  it.preferredSize = Dimension(320, 240)
}

private fun makeCmp0(): Component {
  val model = DefaultListModel<String>()
  for (i in 0..5000) {
    model.addElement(i.toString())
  }
  val leftList = makeList(model)

  val rightList = makeList(DefaultListModel<String>())

  val button1 = makeButton(">")
  button1.addActionListener { move0(leftList, rightList) }

  val button2 = makeButton("<")
  button2.addActionListener { move0(rightList, leftList) }

  return SpringLayoutUtil.makePanel(leftList, rightList, button1, button2)
}

private fun makeCmp1(): Component {
  val model = DefaultListModel<String>()
  for (i in 10_000..30_000) {
    model.addElement(i.toString())
  }
  val leftList = makeList(model)

  val rightList = makeList(DefaultListModel<String>())

  val button1 = makeButton(">")
  button1.addActionListener { move1(leftList, rightList) }

  val button2 = makeButton("<")
  button2.addActionListener { move1(rightList, leftList) }

  return SpringLayoutUtil.makePanel(leftList, rightList, button1, button2)
}

private fun makeCmp2(): Component {
  val model = ArrayListModel<String>()
  for (i in 30_000..50_000) {
    model.add(i.toString())
  }
  val leftList = makeList(model)

  val rightList = makeList(ArrayListModel<String>())

  val button1 = makeButton(">")
  button1.addActionListener { move2(leftList, rightList) }

  val button2 = makeButton("<")
  button2.addActionListener { move2(rightList, leftList) }

  return SpringLayoutUtil.makePanel(leftList, rightList, button1, button2)
}

private fun <E> move0(from: JList<E>, to: JList<E>) {
  val selectedIndices = from.selectedIndices
  val fromModel = from.model as? DefaultListModel<E>
  val toModel = to.model as? DefaultListModel<E>
  if (selectedIndices.isNotEmpty() && fromModel != null && toModel != null) {
    for (i in selectedIndices) {
      toModel.addElement(fromModel[i])
    }
    for (i in selectedIndices.indices.reversed()) {
      fromModel.remove(selectedIndices[i])
    }
  }
}

private fun <E> move1(from: JList<E>, to: JList<E>) {
  val sm = from.selectionModel
  val selectedIndices = from.selectedIndices

  val fromModel = from.model as? DefaultListModel<E> ?: return
  val toModel = to.model as? DefaultListModel<E> ?: return
  val unselectedValues = mutableListOf<E>()
  for (i in 0 until fromModel.size) {
    if (!sm.isSelectedIndex(i)) {
      unselectedValues.add(fromModel.getElementAt(i))
    }
  }
  if (selectedIndices.isNotEmpty()) {
    for (i in selectedIndices) {
      toModel.addElement(fromModel.get(i))
    }
    val model = DefaultListModel<E>()
    unselectedValues.forEach { model.addElement(it) }
    from.model = model
  }
}

private fun <E> move2(from: JList<E>, to: JList<E>) {
  val selectedIndices = from.selectedIndices
  if (selectedIndices.isNotEmpty()) {
    (to.model as? ArrayListModel<E>)?.addAll(from.selectedValuesList)
    (from.model as? ArrayListModel<E>)?.remove(selectedIndices)
  }
}

private fun <E> makeList(model: ListModel<E>): JList<E> {
  val list = JList(model)
  val popup = JPopupMenu()
  popup.add("reverse").addActionListener {
    val sm = list.selectionModel
    for (i in 0 until list.model.size) {
      if (sm.isSelectedIndex(i)) {
        sm.removeSelectionInterval(i, i)
      } else {
        sm.addSelectionInterval(i, i)
      }
    }
  }
  list.componentPopupMenu = popup
  return list
}

private fun makeButton(title: String) = JButton(title).also {
  it.isFocusable = false
  it.border = BorderFactory.createEmptyBorder(2, 8, 2, 8)
}

private class ArrayListModel<E> : AbstractListModel<E>() {
  private val delegate = mutableListOf<E>()

  fun add(element: E) {
    val index = delegate.size
    delegate.add(element)
    fireIntervalAdded(this, index, index)
  }

  fun addAll(c: Collection<E>) {
    delegate.addAll(c)
    fireIntervalAdded(this, 0, delegate.size)
  }

  // fun remove(index: Int): E {
  //   val rv = delegate[index]
  //   delegate.removeAt(index)
  //   fireIntervalRemoved(this, index, index)
  //   return rv
  // }

  fun remove(selectedIndices: IntArray) {
    if (selectedIndices.isNotEmpty()) {
      val max = selectedIndices.size - 1
      for (i in max downTo 0) {
        delegate.removeAt(selectedIndices[i])
      }
      fireIntervalRemoved(this, selectedIndices[0], selectedIndices[max])
    }
  }

  override fun getElementAt(index: Int) = delegate[index]

  override fun getSize() = delegate.size
}

private object SpringLayoutUtil {
  private fun setScaleAndAdd(
    parent: Container,
    layout: SpringLayout,
    child: Component,
    r: Rectangle2D.Float
  ) {
    val pnlWidth = layout.getConstraint(SpringLayout.WIDTH, parent)
    val pnlHeight = layout.getConstraint(SpringLayout.HEIGHT, parent)

    val c = layout.getConstraints(child)
    c.x = Spring.scale(pnlWidth, r.x)
    c.y = Spring.scale(pnlHeight, r.y)
    c.width = Spring.scale(pnlWidth, r.width)
    c.height = Spring.scale(pnlHeight, r.height)

    parent.add(child)
  }

  fun makePanel(
    leftList: JList<*>,
    rightList: JList<*>,
    l2rButton: JButton,
    r2lButton: JButton
  ): Component {
    val box = Box.createVerticalBox()
    box.border = BorderFactory.createEmptyBorder(0, 2, 0, 2)
    box.add(Box.createVerticalGlue())
    box.add(l2rButton)
    box.add(Box.createVerticalStrut(20))
    box.add(r2lButton)
    box.add(Box.createVerticalGlue())

    val cpn = JPanel(GridBagLayout())
    cpn.add(box)

    val spl = JScrollPane(leftList)
    val spr = JScrollPane(rightList)

    val layout = SpringLayout()
    val p = JPanel(layout)
    setScaleAndAdd(p, layout, spl, Rectangle2D.Float(.05f, .05f, .40f, .90f))
    setScaleAndAdd(p, layout, cpn, Rectangle2D.Float(.45f, .05f, .10f, .90f))
    setScaleAndAdd(p, layout, spr, Rectangle2D.Float(.55f, .05f, .40f, .90f))
    return p
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
