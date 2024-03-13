package example

import java.awt.*
import java.awt.font.TextAttribute
import java.util.concurrent.ConcurrentHashMap
import javax.swing.*

fun makeUI(): Component {
  val code = "BC89FE5A"
  val field0 = JFormattedTextField(code)
  field0.setHorizontalAlignment(SwingConstants.RIGHT)
  field0.setColumns(8)
  val field1 = JFormattedTextField(code)
  field1.setHorizontalAlignment(SwingConstants.RIGHT)
  val mono = Font(Font.MONOSPACED, Font.PLAIN, field1.font.size)
  field1.setFont(mono)
  field1.setColumns(8)
  val field2 = JFormattedTextField(code)
  field2.setHorizontalAlignment(SwingConstants.RIGHT)
  val attr: MutableMap<TextAttribute, Any?> = ConcurrentHashMap()
  attr[TextAttribute.TRACKING] = -.011f
  field2.setFont(mono.deriveFont(attr))
  field2.setColumns(8)
  val field3: JFormattedTextField = object : JFormattedTextField(code) {
    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      d.width += 1
      return d
    }
  }
  field3.setHorizontalAlignment(SwingConstants.RIGHT)
  field3.setFont(mono)
  field3.setColumns(8)
  val field4 = JFormattedTextField(code)
  field4.setHorizontalAlignment(SwingConstants.RIGHT)
  field4.setFont(mono)
  field4.setColumns(9)
  val l0 = AlignedLabel("Default:")
  val l1 = AlignedLabel("MONOSPACED:")
  val l2 = AlignedLabel("TRACKING-.011f:")
  val l3 = AlignedLabel("PreferredSize+1:")
  val l4 = AlignedLabel("Columns+1:")
  AlignedLabel.groupLabels(l0, l1, l2, l3, l4)
  val box = Box.createVerticalBox()
  box.add(makeTitledPanel(l0, field0))
  box.add(makeTitledPanel(l1, field1))
  box.add(makeTitledPanel(l2, field2))
  box.add(makeTitledPanel(l3, field3))
  box.add(makeTitledPanel(l4, field4))
  val log = JTextArea()
  EventQueue.invokeLater {
    append(log, field0, code)
    append(log, field1, code)
    append(log, field2, code)
  }
  val p = JPanel(BorderLayout())
  p.add(box, BorderLayout.WEST)
  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(log))
    it.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(
  title: Component,
  c: Component,
): Container {
  val box = JPanel()
  box.add(title)
  box.add(c)
  return box
}

private fun append(
  log: JTextArea,
  c: JComponent,
  str: String,
) {
  val font = c.font
  val frc = c.getFontMetrics(font).fontRenderContext
  val r2 = font.getStringBounds(str, frc)
  val r = SwingUtilities.calculateInnerArea(c, null)
  log.append("%s%n  %s%n  %s%n".format(font, r, r2))
}

private class AlignedLabel(text: String) : JLabel(text) {
  private var group = mutableListOf<AlignedLabel>()
  private var maxWidth = 0

  init {
    horizontalAlignment = RIGHT
  }

  override fun getPreferredSize() = super.getPreferredSize()?.also {
    it.width = getMaxWidth() + INDENT
  }

  private fun getSuperPreferredWidth() = super.getPreferredSize().width

  private fun getMaxWidth(): Int {
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
