package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.beans.PropertyChangeListener
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val leftPane = JSplitPane(JSplitPane.VERTICAL_SPLIT)
  leftPane.topComponent = JScrollPane(JTextArea("1111111"))
  leftPane.bottomComponent = JScrollPane(JTextArea("2222"))
  leftPane.isContinuousLayout = true
  leftPane.resizeWeight = .5

  val rightPane = JSplitPane(JSplitPane.VERTICAL_SPLIT)
  rightPane.topComponent = JScrollPane(JTree())
  rightPane.bottomComponent = JScrollPane(JTree())
  rightPane.isContinuousLayout = true
  rightPane.resizeWeight = .5

  val centerPane = JSplitPane() // JSplitPane.HORIZONTAL_SPLIT
  centerPane.leftComponent = leftPane
  centerPane.rightComponent = rightPane
  centerPane.isContinuousLayout = true
  centerPane.resizeWeight = .5

  val pcl = PropertyChangeListener { e ->
    if (JSplitPane.DIVIDER_LOCATION_PROPERTY == e.propertyName) {
      val source = e.source as? JSplitPane
      val target = if (source == leftPane) rightPane else leftPane
      val location = e.newValue
      if (location is Int && location != target.dividerLocation) {
        target.dividerLocation = location
      }
    }
  }
  leftPane.addPropertyChangeListener(pcl)
  rightPane.addPropertyChangeListener(pcl)

  val check = JCheckBox("setContinuousLayout", true)
  check.addActionListener { e ->
    val flag = (e.source as? JCheckBox)?.isSelected == true
    leftPane.isContinuousLayout = flag
    rightPane.isContinuousLayout = flag
    centerPane.isContinuousLayout = flag
  }

  return JPanel(BorderLayout()).also {
    it.add(check, BorderLayout.NORTH)
    it.add(centerPane)
    it.preferredSize = Dimension(320, 240)
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
