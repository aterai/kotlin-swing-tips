package example

import java.awt.*
import java.awt.geom.Rectangle2D
import javax.swing.*

fun createUI() = JTabbedPane().also {
  // Tab1: Removes selected items one at a time via DefaultListModel#remove(int).
  it.addTab("default remove", createIndividualRemovalTab())
  // Tab2: Collects the unselected elements and swaps in a brand-new model.
  it.addTab("clear + addElement", createModelRebuildTab())
  // Tab3: Uses a custom ListModel that batches removal into fewer events.
  it.addTab("addAll + remove", createBatchArrayModelTab())
  it.preferredSize = Dimension(320, 240)
}

private fun createIndividualRemovalTab(): Component {
  val model = DefaultListModel<String>()
  for (i in 0..5000) {
    model.addElement(i.toString())
  }
  val leftList = createSelectableList(model)
  val rightList = createSelectableList<String>(DefaultListModel())

  val moveRightButton = createTransferButton(">")
  moveRightButton.addActionListener {
    transferSelectedByIndividualRemoval<String>(leftList, rightList)
  }

  val moveLeftButton = createTransferButton("<")
  moveLeftButton.addActionListener {
    transferSelectedByIndividualRemoval(rightList, leftList)
  }

  return SpringLayoutUtils.createDualListPanel(
    leftList,
    rightList,
    moveRightButton,
    moveLeftButton,
  )
}

private fun createModelRebuildTab(): Component {
  val model = DefaultListModel<String>()
  for (i in 10_000..30_000) {
    model.addElement(i.toString())
  }
  val leftList = createSelectableList<String>(model)
  val rightList = createSelectableList<String>(DefaultListModel())

  val moveRightButton = createTransferButton(">")
  moveRightButton.addActionListener {
    transferSelectedByModelRebuild(leftList, rightList)
  }

  val moveLeftButton = createTransferButton("<")
  moveLeftButton.addActionListener {
    transferSelectedByModelRebuild(rightList, leftList)
  }

  return SpringLayoutUtils.createDualListPanel(
    leftList,
    rightList,
    moveRightButton,
    moveLeftButton,
  )
}

private fun createBatchArrayModelTab(): Component {
  val model = ArrayListModel<String>()
  for (i in 30_000..50_000) {
    model.add(i.toString())
  }
  val leftList = createSelectableList(model)
  val rightList = createSelectableList<String>(ArrayListModel())

  val moveRightButton = createTransferButton(">")
  moveRightButton.addActionListener {
    transferSelectedByBatchArrayModel<String>(leftList, rightList)
  }

  val moveLeftButton = createTransferButton("<")
  moveLeftButton.addActionListener {
    transferSelectedByBatchArrayModel(rightList, leftList)
  }

  return SpringLayoutUtils.createDualListPanel(
    leftList,
    rightList,
    moveRightButton,
    moveLeftButton,
  )
}

private fun <E> transferSelectedByIndividualRemoval(src: JList<E>, dst: JList<E>) {
  val selectedIndices = src.selectedIndices
  if (selectedIndices.size > 0) {
    val sourceModel = src.getModel() as DefaultListModel<E>
    val destinationModel = dst.getModel() as DefaultListModel<E>
    for (index in selectedIndices) {
      destinationModel.addElement(sourceModel.get(index))
    }
    for (i in selectedIndices.indices.reversed()) {
      sourceModel.remove(selectedIndices[i])
    }
  }
}

private fun <E> transferSelectedByModelRebuild(src: JList<E>, dst: JList<E>) {
  val selectedIndices = src.selectedIndices
  if (selectedIndices.size > 0) {
    val sourceModel = src.getModel() as? DefaultListModel<E> ?: return
    val destinationModel = dst.getModel() as? DefaultListModel<E> ?: return
    val selectionModel = src.selectionModel
    val unselectedValues = mutableListOf<E>()
    for (i in 0..<sourceModel.size) {
      if (!selectionModel.isSelectedIndex(i)) {
        unselectedValues.add(sourceModel.getElementAt(i))
      }
    }
    for (index in selectedIndices) {
      destinationModel.addElement(sourceModel.get(index))
    }
    val rebuiltModel = DefaultListModel<E>()
    unselectedValues.forEach { rebuiltModel.addElement(it) }

    // // Java 11:
    // // https://bugs.openjdk.org/browse/JDK-8201289
    // // Destination is a live model: batch the insert so it fires O(1)
    // // events instead of one per selected element.
    // destinationModel.addAll(src.getSelectedValuesList());
    // DefaultListModel<E> rebuiltModel = new DefaultListModel<>();
    // rebuiltModel.addAll(unselectedValues);
    src.setModel(rebuiltModel)
  }
}

