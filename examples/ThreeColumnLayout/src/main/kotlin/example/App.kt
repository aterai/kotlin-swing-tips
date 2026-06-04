package example

import java.awt.*
import javax.swing.*

fun createUI(): Component {
  val tabbedPane = JTabbedPane()
  tabbedPane.addTab("SpringLayout", createCmp1())
  tabbedPane.addTab("Custom BorderLayout", createCmp2())
  return JPanel(BorderLayout()).also {
    it.add(tabbedPane)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createCmp1(): Component {
  val leftList = createList(createListModel())
  val lsp = JScrollPane(leftList)
  lsp.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER

  val rightList = createList(DefaultListModel<String>())
  val rsp = JScrollPane(rightList)
  rsp.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER

  val button1 = createButton(">")
  button1.addActionListener {
    move(leftList, rightList)
  }

  val button2 = createButton("<")
  button2.addActionListener {
    move(rightList, leftList)
  }

  val box = createCenterBox(button1, button2)
  val layout = SpringLayout()
  val panel = JPanel(layout)
  panel.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

  val centerConstraints = layout.getConstraints(box)
  centerConstraints.width = Spring.constant(box.preferredSize.width)
  val leftConstraints = layout.getConstraints(lsp)
  val rightConstraints = layout.getConstraints(rsp)
  val width = Spring.max(leftConstraints.width, rightConstraints.width)
  leftConstraints.width = width
  rightConstraints.width = width
  panel.add(lsp, leftConstraints)
  panel.add(box, centerConstraints)
  panel.add(rsp, rightConstraints)

  val height = layout.getConstraint(SpringLayout.HEIGHT, panel)
  leftConstraints.height = height
  rightConstraints.height = height
  centerConstraints.height = height
  centerConstraints.setConstraint(
    SpringLayout.WEST,
    leftConstraints.getConstraint(SpringLayout.EAST),
  )
  rightConstraints.setConstraint(
    SpringLayout.WEST,
    centerConstraints.getConstraint(SpringLayout.EAST),
  )
  layout.putConstraint(SpringLayout.EAST, panel, 0, SpringLayout.EAST, rsp)

  return panel
}

private fun createCmp2(): Component {
  val leftList = createList(createListModel())
  val lsp = JScrollPane(leftList)
  lsp.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
  val rightList = createList(DefaultListModel<String>())
  val rsp = JScrollPane(rightList)
  rsp.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER

  val button1 = createButton(">")
  button1.addActionListener {
    move(leftList, rightList)
  }

  val button2 = createButton("<")
  button2.addActionListener {
    move(rightList, leftList)
  }

  val box = createCenterBox(button1, button2)
  val layout = object : BorderLayout(0, 0) {
    override fun layoutContainer(target: Container) {
      synchronized(target.treeLock) {
        val r = SwingUtilities.calculateInnerArea(target as? JComponent, null)
        val hgp = getHgap()
        var wc = r.width
        var we = wc / 2
        var ww = wc - we

        getLayoutComponent(CENTER)?.also {
          val d = it.preferredSize
          wc -= d.width + hgp * 2
          we = wc / 2
          ww = wc - we
          it.setBounds(r.x + hgp + ww, r.y, wc, r.height)
        }

        getLayoutComponent(EAST)?.setBounds(r.x + r.width - we, r.y, we, r.height)

        getLayoutComponent(WEST)?.setBounds(r.x, r.y, ww, r.height)
      }
    }
  }
  val panel = JPanel(layout)
  panel.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  panel.add(lsp, BorderLayout.WEST)
  panel.add(box, BorderLayout.CENTER)
  panel.add(rsp, BorderLayout.EAST)
  return panel
}

private fun <E> move(
  from: JList<E>,
  to: JList<E>,
) {
  val sm = from.selectionModel
  val selectedIndices = from.selectedIndices
  val fromModel = from.model as? DefaultListModel<E>
  val toModel = to.model as? DefaultListModel<E>
  if (fromModel == null || toModel == null) {
    return
  }
  val unselectedValues = mutableListOf<E>()
  for (i in 0..<fromModel.size) {
    if (!sm.isSelectedIndex(i)) {
      unselectedValues.add(fromModel.getElementAt(i))
    }
  }
  if (selectedIndices.isNotEmpty()) {
    for (i in selectedIndices) {
      toModel.addElement(fromModel[i])
    }
    fromModel.clear()
    val model = DefaultListModel<E>()
    unselectedValues.forEach { model.addElement(it) }
    from.model = model
  }
}

private fun createListModel(): DefaultListModel<String> {
  val model = DefaultListModel<String>()
  model.addElement("l${"o".repeat(40)}ng")
  for (i in 0..<5000) {
    model.addElement(i.toString())
  }
  return model
}

private fun <E> createList(model: ListModel<E>): JList<E> {
  val list = JList(model)
  val popup = JPopupMenu()
  popup.add("reverse").addActionListener {
    val sm = list.selectionModel
    for (i in 0..<list.model.size) {
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

private fun createButton(title: String): JButton {
  val button = JButton(title)
  button.isFocusable = false
  button.border = BorderFactory.createEmptyBorder(2, 8, 2, 8)
  return button
}

private fun createCenterBox(vararg buttons: JButton): Component {
  val box = Box.createVerticalBox()
  box.border = BorderFactory.createEmptyBorder(0, 5, 0, 5)
  box.add(Box.createVerticalGlue())
  for (b in buttons) {
    box.add(b)
    box.add(Box.createVerticalStrut(20))
  }
  box.add(Box.createVerticalGlue())
  return box
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
