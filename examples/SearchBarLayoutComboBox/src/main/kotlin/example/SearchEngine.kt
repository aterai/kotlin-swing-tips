package example

import javax.swing.ImageIcon

open class SearchEngine(
  private val name: String,
  val url: String,
  val favicon: ImageIcon,
) {
  override fun toString() = name
}
