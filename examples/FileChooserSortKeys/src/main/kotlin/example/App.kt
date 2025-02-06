package example

import java.awt.*
import java.awt.event.ActionEvent
import javax.swing.*
import javax.swing.event.AncestorEvent
import javax.swing.event.AncestorListener

private val model = SpinnerNumberModel(0, -1, 3, 1)
private val combo = JComboBox(SortOrder.entries.toTypedArray())

fun makeUI(): Component {
  val log = JTextArea()
  val fileChooser = makeFileChooser()
  val button = object : JButton("open") {
    override fun updateUI() {
      super.updateUI()
      SwingUtilities.updateComponentTreeUI(fileChooser)
    }
  }
  button.addActionListener {
    val retValue = fileChooser.showOpenDialog(button.rootPane)
    if (retValue == JFileChooser.APPROVE_OPTION) {
      log.text = fileChooser.selectedFile.absolutePath
    }
  }
  val p = JPanel()
  p.border = BorderFactory.createTitledBorder("JFileChooser")
  p.add(JLabel("SortKey column:"))
  p.add(JSpinner(model))
  p.add(combo)
  p.add(button)
  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(log))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeFileChooser() = object : JFileChooser() {
  private var handler: AncestorListener? = null

  override fun updateUI() {
    removeAncestorListener(handler)
    super.updateUI()
    handler = object : AncestorListener {
      override fun ancestorAdded(e: AncestorEvent) {
        val fc = e.component as? JFileChooser ?: return
        setViewTypeDetails(fc)
        descendants(e.component)
          .filterIsInstance<JTable>()
          .firstNotNullOf { table ->
            val sortKeys = table.rowSorter.sortKeys
            val col = model.number.toInt()
            if (col < 0) {
              table.rowSorter.sortKeys = emptyList()
            } else if (sortKeys.isEmpty() && col < table.columnCount) {
              val order = combo.getItemAt(combo.selectedIndex)
              table.rowSorter.sortKeys = listOf(RowSorter.SortKey(col, order))
            }
          }
      }

      override fun ancestorRemoved(e: AncestorEvent) {
        // not need
      }

      override fun ancestorMoved(e: AncestorEvent) {
        // not need
      }
    }
    addAncestorListener(handler)
  }
}

fun setViewTypeDetails(fc: JFileChooser) {
  val cmd = "viewTypeDetails"
  val act = fc.actionMap[cmd]
  act?.actionPerformed(ActionEvent(fc, ActionEvent.ACTION_PERFORMED, cmd))
}

fun descendants(parent: Container): List<Component> =
  parent.components
    .filterIsInstance<Container>()
    .flatMap { listOf(it) + descendants(it) }

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
