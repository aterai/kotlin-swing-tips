package example

import java.awt.*
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import java.util.Collections
import javax.swing.*

private const val MIN_X = 5
private const val MIN_Y = 5
private const val MAX_X = 315
private const val MAX_Y = 175
private const val MIN_NUM = 50
private const val MAX_NUM = 500
private val DRAW_COLOR = Color.BLACK
private val BACK_COLOR = Color.WHITE

private val array = mutableListOf<Double>()
private var number = 150
private var fax = 0.0
private var fay = 0.0

private var worker: SwingWorker<String, Rectangle>? = null
private val distributionsCmb = JComboBox(GenerateInputs.entries.toTypedArray())
private val algorithmsCmb = JComboBox(SortAlgorithms.entries.toTypedArray())
private val model = SpinnerNumberModel(number, MIN_NUM, MAX_NUM, 10)
private val spinner = JSpinner(model)
private val startButton = JButton("Start")
private val cancelButton = JButton("Cancel")
val panel = object : JPanel() {
  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    drawAllOval(g)
  }
}

fun makeUI(): Component {
  genArray(number)
  startButton.addActionListener {
    setComponentEnabled(false)
    panel.toolTipText = null
    workerExecute()
  }
  cancelButton.addActionListener {
    worker?.takeUnless { it.isDone }?.cancel(true)
  }
  val il = ItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      genArray(number)
      panel.toolTipText = null
      panel.repaint()
    }
  }
  distributionsCmb.addItemListener(il)
  algorithmsCmb.addItemListener(il)
  panel.background = BACK_COLOR

  val box1 = Box.createHorizontalBox().also {
    it.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
    it.add(JLabel(" Number:"))
    it.add(spinner)
    it.add(JLabel(" Input:"))
    it.add(distributionsCmb)
  }

  val box2 = Box.createHorizontalBox().also {
    it.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
    it.add(JLabel(" Algorithm:"))
    it.add(algorithmsCmb)
    it.add(startButton)
    it.add(cancelButton)
  }

  val p = JPanel(GridLayout(2, 1)).also {
    it.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
    it.add(box1)
    it.add(box2)
  }

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(panel)
    it.preferredSize = Dimension(320, 240)
  }
}

fun drawAllOval(g: Graphics) {
  for (i in 0..<number) {
    val px = (MIN_X + fax * i).toInt()
    val py = MAX_Y - (fay * array[i]).toInt()
    g.color = if (i % 5 == 0) Color.RED else DRAW_COLOR
    g.drawOval(px, py, 4, 4)
  }
}

fun setComponentEnabled(flag: Boolean) {
  cancelButton.isEnabled = !flag
  startButton.isEnabled = flag
  spinner.isEnabled = flag
  distributionsCmb.isEnabled = flag
  algorithmsCmb.isEnabled = flag
}

fun genArray(n: Int) {
  array.clear()
  fax = (MAX_X - MIN_X) / n.toDouble()
  fay = MAX_Y.toDouble() - MIN_Y
  distributionsCmb.getItemAt(distributionsCmb.selectedIndex).generate(array, n)
}

fun workerExecute() {
  val tmp = model.number.toInt()
  if (tmp != number) {
    number = tmp
    genArray(number)
  }
  val sa = algorithmsCmb.getItemAt(algorithmsCmb.selectedIndex)
  val paintArea = Rectangle(MIN_X, MIN_Y, MAX_X - MIN_X, MAX_Y - MIN_Y)
  worker = object : SortingTask(sa, number, array, paintArea, fax, fay) {
    override fun process(chunks: List<Rectangle>) {
      if (panel.isDisplayable && !isCancelled) {
        chunks.forEach(panel::repaint)
      } else {
        cancel(true)
      }
    }

    override fun done() {
      setComponentEnabled(true)
      panel.toolTipText = runCatching {
        if (isCancelled) "Cancelled" else get()
      }.onFailure {
        if (it is InterruptedException) {
          Thread.currentThread().interrupt()
        }
        "Error: ${it.message}"
      }.getOrNull()
      panel.repaint()
    }
  }.also { it.execute() }
}

private enum class SortAlgorithms(
  private val description: String,
) {
  ISORT("Insertion Sort"),
  SELSORT("Selection Sort"),
  SHELLSORT("Shell Sort"),
  HSORT("Heap Sort"),
  QSORT("Quicksort"),
  QSORT2("2-way Quicksort"),
  ;

  override fun toString() = description
}

private enum class GenerateInputs {
  RANDOM {
    override fun generate(
      array: MutableList<Double>,
      n: Int,
    ) {
      repeat(n) {
        array.add(Math.random())
      }
    }
  },
  ASCENDING {
    override fun generate(
      array: MutableList<Double>,
      n: Int,
    ) {
      for (i in 0..<n) {
        array.add(i / n.toDouble())
      }
    }
  },
  DESCENDING {
    override fun generate(
      array: MutableList<Double>,
      n: Int,
    ) {
      for (i in 0..<n) {
        array.add(1.0 - i / n.toDouble())
      }
    }
  }, ;

