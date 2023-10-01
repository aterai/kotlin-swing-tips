package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ContainerAdapter
import java.awt.event.ContainerEvent
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import javax.swing.*

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

  val button2 = JButton("ListView")
  button2.addActionListener {
    val chooser = JFileChooser()
    descendants(chooser)
      .filterIsInstance<JList<*>>()
      .first()
      .also { addCellEditorListener(it) }
    val retValue = chooser.showOpenDialog(log.rootPane)
    if (retValue == JFileChooser.APPROVE_OPTION) {
      log.text = chooser.selectedFile.absolutePath
    }
  }

  val button3 = JButton("DetailsView")
  button3.addActionListener {
    val chooser = JFileChooser()
    val cmd = "viewTypeDetails"
    val detailsAction = chooser.actionMap[cmd]
    detailsAction?.actionPerformed(ActionEvent(chooser, ActionEvent.ACTION_PERFORMED, cmd))
    descendants(chooser)
      .filterIsInstance<JTable>()
      .first()
      .also { addCellEditorFocusListener(it) }
    val retValue = chooser.showOpenDialog(log.rootPane)
    if (retValue == JFileChooser.APPROVE_OPTION) {
      log.text = chooser.selectedFile.absolutePath
    }
  }

  val p1 = JPanel()
  val title1 = "The cell editor selects the whole filename"
  p1.setBorder(BorderFactory.createTitledBorder(title1))
  p1.add(button1)

  val p2 = JPanel()
  val title2 = "The cell editor selects the filename without the extension"
  p2.setBorder(BorderFactory.createTitledBorder(title2))
  p2.add(button2)
  p2.add(button3)

  val p = JPanel(GridLayout(2, 1))
  p.add(p1)
  p.add(p2)

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

private fun selectWithoutExtension(editor: JTextField) {
  EventQueue.invokeLater {
    val name = editor.getText()
    val end = name.lastIndexOf('.')
    editor.selectionStart = 0
    editor.selectionEnd = if (end > 0) end else name.length
  }
}

private fun addCellEditorListener(list: JList<*>) {
  val readOnly = UIManager.getBoolean("FileChooser.readOnly")
  if (!readOnly) {
    list.addContainerListener(object : ContainerAdapter() {
      override fun componentAdded(e: ContainerEvent) {
        val c = e.child
        if (c is JTextField && "Tree.cellEditor" == c.getName()) {
          selectWithoutExtension(c)
        }
      }
    })
  }
}

private fun addCellEditorFocusListener(table: JTable) {
  val readOnly = UIManager.getBoolean("FileChooser.readOnly")
  val columnModel = table.columnModel
  if (!readOnly && columnModel.columnCount > 0) {
    val tc = columnModel.getColumn(0)
    val editor = tc.cellEditor as DefaultCellEditor
    val tf = editor.component as JTextField
    tf.addFocusListener(object : FocusAdapter() {
      override fun focusGained(e: FocusEvent) {
        selectWithoutExtension(tf)
      }
    })
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
