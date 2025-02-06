package example

import java.awt.*
import java.awt.event.ActionEvent
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import javax.swing.*
import javax.swing.JSpinner.DefaultEditor
import javax.swing.text.DefaultFormatterFactory
import javax.swing.text.NumberFormatter

fun makeUI(): Component {
  val s0 = JSpinner(makeSpinnerNumberModel())
  val s1 = makeSpinner1(makeSpinnerNumberModel())
  val s2 = makeSpinner2(makeSpinnerNumberModel())
  val s3 = makeSpinner3(makeSpinnerNumberModel())

  val act = object : AbstractAction("setEnabled") {
    private var old: Any? = null

    override fun actionPerformed(e: ActionEvent) {
      val flg = (e.source as? JCheckBox)?.isSelected == true
      listOf(s0, s1, s2, s3).forEach { it.isEnabled = flg }
      if (flg) {
        listOf(s2, s3).forEach { it.value = old }
      } else {
        old = s2.value
        listOf(s2, s3).forEach { it.value = Double.NaN }
      }
    }
  }

  val cbx = JCheckBox(act)
  cbx.isSelected = true

  val box = Box.createVerticalBox()
  box.border = BorderFactory.createEmptyBorder(5, 15, 5, 5)
  listOf(s0, s1, s2, s3).forEach { c ->
    c.isEnabled = true
    c.alignmentX = Component.LEFT_ALIGNMENT
    val d = c.preferredSize
    d.width = Int.MAX_VALUE
    c.maximumSize = d
    box.add(c)
    box.add(Box.createVerticalStrut(5))
  }
  box.add(Box.createVerticalGlue())

  return JPanel(BorderLayout()).also {
    it.add(cbx, BorderLayout.NORTH)
    it.add(box)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeSpinnerNumberModel() = SpinnerNumberModel(10.0, 0.0, 100.0, 1.0)

private fun makeSpinner1(m: SpinnerNumberModel) = JSpinner(m).also {
  getJFormattedTextField(it)?.also { ftf ->
    val dfs = DecimalFormatSymbols()
    ftf.formatterFactory = makeFormatterFactory(dfs)
    ftf.disabledTextColor = UIManager.getColor("TextField.disabledColor")
  }
}

private fun makeSpinner2(m: SpinnerNumberModel) = JSpinner(m).also {
  getJFormattedTextField(it)?.also { ftf ->
    val dfs = DecimalFormatSymbols()
    dfs.naN = " "
    ftf.formatterFactory = makeFormatterFactory(dfs)
  }
}

private fun makeSpinner3(m: SpinnerNumberModel) = JSpinner(m).also {
  getJFormattedTextField(it)?.also { ftf ->
    val dfs = DecimalFormatSymbols()
    dfs.naN = "----"
    ftf.formatterFactory = makeFormatterFactory(dfs)
  }
}

private fun getJFormattedTextField(s: JSpinner) = (s.editor as? DefaultEditor)?.let {
  it.textField.also { ftf -> ftf.columns = 8 }
}

private fun makeFormatterFactory(dfs: DecimalFormatSymbols): DefaultFormatterFactory {
  val format = DecimalFormat("0.00", dfs)
  val displayFormatter = NumberFormatter(format)
  displayFormatter.valueClass = Double::class.java
  val editFormatter = NumberFormatter(format)
  editFormatter.valueClass = Double::class.java
  return DefaultFormatterFactory(displayFormatter, displayFormatter, editFormatter)
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
