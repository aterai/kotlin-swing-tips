package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val b1 = Box.createHorizontalBox().also {
    it.add(Box.createHorizontalGlue())
    it.add(JButton("default"))
    it.add(Box.createHorizontalStrut(5))
    it.add(JButton("a"))
    it.border = BorderFactory.createEmptyBorder(5, 0, 5, 5)
  }
  val b2 = createRightAlignBox2(120, 5, "getPreferredSize", "xxx")
  val b3 = createRightAlignBox3(100, 5, "Spring+Box", "Layout")
  val b4 = createRightAlignBox4(120, 2, "SpringLayout", "gap:2")
  val b5 = createRightAlignBox5(3, "GridLayout+Box", "gap:3")
  val b6 = createRightAlignBox6(120, 2, "GridBugLayout", "gap:2")

  val box = Box.createVerticalBox()
  listOf(b6, b5, b4, b3, b2, b1).forEach {
    box.add(JSeparator())
    box.add(it)
  }
  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

fun createRightAlignBox6(width: Int, gap: Int, vararg titles: String): Component {
  val p = JPanel(GridBagLayout())
  val c = GridBagConstraints()
  c.insets = Insets(0, gap, 0, 0)
  titles.map { JButton(it) }.forEach {
    c.ipadx = width - it.preferredSize.width
    p.add(it, c)
  }
  p.border = BorderFactory.createEmptyBorder(gap, gap, gap, gap)
  return JPanel(BorderLayout()).also { it.add(p, BorderLayout.EAST) }
}

fun createRightAlignBox5(gap: Int, vararg titles: String): Component {
  val list = titles.map { JButton(it) }
  val p = object : JPanel(GridLayout(1, list.size, gap, gap)) {
    override fun getMaximumSize() = super.getPreferredSize()
  }
  list.forEach { p.add(it) }
  return Box.createHorizontalBox().also {
    it.add(Box.createHorizontalGlue())
    it.add(p)
    it.border = BorderFactory.createEmptyBorder(gap, gap, gap, gap)
  }
}

fun createRightAlignBox4(width: Int, gap: Int, vararg titles: String): Component {
  val list = titles.map { JButton(it) }
  val layout = SpringLayout()
  val p = object : JPanel(layout) {
    override fun getPreferredSize(): Dimension {
      val maxHeight = list.maxOfOrNull { it.preferredSize.height } ?: 0
      return Dimension(width * list.size + gap + gap, maxHeight + gap + gap)
    }
  }
  var x = layout.getConstraint(SpringLayout.WIDTH, p)
  val y = Spring.constant(gap)
  val g = Spring.minus(Spring.constant(gap))
  val w = Spring.constant(width)
  for (b in list) {
    val constraints = layout.getConstraints(b)
    x = Spring.sum(x, g)
    constraints.setConstraint(SpringLayout.EAST, x)
    constraints.y = y
    constraints.width = w
    p.add(b)
    x = Spring.sum(x, Spring.minus(w))
  }
  return p
}

fun createRightAlignBox3(width: Int, gap: Int, vararg titles: String): Component {
  val list = titles.map { JButton(it) }
  val layout = SpringLayout()
  val p = object : JPanel(layout) {
    override fun getPreferredSize(): Dimension {
      val maxHeight = list.maxOfOrNull { it.preferredSize.height } ?: 0
      return Dimension(width * list.size + gap + gap, maxHeight + gap + gap)
    }
  }
  val cons = layout.getConstraints(p)
  // cons.setConstraint(SpringLayout.SOUTH, Spring.constant(p.getPreferredSize().height))
  cons.setConstraint(SpringLayout.EAST, Spring.constant((width + gap) * list.size))

  var x = Spring.constant(0)
  val y = Spring.constant(gap)
  val g = Spring.constant(gap)
  val w = Spring.constant(width)
  for (b in list) {
    val constraints = layout.getConstraints(b)
    constraints.x = x
    constraints.y = y
    constraints.width = w
    p.add(b)
    x = Spring.sum(x, w)
    x = Spring.sum(x, g)
  }

  return Box.createHorizontalBox().also {
    it.add(Box.createHorizontalGlue())
    it.add(p)
  }
}

fun createRightAlignBox2(width: Int, gap: Int, vararg titles: String): Component {
  val list = titles.map { JButton(it) }
  val box = object : JPanel() {
    override fun updateUI() {
      list.forEach { it.preferredSize = null }
      super.updateUI()
      EventQueue.invokeLater {
        val maxHeight = list.maxOfOrNull { it.preferredSize.height } ?: 0
        val d = Dimension(width, maxHeight)
        list.forEach { it.preferredSize = d }
        revalidate()
      }
    }
  }
  box.layout = BoxLayout(box, BoxLayout.X_AXIS)
  box.add(Box.createHorizontalGlue())
  list.forEach {
    box.add(it)
    box.add(Box.createHorizontalStrut(gap))
  }
  box.border = BorderFactory.createEmptyBorder(gap, 0, gap, 0)
  return box
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
