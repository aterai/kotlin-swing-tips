package example

import java.awt.*
import java.awt.event.ActionEvent
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableCellRenderer

fun makeUI(): Component {
  val log = JTextArea()
  val button1 = JButton("Default")
  button1.addActionListener {
    val chooser = JFileChooser()
    val retValue = chooser.showOpenDialog(log.rootPane)
    if (retValue == JFileChooser.APPROVE_OPTION) {
      log.text = chooser.selectedFile.absolutePath
    }
  }

  val button2 = JButton("JList tooltips")
  button2.addActionListener {
    val chooser = JFileChooser()
    descendants(chooser)
      .filterIsInstance<JList<*>>()
      .forEach { it.cellRenderer = TooltipListCellRenderer<Any>() }
    val retValue = chooser.showOpenDialog(log.rootPane)
    if (retValue == JFileChooser.APPROVE_OPTION) {
      log.text = chooser.selectedFile.absolutePath
    }
  }

  val button3 = JButton("JTable tooltips")
  button3.addActionListener {
    val cmd = "viewTypeDetails"
    val actionEvent = ActionEvent(it.source, ActionEvent.ACTION_PERFORMED, cmd)
    val chooser = JFileChooser()
    chooser.actionMap[cmd]?.actionPerformed(actionEvent)
    descendants(chooser)
      .filterIsInstance<JTable>()
      .firstOrNull()
      ?.setDefaultRenderer(Any::class.java, TooltipTableCellRenderer())
    val retValue = chooser.showOpenDialog(log.rootPane)
    if (retValue == JFileChooser.APPROVE_OPTION) {
      log.text = chooser.selectedFile.absolutePath
    }
  }

  val p = JPanel()
  p.border = BorderFactory.createTitledBorder("JFileChooser")
  p.add(button1)
  p.add(button2)
  p.add(button3)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(log))
    it.preferredSize = Dimension(320, 240)
  }
}

fun descendants(parent: Container): List<Component> =
  parent.components
    .filterIsInstance<Container>()
    .flatMap { listOf(it) + descendants(it) }

private class TooltipListCellRenderer<E> : ListCellRenderer<E> {
  private val renderer = DefaultListCellRenderer()

  override fun getListCellRendererComponent(
    list: JList<out E>,
    value: E?,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean,
  ): Component = renderer
    .getListCellRendererComponent(
      list,
      value,
      index,
      isSelected,
      cellHasFocus,
    ).also {
      (it as? JComponent)?.toolTipText = value?.toString()
    }
}

private class TooltipTableCellRenderer : TableCellRenderer {
  private val renderer = DefaultTableCellRenderer()

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component {
    val c = renderer.getTableCellRendererComponent(
      table,
      value,
      isSelected,
      hasFocus,
      row,
      column,
    )
    if (c is JLabel) {
      val i = c.insets
      val rect = table.getCellRect(row, column, false)
      rect.width -= i.left + i.right
      c.toolTipText = if (isClipped(c, rect)) c.text else table.toolTipText
    }
    return c
  }

  private fun isClipped(
    label: JLabel,
    viewR: Rectangle,
  ): Boolean {
    val iconR = Rectangle()
    val textR = Rectangle()
    val str = SwingUtilities.layoutCompoundLabel(
      label,
      label.getFontMetrics(label.font),
      label.text,
      label.icon,
      label.verticalAlignment,
      label.horizontalAlignment,
      label.verticalTextPosition,
      label.horizontalTextPosition,
      viewR,
      iconR,
      textR,
      label.iconTextGap,
    )
    return label.text != str
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