private fun <E> transferSelectedByBatchArrayModel(src: JList<E>, dst: JList<E>) {
  val selectedIndices = src.selectedIndices
  if (selectedIndices.size > 0) {
    (dst.getModel() as? ArrayListModel<E>)?.addAll(src.getSelectedValuesList())
    (src.getModel() as? ArrayListModel<E>)?.remove(*selectedIndices)
  }
}

private fun <E> createSelectableList(model: ListModel<E>): JList<E> {
  val list = JList(model)
  val popup = JPopupMenu()
  popup.add("reverse").addActionListener {
    val selectionModel = list.selectionModel
    for (i in 0..<list.getModel().size) {
      if (selectionModel.isSelectedIndex(i)) {
        selectionModel.removeSelectionInterval(i, i)
      } else {
        selectionModel.addSelectionInterval(i, i)
      }
    }
  }
  list.setComponentPopupMenu(popup)
  return list
}

private fun createTransferButton(title: String): JButton {
  val button = JButton(title)
  button.setFocusable(false)
  button.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8))
  return button
}

private class ArrayListModel<E> : AbstractListModel<E>() {
  private val items = mutableListOf<E>()

  fun add(element: E) {
    val index = items.size
    items.add(element)
    fireIntervalAdded(this, index, index)
  }

  fun addAll(elements: MutableCollection<out E>) {
    val firstAddedIndex = items.size
    items.addAll(elements)
    val lastAddedIndex = items.size - 1
    if (lastAddedIndex >= firstAddedIndex) {
      fireIntervalAdded(this, firstAddedIndex, lastAddedIndex)
    }
  }

  fun remove(index: Int): E {
    val removedElement = items[index]
    items.removeAt(index)
    fireIntervalRemoved(this, index, index)
    return removedElement
  }

  fun remove(vararg indices: Int) {
    if (indices.isEmpty()) {
      return
    }
    var runEnd = indices.size - 1
    while (runEnd >= 0) {
      var runStart = runEnd
      while (runStart > 0 && indices[runStart - 1] == indices[runStart] - 1) {
        runStart--
      }
      val fromIndex = indices[runStart]
      val toIndex = indices[runEnd]
      items.subList(fromIndex, toIndex + 1).clear()
      fireIntervalRemoved(this, fromIndex, toIndex)
      runEnd = runStart - 1
    }
  }

  override fun getElementAt(index: Int) = items[index]

  override fun getSize() = items.size
}

private object SpringLayoutUtils {
  private fun setScaleAndAdd(
    parent: Container,
    child: Component,
    bounds: Rectangle2D,
  ) {
    val layoutManager = parent.layout
    if (layoutManager is SpringLayout) {
      val parentWidth = layoutManager.getConstraint(SpringLayout.WIDTH, parent)
      val parentHeight = layoutManager.getConstraint(SpringLayout.HEIGHT, parent)
      val childConstraints = layoutManager.getConstraints(child)
      childConstraints.setX(Spring.scale(parentWidth, bounds.x.toFloat()))
      childConstraints.setY(Spring.scale(parentHeight, bounds.y.toFloat()))
      childConstraints.setWidth(Spring.scale(parentWidth, bounds.width.toFloat()))
      childConstraints.setHeight(Spring.scale(parentHeight, bounds.height.toFloat()))
      parent.add(child)
    }
  }

  fun createDualListPanel(
    leftList: JList<*>,
    rightList: JList<*>,
    moveRightBtn: JButton,
    moveLeftBtn: JButton,
  ): Component {
    val buttonBox = Box.createVerticalBox()
    buttonBox.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2))
    buttonBox.add(Box.createVerticalGlue())
    buttonBox.add(moveRightBtn)
    buttonBox.add(Box.createVerticalStrut(20))
    buttonBox.add(moveLeftBtn)
    buttonBox.add(Box.createVerticalGlue())

    val buttonPanel = JPanel(GridBagLayout())
    buttonPanel.add(buttonBox)

    val leftScroll = JScrollPane(leftList)
    val rightScroll = JScrollPane(rightList)

    val p = JPanel(SpringLayout())
    setScaleAndAdd(p, leftScroll, Rectangle2D.Float(.05f, .05f, .40f, .90f))
    setScaleAndAdd(p, buttonPanel, Rectangle2D.Float(.45f, .05f, .10f, .90f))
    setScaleAndAdd(p, rightScroll, Rectangle2D.Float(.55f, .05f, .40f, .90f))
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
