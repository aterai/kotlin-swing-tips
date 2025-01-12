package example

import java.awt.*
import java.awt.event.ActionEvent
import javax.swing.*

fun makeUI(): Component {
  val model1 = SpinnerNumberModel(100, 10, 300, 10)
  val scroll1 = makeScrollPane(JTree(), model1)
  val model2 = SpinnerNumberModel(150, 10, 300, 10)
  val textArea = JTextArea()
  val scroll2 = makeScrollPane(textArea, model2)
  val split = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scroll1, scroll2)
  split.isOneTouchExpandable = true
  split.isContinuousLayout = true
  val menu1 = JMenu("JSplitPane")
  menu1.add(makeSpinner("L: ", model1))
  menu1.add(makeSpinner("R: ", model2))
  menu1.addSeparator()
  menu1.add("resetToPreferredSizes").addActionListener {
    split.resetToPreferredSizes()
    info(split, textArea)
  }
  menu1.addSeparator()
  menu1.add("setDividerLocation(.5)").addActionListener { split.setDividerLocation(.5) }
  menu1.add("selectMin").addActionListener { selectMinMax(split, "selectMin") }
  menu1.add("selectMax").addActionListener { selectMinMax(split, "selectMax") }
  val menu2 = JMenu("ResizeWeight")
  val r0 = JRadioButtonMenuItem("0.0", true)
  menu2.add(r0).addActionListener { split.resizeWeight = 0.0 }
  val r1 = JRadioButtonMenuItem("0.5")
  menu2.add(r1).addActionListener { split.resizeWeight = .5 }
  val r2 = JRadioButtonMenuItem("1.0")
  menu2.add(r2).addActionListener { split.resizeWeight = 1.0 }
  val group = ButtonGroup()
  for (r in listOf(r0, r1, r2)) {
    group.add(r)
  }
  val menuBar = JMenuBar()
  menuBar.add(menu1)
  menuBar.add(menu2)
  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = menuBar }
    it.add(split)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun info(split: JSplitPane, textArea: JTextArea) {
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
  splitPane.requestFocusInWindow()
  object : SwingWorker<Unit?, Unit?>() {
    override fun doInBackground() = null

    override fun done() {
      super.done()
      val a = splitPane.actionMap[cmd]
      a.actionPerformed(ActionEvent(splitPane, ActionEvent.ACTION_PERFORMED, cmd))
      KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner()
    }
  }.execute()
}

private fun makeScrollPane(c: Component, m: SpinnerNumberModel): JScrollPane {
  return object : JScrollPane(c) {
    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      d.width = m.number.toInt()
      return d
    }
  }
}

private fun makeSpinner(title: String, model: SpinnerModel): Component {
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
