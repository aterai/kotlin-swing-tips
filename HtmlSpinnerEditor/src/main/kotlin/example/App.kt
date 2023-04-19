package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

fun makeUI(): Component {
  val items = arrayOf(
    "<html><font color='red'>Sunday</font> <font color='gray'>(Sun.)",
    "<html><font color='black'>Monday</font> <font color='gray'>(Mon.)",
    "<html><font color='black'>Tuesday</font> <font color='gray'>(Tue.)",
    "<html><font color='black'>Wednesday</font> <font color='gray'>(Wed.)",
    "<html><font color='black'>Thursday</font> <font color='gray'>(Thu.)",
    "<html><font color='black'>Friday</font> <font color='gray'>(Fri.)",
    "<html><font color='blue'>Saturday</font> <font color='gray'>(Sat.)"
  )
  val p1 = JPanel(BorderLayout(5, 5))
  p1.add(JSpinner(SpinnerListModel(items)))
  p1.border = BorderFactory.createTitledBorder("ListEditor(default)")

  val spinner = object : JSpinner(SpinnerListModel(items)) {
    override fun setEditor(editor: JComponent) {
      val oldEditor = getEditor()
      if (editor != oldEditor && oldEditor is HtmlListEditor) {
        oldEditor.dismiss(this)
      }
      super.setEditor(editor)
    }
  }
  spinner.editor = HtmlListEditor(spinner)
  val p2 = JPanel(BorderLayout(5, 5))
  p2.add(spinner)
  p2.border = BorderFactory.createTitledBorder("HtmlListEditor")

  val panel = JPanel(BorderLayout(25, 25))
  panel.add(p1, BorderLayout.NORTH)
  panel.add(p2, BorderLayout.SOUTH)
  panel.border = BorderFactory.createEmptyBorder(25, 25, 25, 25)

  return JPanel(BorderLayout()).also {
    it.add(panel, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private class HtmlListEditor(
  spinner: JSpinner
) : JLabel(spinner.value.toString()), ChangeListener {
  init {
    require(spinner.model is SpinnerListModel) { "model not a SpinnerListModel" }
    spinner.addChangeListener(this)
    val tipText = spinner.toolTipText
    if (tipText != null) {
      toolTipText = tipText
    }
  }

  override fun updateUI() {
    super.updateUI()
    border = BorderFactory.createEmptyBorder(0, 5, 0, 5)
    isOpaque = true
    background = Color.WHITE
    inheritsPopupMenu = true
  }

  override fun stateChanged(e: ChangeEvent) {
    text = (e.source as? JSpinner)?.value?.toString()
  }

  override fun getPreferredSize(): Dimension = super.getPreferredSize().also {
    it.width = 200
  }

  // @see javax/swing/JSpinner.DefaultEditor.html#dismiss(JSpinner)
  fun dismiss(spinner: JSpinner) {
    spinner.removeChangeListener(this)
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
