package example

import java.awt.*
import java.util.ArrayDeque
import javax.swing.*
import javax.swing.event.ChangeListener

fun makeUI(): Component {
  val p = JPanel(FlowLayout(FlowLayout.CENTER, 20, 10))
  p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10))

  val model0 = SpinnerNumberModel(100, 0, 100, 1)
  val model1 = SpinnerNumberModel(0, 0, 100, 1)
  val model2 = SpinnerNumberModel(0, 0, 100, 1)

  val list = listOf(model0, model1, model2)
  val expectedSum = 100
  val log = JTextArea()
  val handler = ChangeListener {
    val str = list.map { it.number }.joinToString(" + ")
    log.append("%s = %d%n".format(str, expectedSum))
  }
  val group = SpinnerNumberModelGroup(expectedSum)
  for (m in list) {
    m.addChangeListener(handler)
    group.add(m)
    p.add(JSpinner(m))
  }

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(log))
    it.preferredSize = Dimension(320, 240)
  }
}

// https://stackoverflow.com/questions/21388255/multiple-jsliders-reacting-to-each-other-to-always-equal-100-percent
private class SpinnerNumberModelGroup(private val expectedSum: Int) {
  private val candidates = ArrayDeque<SpinnerNumberModel>()
  private val changeListener = ChangeListener { e ->
    val source = e.source as? SpinnerNumberModel
    update(source)
  }
  private var updating = false

  private fun update(source: SpinnerNumberModel?) {
    if (updating || source == null) {
      return
    }
    updating = true
    if (candidates.size - 1 > 0) {
      val sum = candidates.map { it.number.toInt() }.sum()
      val delta = sum - expectedSum
      if (delta > 0) {
        distributeRemove(delta, source)
      } else {
        distributeAdd(delta, source)
      }
    }
    updating = false
  }

  private fun distributeRemove(
    delta: Int,
    source: SpinnerNumberModel,
  ) {
    var counter = 0
    var remaining = delta
    while (remaining > 0) {
      val model = candidates.removeFirst()
      counter++
      if (model == source) {
        candidates.addLast(model)
      } else {
        val prev = model.previousValue
        if (prev is Int) {
          model.setValue(prev)
          remaining--
          counter = 0
        }
        candidates.addLast(model)
        if (remaining == 0) {
          break
        }
      }
      if (counter > candidates.size) {
        val msg = "Can not distribute $delta among $candidates"
        throw IllegalArgumentException(msg)
      }
    }
  }

  private fun distributeAdd(
    delta: Int,
    source: SpinnerNumberModel,
  ) {
    var counter = 0
    var remaining = -delta
    while (remaining > 0) {
      val model = candidates.removeLast()
      counter++
      if (model == source) {
        candidates.addFirst(model)
      } else {
        val next = model.nextValue
        if (next is Int) {
          model.setValue(next)
          remaining--
          counter = 0
        }
        candidates.addFirst(model)
        if (remaining == 0) {
          break
        }
      }
      if (counter > candidates.size) {
        val msg = "Can not distribute $delta among $candidates"
        throw IllegalArgumentException(msg)
      }
    }
  }

  fun add(spinner: SpinnerNumberModel) {
    candidates.add(spinner)
    spinner.addChangeListener(changeListener)
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