  abstract fun generate(
    array: MutableList<Double>,
    n: Int,
  )
}

// SortAnim.java -- Animate sorting algorithms
// Copyright (C) 1999 Lucent Technologies
// From 'Programming Pearls' by Jon Bentley
// Sorting Algorithm Animations from Programming Pearls
// http://www.cs.bell-labs.com/cm/cs/pearls/sortanim.html
// modified by aterai aterai@outlook.com
private open class SortingTask(
  private val sortAlgorithm: SortAlgorithms,
  private val number: Int,
  private val array: List<Double>,
  private val rect: Rectangle,
  private val fax: Double,
  private val fay: Double,
) : SwingWorker<String, Rectangle>() {
  private val repaintArea = Rectangle(rect)

  init {
    repaintArea.grow(5, 5)
  }

  @Throws(InterruptedException::class)
  override fun doInBackground(): String {
    when (sortAlgorithm) {
      SortAlgorithms.ISORT -> isort(number)
      SortAlgorithms.SELSORT -> ssort(number)
      SortAlgorithms.SHELLSORT -> shellsort(number)
      SortAlgorithms.HSORT -> heapsort(number)
      SortAlgorithms.QSORT -> qsort(0, number - 1)
      SortAlgorithms.QSORT2 -> qsort2(0, number - 1)
    }
    return "Done"
  }

  @Throws(InterruptedException::class)
  private fun swap(
    i: Int,
    j: Int,
  ) {
    if (isCancelled) {
      throw InterruptedException()
    }
    var px = (rect.x + fax * i).toInt()
    var py = rect.y + rect.height - (fay * array[i]).toInt()
    publish(Rectangle(px, py, 4, 4))

    Collections.swap(array, i, j)
    px = (rect.x + fax * i).toInt()
    py = rect.y + rect.height - (fay * array[i]).toInt()
    publish(Rectangle(px, py, 4, 4))
    publish(repaintArea)
    Thread.sleep(5)
  }

  // Sorting Algorithms
  @Throws(InterruptedException::class)
  private fun isort(n: Int) {
    for (i in 1..<n) {
      var j = i
      while (j > 0 && array[j - 1] > array[j]) {
        swap(j - 1, j)
        j--
      }
    }
  }

  @Throws(InterruptedException::class)
  private fun ssort(n: Int) {
    for (i in 0..<n - 1) {
      for (j in i..<n) {
        if (array[j] < array[i]) {
          swap(i, j)
        }
      }
    }
  }

  @Suppress("NestedBlockDepth")
  @Throws(InterruptedException::class)
  private fun shellsort(n: Int) {
    var i: Int
    var j: Int
    var h = 1
    while (h < n) {
      h = 3 * h + 1
    }
    while (true) {
      h /= 3
      if (h - 1 < 0) {
        break
      }
      i = h
      while (i < n) {
        j = i
        while (j >= h) {
          if (array[j - h] < array[j]) {
            break
          }
          swap(j - h, j)
          j -= h
        }
        i++
      }
    }
  }

  @Throws(InterruptedException::class)
  private fun shiftDown(
    l: Int,
    u: Int,
  ) {
    var i = l
    var c: Int
    @Suppress("LoopWithTooManyJumpStatements")
    while (true) {
      c = 2 * i
      if (c > u) {
        break
      }
      if (c + 1 <= u && array[c + 1] > array[c]) {
        c++
      }
      if (array[i] >= array[c]) {
        break
      }
      swap(i, c)
      i = c
    }
  }

  @Throws(InterruptedException::class)
  private fun heapsort(n: Int) { // BEWARE!!! Sorts x[1..n-1]
    var i = n / 2
    while (i > 0) {
      shiftDown(i, n - 1)
      i--
    }
    i = n - 1
    while (i >= 2) {
      swap(1, i)
      shiftDown(1, i - 1)
      i--
    }
  }

  @Throws(InterruptedException::class)
  private fun qsort(
    l: Int,
    u: Int,
  ) {
    if (l >= u) {
      return
    }
    var m = l
    for (i in l + 1..u) {
      if (array[i] < array[l]) {
        swap(++m, i)
      }
    }
    swap(l, m)
    qsort(l, m - 1)
    qsort(m + 1, u)
  }

  @Throws(InterruptedException::class)
  private fun qsort2(
    l: Int,
    u: Int,
  ) {
    if (l >= u) {
      return
    }
    var i = l
    var j = u + 1
    while (true) {
      do {
        i++
      } while (i <= u && array[i] < array[l])
      do {
        j--
      } while (array[j] > array[l])
      if (i > j) {
        break
      }
      swap(i, j)
    }
    swap(l, j)
    qsort2(l, j - 1)
    qsort2(j + 1, u)
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
      minimumSize = Dimension(256, 200)
      isResizable = false
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
