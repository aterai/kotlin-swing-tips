package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.border.TitledBorder

fun makeUI(): Component {
  val fileNameLabel = AlignedLabel("File Name:")
  val filesOfTypeLabel = AlignedLabel("Files of Type:")
  val hostLabel = AlignedLabel("Host:")
  val portLabel = AlignedLabel("Port:")
  val userLabel = AlignedLabel("User Name:")
  val passwordLabel = AlignedLabel("Password:")
  AlignedLabel.groupLabels(fileNameLabel, filesOfTypeLabel, hostLabel, portLabel, userLabel, passwordLabel)

  val innerBorder = BorderFactory.createEmptyBorder(5, 2, 5, 5)

  val box1 = Box.createVerticalBox()
  val border1 = BorderFactory.createTitledBorder("FileChooser")
  border1.titlePosition = TitledBorder.ABOVE_TOP
  box1.border = BorderFactory.createCompoundBorder(border1, innerBorder)
  box1.add(makeLabeledBox(fileNameLabel, JTextField()))
  box1.add(Box.createVerticalStrut(5))
  box1.add(makeLabeledBox(filesOfTypeLabel, JComboBox<String>()))

  val box2 = Box.createVerticalBox()
  val border2 = BorderFactory.createTitledBorder("HTTP Proxy")
  border2.titlePosition = TitledBorder.ABOVE_TOP
  box2.border = BorderFactory.createCompoundBorder(border2, innerBorder)
  box2.add(makeLabeledBox(hostLabel, JTextField()))
  box2.add(Box.createVerticalStrut(5))
  box2.add(makeLabeledBox(portLabel, JTextField()))
  box2.add(Box.createVerticalStrut(5))
  box2.add(makeLabeledBox(userLabel, JTextField()))
  box2.add(Box.createVerticalStrut(5))
  box2.add(makeLabeledBox(passwordLabel, JPasswordField()))

  val box = Box.createVerticalBox()
  box.add(box1)
  box.add(Box.createVerticalStrut(10))
  box.add(box2)

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeLabeledBox(label: Component, c: Component) = Box.createHorizontalBox().also {
  it.add(label)
  it.add(Box.createHorizontalStrut(5))
  it.add(c)
}

// @see javax/swing/plaf/metal/MetalFileChooserUI.java
private class AlignedLabel(text: String) : JLabel(text) {
  private var group = mutableListOf<AlignedLabel>()
  private var maxWidth = 0

  init {
    horizontalAlignment = SwingConstants.RIGHT
  }

  override fun getPreferredSize() = super.getPreferredSize()?.also {
    // Align the width with all other labels in group.
    it.width = getMaxWidth() + INDENT
  }

  private fun getSuperPreferredWidth() = super.getPreferredSize().width

  private fun getMaxWidth(): Int {
    if (maxWidth == 0) {
      // val max = group.stream().map { it.getSuperPreferredWidth() }.reduce(0, Integer::max)
      // val max = group.map { it.getSuperPreferredWidth() }.fold(0) { a, b -> maxOf(a, b) }
      // val max = group.map { it.getSuperPreferredWidth() }.fold(0, ::maxOf)
      val max = group.map { it.getSuperPreferredWidth() }.maxOrNull() ?: 0
      group.forEach { al -> al.maxWidth = max }
    }
    return maxWidth
  }

  companion object {
    private const val INDENT = 10

    fun groupLabels(vararg list: AlignedLabel) {
      val gp = list.toMutableList()
      gp.forEach { label -> label.group = gp }
    }
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
