package example

import java.awt.*
import javax.swing.*
import javax.swing.border.TitledBorder

fun createUI(): Component {
  val fileName = AlignedLabel("File Name:")
  val filesOfType = AlignedLabel("Files of Type:")
  val host = AlignedLabel("Host:")
  val port = AlignedLabel("Port:")
  val userName = AlignedLabel("User Name:")
  val password = AlignedLabel("Password:")
  AlignedLabel.groupLabels(fileName, filesOfType, host, port, userName, password)

  val fileChooserBox = createTitledBox("FileChooser")
  fileChooserBox.add(createLabeledBox(fileName, JTextField()))
  fileChooserBox.add(Box.createVerticalStrut(5))
  fileChooserBox.add(createLabeledBox(filesOfType, JComboBox<String>()))

  val proxyBox = createTitledBox("HTTP Proxy")
  proxyBox.add(createLabeledBox(host, JTextField()))
  proxyBox.add(Box.createVerticalStrut(5))
  proxyBox.add(createLabeledBox(port, JTextField()))
  proxyBox.add(Box.createVerticalStrut(5))
  proxyBox.add(createLabeledBox(userName, JTextField()))
  proxyBox.add(Box.createVerticalStrut(5))
  proxyBox.add(createLabeledBox(password, JPasswordField()))

  val box = Box.createVerticalBox()
  box.add(fileChooserBox)
  box.add(Box.createVerticalStrut(10))
  box.add(proxyBox)

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createTitledBox(title: String): Box {
  val titledBorder = BorderFactory.createTitledBorder(title)
  titledBorder.titlePosition = TitledBorder.ABOVE_TOP
  val innerBorder = BorderFactory.createEmptyBorder(5, 2, 5, 5)
  val box = Box.createVerticalBox()
  box.border = BorderFactory.createCompoundBorder(titledBorder, innerBorder)
  return box
}

private fun createLabeledBox(
  label: Component,
  field: Component,
): Component {
  val box = Box.createHorizontalBox()
  box.add(label)
  box.add(Box.createHorizontalStrut(5))
  box.add(field)
  return box
}

// @see javax/swing/plaf/metal/MetalFileChooserUI.java
private class AlignedLabel(
  text: String,
) : JLabel(text, SwingConstants.RIGHT) {
  // An ungrouped label is aligned only with itself.
  private var group: List<AlignedLabel> = listOf(this)

  override fun getPreferredSize(): Dimension {
    val d = super.getPreferredSize()
    // Align the width with all other labels in group.
    d.width = getMaxWidth() + INDENT
    return d
  }

  // BasicLabelUI calculates the minimum and maximum sizes without calling
  // the overridden getPreferredSize(), so BoxLayout would resize the labels unevenly.
  override fun getMinimumSize(): Dimension = preferredSize

  override fun getMaximumSize(): Dimension = preferredSize

  // The width is not cached so that changes of the text or font are reflected.
  private fun getMaxWidth() = group.maxOfOrNull(AlignedLabel::getSuperPreferredWidth) ?: 0

  private fun getSuperPreferredWidth() = super.getPreferredSize().width

  companion object {
    private const val INDENT = 10

    fun groupLabels(vararg labels: AlignedLabel) {
      val group = labels.toList()
      group.forEach { it.group = group }
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
