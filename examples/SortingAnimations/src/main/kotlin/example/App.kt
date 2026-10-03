package example

import java.awt.*
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import java.util.Collections
import java.util.concurrent.ExecutionException
import javax.swing.*

private val DOT_COLOR = Color.BLACK
private val MARKER_COLOR = Color.RED
private const val MIN_NUMBER = 50
private const val MAX_NUMBER = 500
private val plotArea = Rectangle(5, 5, 310, 170)

// The list is modified by the SwingWorker thread and read by the EDT
private val array = Collections.synchronizedList(ArrayList<Double>(MAX_NUMBER))
private val distributionCombo = JComboBox(InputDistribution.entries.toTypedArray())
private val algorithmCombo = JComboBox(SortAlgorithm.entries.toTypedArray())
private val numberModel = SpinnerNumberModel(150, MIN_NUMBER, MAX_NUMBER, 10)
private val numberSpinner = JSpinner(numberModel)
private val startButton = JButton("Start")
private val cancelButton = JButton("Cancel")
private val canvas = object : JPanel() {
  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    drawDots(g)
  }
}
private var worker: SwingWorker<String, Rectangle>? = null
private var needsRegeneration = false

fun createUI(): Component {
  generateArray()
  setComponentsEnabled(true)

  startButton.addActionListener { startSorting() }
  cancelButton.addActionListener {
    worker?.takeUnless { it.isDone }?.cancel(true)
  }

  val listener = ItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      resetArray()
    }
  }
  distributionCombo.addItemListener(listener)
  algorithmCombo.addItemListener(listener)
  numberSpinner.addChangeListener { resetArray() }
  canvas.background = Color.WHITE

  val box1 = Box.createHorizontalBox().also {
    it.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
    it.add(JLabel(" Number:"))
    it.add(numberSpinner)
    it.add(JLabel(" Input:"))
    it.add(distributionCombo)
  }

  val box2 = Box.createHorizontalBox().also {
    it.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
    it.add(JLabel(" Algorithm:"))
    it.add(algorithmCombo)
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
    it.add(canvas)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun drawDots(g: Graphics) {
  val size = array.size
  for (i in 0..<size) {
    val r = SortingTask.getDotBounds(plotArea, size, i, array[i])
    g.color = if (i % 5 == 0) MARKER_COLOR else DOT_COLOR
    g.drawOval(r.x, r.y, r.width, r.height)
  }
}

private fun setComponentsEnabled(enabled: Boolean) {
  cancelButton.isEnabled = !enabled
  startButton.isEnabled = enabled
  numberSpinner.isEnabled = enabled
  distributionCombo.isEnabled = enabled
  algorithmCombo.isEnabled = enabled
}

private fun generateArray() {
  val distribution = distributionCombo.getItemAt(distributionCombo.selectedIndex)
  array.clear()
  distribution.generate(array, numberModel.number.toInt())
  needsRegeneration = false
}

private fun resetArray() {
  generateArray()
  canvas.toolTipText = null
  canvas.repaint()
}

private fun startSorting() {
  // The previous run has already sorted (or partially sorted) the array
  if (needsRegeneration) {
    generateArray()
    canvas.repaint()
  }
  needsRegeneration = true
  setComponentsEnabled(false)
  canvas.toolTipText = null
  val algorithm = algorithmCombo.getItemAt(algorithmCombo.selectedIndex)
  worker = object : SortingTask(algorithm, array, plotArea) {
    override fun process(chunks: List<Rectangle>) {
      if (canvas.isDisplayable && !isCancelled) {
        chunks.forEach(canvas::repaint)
      } else {
        cancel(true)
      }
    }

    override fun done() {
      if (canvas.isDisplayable) {
        setComponentsEnabled(true)
        canvas.toolTipText = getDoneMessage()
        canvas.repaint()
      }
    }
  }.also { it.execute() }
}

private enum class SortAlgorithm(
  private val description: String,
) {
  INSERTION("Insertion Sort"),
  SELECTION("Selection Sort"),
  SHELL("Shell Sort"),
  HEAP("Heap Sort"),
  QUICK("Quicksort"),
  TWO_WAY_QUICK("2-way Quicksort"),
  ;

  override fun toString() = description
}

private enum class InputDistribution {
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
  private val algorithm: SortAlgorithm,
  private val array: MutableList<Double>,
  area: Rectangle,
) : SwingWorker<String, Rectangle>() {
  private val area = Rectangle(area)

  @Throws(InterruptedException::class)
  override fun doInBackground(): String {
    val n = array.size
    when (algorithm) {
      SortAlgorithm.INSERTION -> insertionSort(n)
      SortAlgorithm.SELECTION -> selectionSort(n)
      SortAlgorithm.SHELL -> shellSort(n)
      SortAlgorithm.HEAP -> heapSort(n)
      SortAlgorithm.QUICK -> quickSort(0, n - 1)
      SortAlgorithm.TWO_WAY_QUICK -> twoWayQuickSort(0, n - 1)
    }
    return "Done"
  }

  protected fun getDoneMessage() =
    try {
      if (isCancelled) "Cancelled" else get()
    } catch (ex: InterruptedException) {
      Thread.currentThread().interrupt()
      "Interrupted"
    } catch (ex: ExecutionException) {
      "Error: ${ex.message}"
    }

  @Throws(InterruptedException::class)
  private fun swap(
    i: Int,
    j: Int,
  ) {
    if (isCancelled) {
      throw InterruptedException()
    }
    // erase the dots at their old positions...
    publishDirtyRegion(i)
    publishDirtyRegion(j)
    Collections.swap(array, i, j)
    // ...and draw them at their new positions
    publishDirtyRegion(i)
    publishDirtyRegion(j)
    Thread.sleep(DELAY)
  }

  private fun publishDirtyRegion(index: Int) {
    val r = getDotBounds(area, array.size, index, array[index])
    // Graphics#drawOval(x, y, w, h) covers (w + 1) x (h + 1) pixels
    r.setSize(r.width + 1, r.height + 1)
    publish(r)
  }

  // Sorting Algorithms
  @Throws(InterruptedException::class)
  private fun insertionSort(n: Int) {
    for (i in 1..<n) {
      var j = i
      while (j > 0 && array[j - 1] > array[j]) {
        swap(j - 1, j)
        j--
      }
    }
  }

  @Throws(InterruptedException::class)
  private fun selectionSort(n: Int) {
    for (i in 0..<n - 1) {
      var min = i
      for (j in i + 1..<n) {
        if (array[j] < array[min]) {
          min = j
        }
      }
      if (min != i) {
        swap(i, min)
      }
    }
  }

  @Throws(InterruptedException::class)
  private fun shellSort(n: Int) {
    // Knuth's gap sequence: 1, 4, 13, 40, 121, ...
    var h = 1
    while (h < n / 3) {
      h = 3 * h + 1
    }
    while (h > 0) {
      for (i in h..<n) {
        var j = i
        while (j >= h && array[j - h] > array[j]) {
          swap(j - h, j)
          j -= h
        }
      }
      h /= 3
    }
  }

  @Throws(InterruptedException::class)
  private fun siftDown(
    root: Int,
    last: Int,
  ) {
    var parent = root
    var child = 2 * parent + 1
    while (child <= last) {
      if (child < last && array[child + 1] > array[child]) {
        child++
      }
      if (array[parent] >= array[child]) {
        break
      }
      swap(parent, child)
      parent = child
      child = 2 * parent + 1
    }
  }

  @Throws(InterruptedException::class)
  private fun heapSort(n: Int) {
    for (i in n / 2 - 1 downTo 0) {
      siftDown(i, n - 1)
    }
    for (i in n - 1 downTo 1) {
      swap(0, i)
      siftDown(0, i - 1)
    }
  }

  @Throws(InterruptedException::class)
  private fun quickSort(
    lower: Int,
    upper: Int,
  ) {
    if (lower < upper) {
      var m = lower
      for (i in lower + 1..upper) {
        if (array[i] < array[lower]) {
          m++
          swap(m, i)
        }
      }
      swap(lower, m)
      quickSort(lower, m - 1)
      quickSort(m + 1, upper)
    }
  }

  @Throws(InterruptedException::class)
  private fun twoWayQuickSort(
    lower: Int,
    upper: Int,
  ) {
    if (lower < upper) {
      val m = partition(lower, upper)
      twoWayQuickSort(lower, m - 1)
      twoWayQuickSort(m + 1, upper)
    }
  }

  @Throws(InterruptedException::class)
  private fun partition(
    lower: Int,
    upper: Int,
  ): Int {
    val pivot = array[lower]
    var i = lower
    var j = upper + 1
    while (true) {
      do {
        i++
      } while (i <= upper && array[i] < pivot)
      do {
        j--
      } while (array[j] > pivot)
      if (i > j) {
        break
      }
      swap(i, j)
    }
    swap(lower, j)
    return j
  }

  companion object {
    const val DOT_SIZE = 4
    private const val DELAY = 5L

    fun getDotBounds(
      area: Rectangle,
      size: Int,
      index: Int,
      value: Double,
    ): Rectangle {
      val x = area.x + (area.width * index / size.toDouble()).toInt()
      val y = area.y + area.height - (area.height * value).toInt()
      return Rectangle(x, y, DOT_SIZE, DOT_SIZE)
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
      minimumSize = Dimension(256, 200)
      isResizable = false
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
