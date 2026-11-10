package com.expeditee.plant.identifier.classifier

data class Node(
    var key: String? = null,
    var leafId: String? = null,
    var name: String? = null,
    var parentKey: String? = null,
    var rank: Float? = null,
    var parent: Node? = null,
    val children: MutableList<Node> = mutableListOf(),
) {

    override fun toString(): String {
        return "$key: $name (rank = $rank; parent = $parent)"
    }

    fun addChild(
        node: Node,
    ) {
        children.add(node)
        node.parent = this
    }

    companion object {

        fun getNodeFromString(
            originalString: String,
        ): Node {
            val splits = originalString.trim().split(
                delimiters = arrayOf(","),
                limit = 6,
            )
            return Node(
                parentKey = splits.getOrNull(0),
                key = splits.getOrNull(1),
                rank = splits.getOrNull(2)?.toFloat(),
                leafId = splits.getOrNull(3),
                name = splits.getOrNull(4),
            )
        }
    }
}
