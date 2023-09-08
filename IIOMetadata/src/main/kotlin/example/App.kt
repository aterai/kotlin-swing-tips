package example

import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.Text
import java.awt.*
import java.util.Enumeration
import javax.imageio.ImageIO
import javax.imageio.metadata.IIOMetadataNode
import javax.swing.*
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeNode

fun makeUI(): Component {
  val tree = JTree()
  val readers = ImageIO.getImageReadersByFormatName("jpeg")
  val reader = readers.next()
  val buf = StringBuilder()

  val cl = Thread.currentThread().contextClassLoader
  val url = cl.getResource("example/test.jpg")
  if (url != null) {
    ImageIO.createImageInputStream(url.openStream()).use { iis ->
      reader.setInput(iis, true)
      buf.append("Width: %d%n".format(reader.getWidth(0)))
        .append("Height: %d%n".format(reader.getHeight(0)))
      val meta = reader.getImageMetadata(0)
      for (s in meta.metadataFormatNames) {
        buf.append("MetadataFormatName: $s\n")
      }
      (meta.getAsTree("javax_imageio_jpeg_image_1.0") as? IIOMetadataNode)?.also {
        val com = it.getElementsByTagName("com")
        if (com.length > 0) {
          val comment = (com.item(0) as? IIOMetadataNode)?.getAttribute("comment")
          buf.append("Comment: $comment\n")
        }
        buf.append("------------\n")
        print(buf, it, 0)
        tree.model = DefaultTreeModel(XmlTreeNode(it))
      }
    }
  }

  val log = JTextArea(buf.toString())
  val tabs = JTabbedPane()
  tabs.addTab("text", JScrollPane(log))
  tabs.addTab("tree", JScrollPane(tree))
  return JPanel(BorderLayout()).also {
    it.add(tabs)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun print(buf: StringBuilder, node: Node, level: Int) {
  val indent = " ".repeat(level * 2)
  buf.append("%s%s%n".format(indent, node.nodeName))
  if (node.hasAttributes()) {
    for (i in 0 until node.attributes.length) {
      val attr = node.attributes.item(i)
      buf.append("%s  #%s=%s%n".format(indent, attr.nodeName, attr.nodeValue))
    }
  }
  if (node.hasChildNodes()) {
    for (i in 0 until node.childNodes.length) {
      val child = node.childNodes.item(i)
      print(buf, child, level + 1)
    }
  }
}

private class XmlTreeNode(
  private val xmlNode: Node?,
  private val parent: XmlTreeNode? = null,
  private val showAttributes: Boolean? = true,
) : TreeNode {
  private var list: MutableList<XmlTreeNode>? = null
  private val isShowAttributes: Boolean get() = showAttributes ?: parent?.isShowAttributes ?: false
  private val xmlTag: String
    get() {
      if (xmlNode is Element && isShowAttributes) {
        val e = xmlNode
        val buf = StringBuilder()
        buf.append(e.tagName)
        if (e.hasAttributes()) {
          val attr = e.attributes
          val count = attr.length
          for (i in 0 until count) {
            val a = attr.item(i)
            if (i == 0) {
              buf.append(" [")
            } else {
              buf.append(", ")
            }
            buf.append(a.nodeName).append('=').append(a.nodeValue)
          }
          buf.append(']')
        }
        return buf.toString()
      } else if (xmlNode is Text) {
        return xmlNode.getNodeValue()
      }
      return xmlNode?.nodeName ?: ""
    }

  private fun loadChildren(l: MutableList<XmlTreeNode>?): MutableList<XmlTreeNode> {
    return if (l == null) {
      val cn = xmlNode?.childNodes ?: return mutableListOf()
      val count = cn.length
      val ml = mutableListOf<XmlTreeNode>().also {
        for (i in 0 until count) {
          val c = cn.item(i)
          if (c is Text && c.getNodeValue().isEmpty()) {
            continue
          }
          it.add(makeXmlTreeNode(c))
        }
      }
      list = ml
      ml
    } else {
      l
    }
  }

  private fun makeXmlTreeNode(node: Node) = XmlTreeNode(node, this)

  override fun children(): Enumeration<XmlTreeNode> {
    val iterator = loadChildren(list).iterator()
    return object : Enumeration<XmlTreeNode> {
      override fun hasMoreElements() = iterator.hasNext()

      override fun nextElement() = iterator.next()
    }
  }

  override fun getAllowsChildren() = true

  override fun getChildAt(childIndex: Int) = loadChildren(list)[childIndex]

  override fun getChildCount() = loadChildren(list).size

  override fun getIndex(node: TreeNode): Int {
    for ((i, c) in loadChildren(list).withIndex()) {
      if (xmlNode === c.xmlNode) {
        return i
      }
    }
    return -1
  }

  override fun getParent(): TreeNode? = parent

  override fun isLeaf(): Boolean {
    if (xmlNode is Element) {
      return false
    }
    return loadChildren(list).isEmpty()
  }

  override fun toString() = xmlTag
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
