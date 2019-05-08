package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.geom.Rectangle2D
import java.util.ArrayList
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  init {
    val tabbedPane = JTabbedPane().also {
      it.addTab("default remove", makeUI0())
      it.addTab("clear + addElement", makeUI1())
      it.addTab("addAll + remove", makeUI2())
    }
    add(tabbedPane)
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeUI0(): Component {
    val model = DefaultListModel<String>()
    for (i in 0..5000) {
      model.addElement(i.toString())
    }
    val leftList = makeList<String>(model)

    val rightList = makeList<String>(DefaultListModel<String>())

    val button1 = makeButton(">")
    button1.addActionListener { move0<String>(leftList, rightList) }

    val button2 = makeButton("<")
    button2.addActionListener { move0<String>(rightList, leftList) }

    return SpringLayoutUtil.makePanel(leftList, rightList, button1, button2)
  }

  private fun makeUI1(): Component {
    val model = DefaultListModel<String>()
    for (i in 10000..30000) {
      model.addElement(i.toString())
    }
    val leftList = makeList<String>(model)

    val rightList = makeList<String>(DefaultListModel<String>())

    val button1 = makeButton(">")
    button1.addActionListener { move1<String>(leftList, rightList) }

    val button2 = makeButton("<")
    button2.addActionListener { move1<String>(rightList, leftList) }

    return SpringLayoutUtil.makePanel(leftList, rightList, button1, button2)
  }

  private fun makeUI2(): Component {
    val model = ArrayListModel<String>()
    for (i in 30000..50000) {
      model.add(i.toString())
    }
    val leftList = makeList<String>(model)

    val rightList = makeList(ArrayListModel<String>())

    val button1 = makeButton(">")
    button1.addActionListener { move2<String>(leftList, rightList) }

    val button2 = makeButton("<")
    button2.addActionListener { move2<String>(rightList, leftList) }

    return SpringLayoutUtil.makePanel(leftList, rightList, button1, button2)
  }

  private fun <E> move0(from: JList<E>, to: JList<E>) {
    val selectedIndices = from.getSelectedIndices()
    if (selectedIndices.size > 0) {
      val fromModel = from.getModel() as DefaultListModel<E>
      val toModel = to.getModel() as DefaultListModel<E>
      for (i in selectedIndices) {
        toModel.addElement(fromModel.get(i))
      }
      for (i in selectedIndices.indices.reversed()) {
        fromModel.remove(selectedIndices[i])
      }
    }
  }

  private fun <E> move1(from: JList<E>, to: JList<E>) {
    val sm = from.getSelectionModel()
    val selectedIndices = from.getSelectedIndices()

    val fromModel = from.getModel() as DefaultListModel<E>
    val toModel = to.getModel() as DefaultListModel<E>
    val unselectedValues = ArrayList<E>()
    for (i in 0 until fromModel.getSize()) {
      if (!sm.isSelectedIndex(i)) {
        unselectedValues.add(fromModel.getElementAt(i))
      }
    }
    if (selectedIndices.size > 0) {
      for (i in selectedIndices) {
        toModel.addElement(fromModel.get(i))
      }
      val model = DefaultListModel<E>()
      unselectedValues.forEach { model.addElement(it) }
      from.setModel(model)
    }
  }

  private fun <E> move2(from: JList<E>, to: JList<E>) {
    val selectedIndices = from.getSelectedIndices()
    if (selectedIndices.size > 0) {
      (to.getModel() as ArrayListModel<E>).addAll(from.getSelectedValuesList())
      (from.getModel() as ArrayListModel<E>).remove(selectedIndices)
    }
  }

  private fun <E> makeList(model: ListModel<E>): JList<E> {
    val list = JList<E>(model)
    val popup = JPopupMenu()
    popup.add("reverse").addActionListener {
      val sm = list.getSelectionModel()
      for (i in 0 until list.getModel().getSize()) {
        if (sm.isSelectedIndex(i)) {
          sm.removeSelectionInterval(i, i)
        } else {
          sm.addSelectionInterval(i, i)
        }
      }
    }
    list.setComponentPopupMenu(popup)
    return list
  }

  private fun makeButton(title: String): JButton {
    val button = JButton(title)
    button.setFocusable(false)
    button.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8))
    return button
  }
}

internal class ArrayListModel<E> : AbstractListModel<E>() {
  private val delegate = ArrayList<E>()

  fun add(element: E) {
    val index = delegate.size
    delegate.add(element)
    fireIntervalAdded(this, index, index)
  }

  fun addAll(c: Collection<E>) {
    delegate.addAll(c)
    fireIntervalAdded(this, 0, delegate.size)
  }

  fun remove(index: Int): E {
    val rv = delegate.get(index)
    delegate.removeAt(index)
    fireIntervalRemoved(this, index, index)
    return rv
  }

  fun remove(selectedIndices: IntArray) {
    if (selectedIndices.size > 0) {
      val max = selectedIndices.size - 1
      for (i in max downTo 0) {
        delegate.removeAt(selectedIndices[i])
      }
      fireIntervalRemoved(this, selectedIndices[0], selectedIndices[max])
    }
  }

  override fun getElementAt(index: Int) = delegate.get(index)

  override fun getSize() = delegate.size
}

internal object SpringLayoutUtil {
  fun setScaleAndAdd(parent: Container, layout: SpringLayout, child: Component, r: Rectangle2D.Float) {
    val panelw = layout.getConstraint(SpringLayout.WIDTH, parent)
    val panelh = layout.getConstraint(SpringLayout.HEIGHT, parent)

    val c = layout.getConstraints(child)
    c.setX(Spring.scale(panelw, r.x))
    c.setY(Spring.scale(panelh, r.y))
    c.setWidth(Spring.scale(panelw, r.width))
    c.setHeight(Spring.scale(panelh, r.height))

    parent.add(child)
  }

  fun makePanel(leftList: JList<*>, rightList: JList<*>, l2rButton: JButton, r2lButton: JButton): Component {
    val box = Box.createVerticalBox()
    box.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2))
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
} /* Singleton */

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    }.onFailure {
      it.printStackTrace()
      Toolkit.getDefaultToolkit().beep()
    }
    JFrame().apply {
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
