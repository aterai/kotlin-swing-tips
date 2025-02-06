package example

import java.awt.*
import javax.swing.*

private val box = Box.createVerticalBox()
private val glue = Box.createVerticalGlue()

fun makeUI(): Component {
  box.border = BorderFactory.createLineBorder(Color.RED, 10)

  val scroll = JScrollPane(box)
  scroll.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
  scroll.verticalScrollBar.unitIncrement = 25

  addComp(JLabel("11111111111111111"))
  addComp(makeButton())
  addComp(makeCheckBox())
  addComp(makeLabel())

  return JPanel(BorderLayout()).also {
    it.add(makeToolBar(), BorderLayout.NORTH)
    it.add(scroll)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun addComp(comp: JComponent) {
  comp.maximumSize = Dimension(Short.MAX_VALUE.toInt(), comp.preferredSize.height)
  box.remove(glue)
  box.add(Box.createVerticalStrut(5))
  box.add(comp)
  box.add(glue)
  box.revalidate()
  EventQueue.invokeLater { comp.scrollRectToVisible(comp.bounds) }
}

private fun makeToolBar() = JToolBar().also {
  val addLabel = JButton("add JLabel")
  addLabel.addActionListener { addComp(makeLabel()) }

  val addButton = JButton("add JButton")
  addButton.addActionListener { addComp(makeButton()) }

  val addCheckBox = JButton("add JCheckBox")
  addCheckBox.addActionListener { addComp(makeCheckBox()) }

  it.add(addLabel)
  it.addSeparator()
  it.add(addButton)
  it.addSeparator()
  it.add(addCheckBox)
}

fun makeLabel() = object : JLabel("Height: 50") {
  override fun getPreferredSize() = super.getPreferredSize().also {
    it.height = 50
  }
}.also {
  it.isOpaque = true
  it.background = Color.YELLOW.brighter()
}

fun makeButton() = JButton("Beep Test").also {
  it.addActionListener {
    Toolkit.getDefaultToolkit().beep()
  }
}

fun makeCheckBox() = JCheckBox("2222222222222222222", true)

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
