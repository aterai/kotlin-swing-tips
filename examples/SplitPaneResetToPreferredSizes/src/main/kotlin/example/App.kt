package example

import java.awt.*
import java.awt.event.ActionEvent
import javax.swing.*

fun createUI(): Component {
  val model1 = SpinnerNumberModel(100, 10, 300, 10)
  val scroll1 = createScrollPane(JTree(), model1)
  val model2 = SpinnerNumberModel(150, 10, 300, 10)
  val textArea = JTextArea()
  val scroll2 = createScrollPane(textArea, model2)
  val split = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scroll1, scroll2)
  split.isOneTouchExpandable = true
  split.isContinuousLayout = true
  val menu1 = JMenu("JSplitPane")
  menu1.add(createSpinner("L: ", model1))
  menu1.add(createSpinner("R: ", model2))
  menu1.addSeparator()
  menu1.add("resetToPreferredSizes").addActionListener {
    split.resetToPreferredSizes()
    appendDividerInfo(split, textArea)
  }
  menu1.addSeparator()
  menu1.add("setDividerLocation(.5)").addActionListener {
    split.setDividerLocation(.5)
  }
  menu1.add("selectMin").addActionListener {
    selectMinMax(split, "selectMin")
  }
  menu1.add("selectMax").addActionListener {
    selectMinMax(split, "selectMax")
  }
  val menuBar = JMenuBar()
  menuBar.add(menu1)
  menuBar.add(createResizeWeightMenu(split))
  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = menuBar }
    it.add(split)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createResizeWeightMenu(split: JSplitPane): JMenu {
  val menu = JMenu("ResizeWeight")
  val group = ButtonGroup()
  for (weight in doubleArrayOf(0.0, .5, 1.0)) {
    // Select the item that matches the current resize weight (default: 0.0)
    val selected = weight.compareTo(split.resizeWeight) == 0
    val item = JRadioButtonMenuItem(weight.toString(), selected)
    item.addActionListener { split.resizeWeight = weight }
    group.add(item)
    menu.add(item)
  }
  return menu
}

private fun appendDividerInfo(
  split: JSplitPane,
  textArea: JTextArea,
) {
  // resetToPreferredSizes() only revalidates the JSplitPane, so wait for the layout
  EventQueue.invokeLater {
    val w1 = split.leftComponent.preferredSize.width
    val w2 = split.rightComponent.preferredSize.width
    val rw = split.resizeWeight
    val loc = split.dividerLocation
    textArea.append("%d:%d,loc:%d,weight:%.1f%n".format(w1, w2, loc, rw))
  }
}

private fun selectMinMax(
  splitPane: JSplitPane,
  cmd: String,
) {
  // The "selectMin"/"selectMax" actions of BasicSplitPaneUI assume that
  // the JSplitPane has the focus, so request it first and invoke the action
  // after the focus change has been processed
  splitPane.requestFocusInWindow()
  object : SwingWorker<Unit?, Unit?>() {
    override fun doInBackground() = null

    override fun done() {
      splitPane.actionMap[cmd]?.actionPerformed(
        ActionEvent(splitPane, ActionEvent.ACTION_PERFORMED, cmd),
      )
      KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner()
    }
  }.execute()
}

private fun createScrollPane(c: Component, m: SpinnerNumberModel): JScrollPane {
  return object : JScrollPane(c) {
    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      d.width = m.number.toInt()
      return d
    }
  }
}

private fun createSpinner(title: String, model: SpinnerModel): Component {
  val box = Box.createHorizontalBox()
  box.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
  box.add(JLabel(title))
  box.add(JSpinner(model))
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
