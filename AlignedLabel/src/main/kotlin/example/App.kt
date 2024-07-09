package example

import java.awt.*
import javax.swing.*
import javax.swing.border.TitledBorder

fun makeUI(): Component {
  val fileName = AlignedLabel("File Name:")
  val filesOfType = AlignedLabel("Files of Type:")
  val host = AlignedLabel("Host:")
  val port = AlignedLabel("Port:")
  val user = AlignedLabel("User Name:")
  val password = AlignedLabel("Password:")
  AlignedLabel.groupLabels(fileName, filesOfType, host, port, user, password)

  val innerBorder = BorderFactory.createEmptyBorder(5, 2, 5, 5)

  val box1 = Box.createVerticalBox()
  val border1 = BorderFactory.createTitledBorder("FileChooser")
  border1.titlePosition = TitledBorder.ABOVE_TOP
  box1.border = BorderFactory.createCompoundBorder(border1, innerBorder)
  box1.add(makeLabeledBox(fileName, JTextField()))
  box1.add(Box.createVerticalStrut(5))
  box1.add(makeLabeledBox(filesOfType, JComboBox<String>()))

  val box2 = Box.createVerticalBox()
  val border2 = BorderFactory.createTitledBorder("HTTP Proxy")
  border2.titlePosition = TitledBorder.ABOVE_TOP
  box2.border = BorderFactory.createCompoundBorder(border2, innerBorder)
  box2.add(makeLabeledBox(host, JTextField()))
  box2.add(Box.createVerticalStrut(5))
  box2.add(makeLabeledBox(port, JTextField()))
  box2.add(Box.createVerticalStrut(5))
  box2.add(makeLabeledBox(user, JTextField()))
  box2.add(Box.createVerticalStrut(5))
  box2.add(makeLabeledBox(password, JPasswordField()))

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

private fun makeLabeledBox(
  label: Component,
  c: Component,
): Component {
  val box = Box.createHorizontalBox()
  box.add(label)
  box.add(Box.createHorizontalStrut(5))
  box.add(c)
  return box
}

// @see javax/swing/plaf/metal/MetalFileChooserUI.java
private class AlignedLabel(
  text: String,
) : JLabel(text) {
  private var group = mutableListOf<AlignedLabel>()
  private var maxWidth = 0

  init {
    horizontalAlignment = RIGHT
  }

  override fun getPreferredSize(): Dimension? {
    val d = super.getPreferredSize()
    // Align the width with all other labels in group.
    d?.width = getMaxWidth() + INDENT
    return d
  }

  private fun getSuperPreferredWidth() = super.getPreferredSize().width

  private fun getMaxWidth(): Int {
    // if (maxWidth == 0) {
    //   // val max = group.stream().map { it.getSuperPreferredWidth() }.reduce(0, Integer::max)
    //   // val max = group.map { it.getSuperPreferredWidth() }.fold(0) { a, b -> maxOf(a, b) }
    //   // val max = group.map { it.getSuperPreferredWidth() }.fold(0, ::maxOf)
    //   val max = group.map { it.getSuperPreferredWidth() }.maxOrNull() ?: 0
    //   group.forEach { al -> al.maxWidth = max }
    // }
    if (maxWidth == 0 && group.isNotEmpty()) {
      val max = group.maxOf(AlignedLabel::getSuperPreferredWidth)
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
